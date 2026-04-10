import { useEffect, useRef, useState } from "react";
import "./App.css";
import UploadPanel from "./components/UploadPanel";
import Results from "./components/Results";
import { clearCachedFiles, loadCachedFiles, saveCachedFiles } from "./utils/fileCache";
import {
  buildMergedCategories,
  buildPerFileDownloadName,
  buildPerFileTextContent,
  downloadTextFile,
  formatBytes,
  normalizeResponseData,
} from "./utils/results";
const RESULTS_CACHE_KEY = "sortify-last-results";
const REMEMBER_DATA_KEY = "sortify-remember-data";
const ACCESS_TOKEN_KEY = "sortify-access-token";
const VISUAL_EFFECTS_KEY = "sortify-visual-effects";

function App() {
  const [theme, setTheme] = useState(() => {
    return localStorage.getItem("sortify-theme") || "light";
  });
  const [visualEffectsEnabled, setVisualEffectsEnabled] = useState(
      () => localStorage.getItem(VISUAL_EFFECTS_KEY) === "true"
  );
  const [rememberData, setRememberData] = useState(() => localStorage.getItem(REMEMBER_DATA_KEY) === "true");
  const [accessToken, setAccessToken] = useState(() => sessionStorage.getItem(ACCESS_TOKEN_KEY) || "");
  const [clientConfig, setClientConfig] = useState({
    accessTokenRequired: false,
    maxFiles: 10,
    maxFileSizeBytes: 1024 * 1024,
    maxTotalUploadBytes: 4 * 1024 * 1024,
    allowedCategories: [],
  });
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [responseData, setResponseData] = useState(() => {
    if (localStorage.getItem(REMEMBER_DATA_KEY) !== "true") {
      return null;
    }
    const cachedResults = localStorage.getItem(RESULTS_CACHE_KEY);
    if (!cachedResults) {
      return null;
    }

    try {
      return normalizeResponseData(JSON.parse(cachedResults));
    } catch {
      return null;
    }
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const settingsRef = useRef(null);
  const mergedCategories = buildMergedCategories(responseData);

  useEffect(() => {
    localStorage.setItem("sortify-theme", theme);
  }, [theme]);

  useEffect(() => {
    localStorage.setItem(VISUAL_EFFECTS_KEY, String(visualEffectsEnabled));
  }, [visualEffectsEnabled]);

  useEffect(() => {
    let isMounted = true;

    fetch("/api/files/client-config")
      .then(async (response) => {
        if (!response.ok) {
          throw new Error("Failed to load client configuration.");
        }
        return response.json();
      })
      .then((config) => {
        if (isMounted) {
          setClientConfig((currentConfig) => ({
            ...currentConfig,
            ...config,
            allowedCategories: config.allowedCategories || currentConfig.allowedCategories,
          }));
        }
      })
      .catch(() => {
        // Leave defaults in place if config loading fails.
      });

    return () => {
      isMounted = false;
    };
  }, []);

  useEffect(() => {
    let isMounted = true;

    if (!rememberData) {
      setSelectedFiles([]);
      return () => {
        isMounted = false;
      };
    }

    loadCachedFiles()
      .then((files) => {
        if (isMounted && files.length > 0) {
          setSelectedFiles(files);
        }
      })
      .catch(() => {
        // Ignore cache restore failures and let the app continue normally.
      });

    return () => {
      isMounted = false;
    };
  }, [rememberData]);

  useEffect(() => {
    if (rememberData && responseData) {
      localStorage.setItem(RESULTS_CACHE_KEY, JSON.stringify(responseData));
      return;
    }

    localStorage.removeItem(RESULTS_CACHE_KEY);
  }, [responseData, rememberData]);

  useEffect(() => {
    if (!rememberData) {
      return;
    }

    saveCachedFiles(selectedFiles).catch(() => {
      // Ignore file cache failures; in-memory data still works for the current session.
    });
  }, [selectedFiles, rememberData]);

  useEffect(() => {
    localStorage.setItem(REMEMBER_DATA_KEY, String(rememberData));

    if (!rememberData) {
      localStorage.removeItem(RESULTS_CACHE_KEY);
      clearCachedFiles().catch(() => {
        // Ignore cache clear failures; in-memory data has already been cleared.
      });
    }
  }, [rememberData]);

  useEffect(() => {
    if (accessToken) {
      sessionStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
      return;
    }

    sessionStorage.removeItem(ACCESS_TOKEN_KEY);
  }, [accessToken]);

  useEffect(() => {
    if (!isSettingsOpen) {
      return undefined;
    }

    const handlePointerDown = (event) => {
      if (!settingsRef.current?.contains(event.target)) {
        setIsSettingsOpen(false);
      }
    };

    const handleEscape = (event) => {
      if (event.key === "Escape") {
        setIsSettingsOpen(false);
      }
    };

    document.addEventListener("pointerdown", handlePointerDown);
    document.addEventListener("keydown", handleEscape);

    return () => {
      document.removeEventListener("pointerdown", handlePointerDown);
      document.removeEventListener("keydown", handleEscape);
    };
  }, [isSettingsOpen]);

  const updateSelectedFiles = (files, { clearResults = true } = {}) => {
    setSelectedFiles(files);
    if (clearResults) {
      setResponseData(null);
    }
    setError("");
  };

  const handleFileChange = (event) => {
    updateSelectedFiles(Array.from(event.target.files || []));
  };

  const handleFileDrop = (files) => {
    updateSelectedFiles(files);
  };

  const handleUpload = async () => {
    if (selectedFiles.length === 0) {
      setError("Please choose at least one .txt file first.");
      return;
    }
    if (clientConfig.accessTokenRequired && !accessToken.trim()) {
      setError("Enter the Sortify access token before uploading files.");
      return;
    }

    const formData = new FormData();
    selectedFiles.forEach((file) => {
      formData.append("files", file);
    });

    try {
      setLoading(true);
      setError("");
      setResponseData(null);

      const response = await fetch("/api/files/process", {
        method: "POST",
        headers: accessToken ? { "X-Sortify-Access-Token": accessToken } : {},
        body: formData,
      });

      if (!response.ok) {
        let errorMessage = `Server error ${response.status}.`;
        try {
          const errorData = await response.json();
          errorMessage = errorData.message || errorMessage;
        } catch {
          const errorText = await response.text();
          if (errorText) {
            errorMessage = `Server error ${response.status}: ${errorText}`;
          }
        }
        throw new Error(errorMessage);
      }

      const data = await response.json();
      setResponseData(normalizeResponseData(data));
    } catch (err) {
      setError(err.message || "Failed to connect to backend.");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleDownloadTxt = () => {
    if (!responseData) return;

    let textContent = "";

    mergedCategories.forEach((group) => {
      textContent += `${group.category}\n`;
      textContent += `${"-".repeat(group.category.length)}\n`;

      group.values.forEach((value) => {
        textContent += `- ${value}\n`;
      });

      textContent += `\n`;
    });

    downloadTextFile("sortify-merged-results.txt", textContent);
  };

  const handleDownloadPerFileTxt = () => {
    if (!responseData?.files?.length) return;

    responseData.files.forEach((file) => {
      downloadTextFile(buildPerFileDownloadName(file.fileName), buildPerFileTextContent(file));
    });
  };

  const handleDownloadSingleFileTxt = (file) => {
    downloadTextFile(buildPerFileDownloadName(file.fileName), buildPerFileTextContent(file));
  };

  const handleClearCachedSession = async () => {
    setSelectedFiles([]);
    setResponseData(null);
    setError("");

    try {
      await clearCachedFiles();
    } catch {
      // Ignore cache clear failures and still clear in-memory state.
    }
  };

  const toggleTheme = () => {
    setTheme((currentTheme) => (currentTheme === "light" ? "dark" : "light"));
  };

  const handleRememberDataChange = async (enabled) => {
    setRememberData(enabled);
    setError("");

    if (!enabled) {
      setSelectedFiles([]);
      setResponseData(null);
      try {
        await clearCachedFiles();
      } catch {
        // Ignore cache clear failures.
      }
    }
  };

  const uploadLimitLabel = `Up to ${clientConfig.maxFiles} files, ${formatBytes(clientConfig.maxFileSizeBytes)} each, ${formatBytes(clientConfig.maxTotalUploadBytes)} total.`;

  const handleCategoryChange = (fileName, clientId, nextCategory) => {
    setResponseData((currentData) => {
      if (!currentData?.files?.length) {
        return currentData;
      }

      return normalizeResponseData({
        ...currentData,
        files: currentData.files.map((file) => {
          if (file.fileName !== fileName) {
            return file;
          }

          return {
            ...file,
            items: file.items.map((item) =>
              item.clientId === clientId ? { ...item, category: nextCategory } : item
            ),
          };
        }),
      });
    });
  };

  return (
      <div className={`page theme-${theme} ${visualEffectsEnabled ? "effects-on" : "effects-off"}`}>
        <div className="container">
          <div className="hero">
            <div className="hero-top">
              <div className={`settings-menu ${isSettingsOpen ? "open" : ""}`} ref={settingsRef}>
                <button
                    type="button"
                    className="settings-toggle"
                    onClick={() => setIsSettingsOpen((open) => !open)}
                    aria-expanded={isSettingsOpen}
                    aria-haspopup="true"
                    aria-label="Open settings"
                >
                  <span className="settings-gear" aria-hidden="true">⚙</span>
                </button>

                {isSettingsOpen && (
                    <div className="settings-panel">
                      <div className="settings-header">
                        <p className="settings-title">Settings</p>
                      </div>

                      <div className="settings-section">
                        <p className="settings-label">Appearance</p>
                        <div className="settings-row">
                          <span className="settings-row-label">Current theme</span>
                          <span className="settings-row-value">{theme === "light" ? "Light" : "Dark"}</span>
                        </div>
                        <div className="settings-row">
                          <span className="settings-row-label">Visual effects</span>
                          <span className="settings-row-value">{visualEffectsEnabled ? "Enabled" : "Disabled"}</span>
                        </div>
                        <button
                            type="button"
                            className="settings-chip"
                            onClick={toggleTheme}
                        >
                          {theme === "light" ? "Switch to Dark Mode" : "Switch to Light Mode"}
                        </button>
                        <button
                            type="button"
                            className="settings-chip"
                            onClick={() => setVisualEffectsEnabled((enabled) => !enabled)}
                        >
                          {visualEffectsEnabled ? "Turn Off Visual Effects" : "Turn On Visual Effects"}
                        </button>
                        <p className="settings-note">
                          Leave this off for the fastest scrolling and the simplest layout rendering.
                        </p>
                      </div>

                      <div className="settings-section">
                        <p className="settings-label">Privacy</p>
                        <div className="settings-row">
                          <span className="settings-row-label">Browser storage</span>
                          <span className="settings-row-value">{rememberData ? "Enabled" : "Disabled"}</span>
                        </div>
                        <label className="remember-toggle">
                          <input
                              type="checkbox"
                              checked={rememberData}
                              onChange={(event) => handleRememberDataChange(event.target.checked)}
                          />
                          <span>Remember files and results on this browser</span>
                        </label>
                        <p className="privacy-note">
                          Leave this off for medical, legal, or other sensitive files.
                        </p>
                        <button
                            type="button"
                            className="settings-chip settings-chip-secondary"
                            onClick={handleClearCachedSession}
                        >
                          Clear saved files and results
                        </button>
                      </div>

                      {clientConfig.accessTokenRequired && (
                          <div className="settings-section">
                            <p className="settings-label">Access</p>
                            <div className="settings-row">
                              <span className="settings-row-label">Protection</span>
                              <span className="settings-row-value">Required</span>
                            </div>
                            <div className="access-token-field">
                              <label htmlFor="sortify-access-token">Access token</label>
                              <input
                                  id="sortify-access-token"
                                  type="password"
                                  value={accessToken}
                                  onChange={(event) => setAccessToken(event.target.value)}
                                  placeholder="Enter the Sortify access token"
                                  autoComplete="off"
                              />
                            </div>
                          </div>
                      )}

                      <div className="settings-section">
                        <p className="settings-label">Limits</p>
                        <div className="settings-row">
                          <span className="settings-row-label">Uploads</span>
                          <span className="settings-row-value">{clientConfig.maxFiles} files max</span>
                        </div>
                        <div className="settings-row">
                          <span className="settings-row-label">Per file</span>
                          <span className="settings-row-value">{formatBytes(clientConfig.maxFileSizeBytes)}</span>
                        </div>
                        <div className="settings-row">
                          <span className="settings-row-label">Total</span>
                          <span className="settings-row-value">{formatBytes(clientConfig.maxTotalUploadBytes)}</span>
                        </div>
                        <p className="settings-note">{uploadLimitLabel}</p>
                      </div>

                      <div className="settings-section">
                        <p className="settings-label">About</p>
                        <div className="settings-row">
                          <span className="settings-row-label">App</span>
                          <span className="settings-row-value">Sortify local build</span>
                        </div>
                        <div className="settings-row">
                          <span className="settings-row-label">Purpose</span>
                          <span className="settings-row-value">AI text organization</span>
                        </div>
                      </div>

                      <p className="settings-footer">made with {"<3"} by Elvis :)</p>
                    </div>
                )}
              </div>
            </div>

            <h1>Sortify</h1>
            <p className="subtitle">
              Upload one or more text files and organize their lines into clean, structured groups.
            </p>
          </div>

          <UploadPanel
              selectedFiles={selectedFiles}
              onFileChange={handleFileChange}
              onFileDrop={handleFileDrop}
              onUpload={handleUpload}
              uploadLimitLabel={uploadLimitLabel}
              loading={loading}
          />

          {error && <p className="error">{error}</p>}

          <Results
              data={responseData ? { ...responseData, combinedCategories: mergedCategories } : responseData}
              categoryOptions={clientConfig.allowedCategories}
              onCategoryChange={handleCategoryChange}
              onDownloadTxt={handleDownloadTxt}
              onDownloadPerFileTxt={handleDownloadPerFileTxt}
              onDownloadSingleFileTxt={handleDownloadSingleFileTxt}
          />
        </div>
      </div>
  );
}

export default App;
