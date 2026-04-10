import { useEffect, useRef, useState } from "react";
import "./App.css";
import UploadPanel from "./components/UploadPanel";
import Results from "./components/Results";

const CATEGORY_OPTIONS = [
  "Tasks & Reminders",
  "Appointments & Schedule",
  "Contacts",
  "Travel",
  "Finance",
  "Shopping & Orders",
  "Events & Dates",
  "Medical",
  "Legal",
  "Work & School",
  "Reference",
  "Other",
];
const RESULTS_CACHE_KEY = "sortify-last-results";
const REMEMBER_DATA_KEY = "sortify-remember-data";
const ACCESS_TOKEN_KEY = "sortify-access-token";
const VISUAL_EFFECTS_KEY = "sortify-visual-effects";
const FILE_CACHE_DB = "sortify-file-cache";
const FILE_CACHE_STORE = "uploads";
const FILE_CACHE_KEY = "selected-files";

function formatBytes(bytes) {
  if (!bytes && bytes !== 0) {
    return "";
  }
  if (bytes >= 1024 * 1024) {
    return `${Math.round(bytes / (1024 * 1024))} MB`;
  }
  if (bytes >= 1024) {
    return `${Math.round(bytes / 1024)} KB`;
  }
  return `${bytes} bytes`;
}

function groupItemsByCategory(items = []) {
  return items.reduce((groups, item) => {
    const category = item.category || "Uncategorized";
    if (!groups[category]) {
      groups[category] = [];
    }

    groups[category].push(item.value);
    return groups;
  }, {});
}

function downloadTextFile(filename, content) {
  const blob = new Blob([content], { type: "text/plain" });
  const url = URL.createObjectURL(blob);

  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);

  URL.revokeObjectURL(url);
}

function buildPerFileTextContent(file) {
  const groupedItems = groupItemsByCategory(file.items);
  let textContent = `${file.fileName}\n`;
  textContent += `${"=".repeat(file.fileName.length)}\n\n`;

  Object.entries(groupedItems).forEach(([category, values]) => {
    textContent += `${category}\n`;
    textContent += `${"-".repeat(category.length)}\n`;

    values.forEach((value) => {
      textContent += `- ${value}\n`;
    });

    textContent += `\n`;
  });

  return textContent;
}

function buildPerFileDownloadName(fileName) {
  return fileName.replace(/\.txt$/i, "") + "-sorted.txt";
}

function buildMergedCategories(responseData) {
  if (responseData?.files?.length) {
    const grouped = new Map();

    responseData.files.forEach((file) => {
      file.items?.forEach((item) => {
        const category = item.category || "Other";

        if (!grouped.has(category)) {
          grouped.set(category, []);
        }

        grouped.get(category).push(item.value);
      });
    });

    return Array.from(grouped.entries()).map(([key, values]) => ({
      category: key,
      values,
    }));
  }

  return responseData?.combinedCategories || [];
}

function normalizeResponseData(data) {
  if (!data?.files?.length) {
    return data;
  }

  return {
    ...data,
    files: data.files.map((file, fileIndex) => ({
      ...file,
      items: (file.items || []).map((item, itemIndex) => ({
        ...item,
        clientId:
          item.clientId ||
          `${file.fileName || "file"}-${fileIndex}-${itemIndex}-${item.value || "item"}`,
      })),
    })),
  };
}

function openFileCache() {
  return new Promise((resolve, reject) => {
    const request = window.indexedDB.open(FILE_CACHE_DB, 1);

    request.onupgradeneeded = () => {
      const database = request.result;
      if (!database.objectStoreNames.contains(FILE_CACHE_STORE)) {
        database.createObjectStore(FILE_CACHE_STORE, { keyPath: "id" });
      }
    };

    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

async function saveCachedFiles(files) {
  if (!window.indexedDB) {
    return;
  }

  const database = await openFileCache();

  await new Promise((resolve, reject) => {
    const transaction = database.transaction(FILE_CACHE_STORE, "readwrite");
    transaction.objectStore(FILE_CACHE_STORE).put({ id: FILE_CACHE_KEY, files });
    transaction.oncomplete = () => resolve();
    transaction.onerror = () => reject(transaction.error);
  });

  database.close();
}

async function loadCachedFiles() {
  if (!window.indexedDB) {
    return [];
  }

  const database = await openFileCache();

  const result = await new Promise((resolve, reject) => {
    const transaction = database.transaction(FILE_CACHE_STORE, "readonly");
    const request = transaction.objectStore(FILE_CACHE_STORE).get(FILE_CACHE_KEY);
    request.onsuccess = () => resolve(request.result?.files || []);
    request.onerror = () => reject(request.error);
  });

  database.close();
  return result;
}

async function clearCachedFiles() {
  if (!window.indexedDB) {
    return;
  }

  const database = await openFileCache();

  await new Promise((resolve, reject) => {
    const transaction = database.transaction(FILE_CACHE_STORE, "readwrite");
    transaction.objectStore(FILE_CACHE_STORE).delete(FILE_CACHE_KEY);
    transaction.oncomplete = () => resolve();
    transaction.onerror = () => reject(transaction.error);
  });

  database.close();
}

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
          setClientConfig(config);
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

    if (rememberData) {
      saveCachedFiles(files).catch(() => {
        // Ignore file cache failures; selection still works for the current session.
      });
    }
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
              categoryOptions={CATEGORY_OPTIONS}
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
