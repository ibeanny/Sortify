import { useEffect, useState } from "react";
import "./App.css";
import UploadPanel from "./components/UploadPanel";
import Results from "./components/Results";

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

function App() {
  const [theme, setTheme] = useState(() => {
    return localStorage.getItem("sortify-theme") || "light";
  });

  const [selectedFiles, setSelectedFiles] = useState([]);
  const [responseData, setResponseData] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    localStorage.setItem("sortify-theme", theme);
  }, [theme]);

  const updateSelectedFiles = (files) => {
    setSelectedFiles(files);
    setResponseData(null);
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
        body: formData,
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Server error ${response.status}: ${errorText}`);
      }

      const data = await response.json();
      setResponseData(data);
    } catch (err) {
      setError(err.message || "Failed to connect to backend.");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleDownloadTxt = () => {
    if (!responseData) return;

    let textContent = "SORTIFY RESULTS\n\n";

    responseData.files?.forEach((file) => {
      const groupedItems = groupItemsByCategory(file.items);
      textContent += `File: ${file.fileName}\n`;
      textContent += `${"=".repeat(file.fileName.length + 6)}\n`;

      Object.entries(groupedItems).forEach(([category, values]) => {
        textContent += `${category}\n`;
        textContent += `${"-".repeat(category.length)}\n`;

        values.forEach((value) => {
          textContent += `- ${value}\n`;
        });

        textContent += `\n`;
      });
    });

    responseData.combinedCategories.forEach((group) => {
      textContent += `${group.category}\n`;
      textContent += `${"-".repeat(group.category.length)}\n`;

      group.values.forEach((value) => {
        textContent += `- ${value}\n`;
      });

      textContent += `\n`;
    });

    textContent += `Total Files: ${responseData.totalFiles}\n`;
    textContent += `Total Lines: ${responseData.totalLines}\n`;

    downloadTextFile("sortify-results.txt", textContent);
  };

  const handleDownloadPerFileTxt = () => {
    if (!responseData?.files?.length) return;

    responseData.files.forEach((file) => {
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

      const downloadName = file.fileName.replace(/\.txt$/i, "") + "-sorted.txt";
      downloadTextFile(downloadName, textContent);
    });
  };

  const toggleTheme = () => {
    setTheme((currentTheme) => (currentTheme === "light" ? "dark" : "light"));
  };

  return (
      <div className={`page theme-${theme}`}>
        <div className="container">
          <div className="hero">
            <div className="hero-top">
              <p className="eyebrow">Text organization</p>

              <button
                  type="button"
                  className="theme-toggle"
                  onClick={toggleTheme}
              >
                {theme === "light" ? "Dark mode" : "Light mode"}
              </button>
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
              loading={loading}
          />

          {error && <p className="error">{error}</p>}

          <Results
              data={responseData}
              onDownloadTxt={handleDownloadTxt}
              onDownloadPerFileTxt={handleDownloadPerFileTxt}
          />
        </div>
      </div>
  );
}

export default App;
