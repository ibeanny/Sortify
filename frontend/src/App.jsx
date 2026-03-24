import { useEffect, useState } from "react";
import "./App.css";
import UploadPanel from "./components/UploadPanel";
import Results from "./components/Results";

function App() {
  const [theme, setTheme] = useState(() => {
    return localStorage.getItem("sortify-theme") || "light";
  });

  const [selectedFile, setSelectedFile] = useState(null);
  const [responseData, setResponseData] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    localStorage.setItem("sortify-theme", theme);
  }, [theme]);

  const handleFileChange = (event) => {
    const file = event.target.files[0];
    setSelectedFile(file || null);
    setResponseData(null);
    setError("");
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setError("Please choose a .txt file first.");
      return;
    }

    const formData = new FormData();
    formData.append("files", selectedFile);

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

    const blob = new Blob([textContent], { type: "text/plain" });
    const url = URL.createObjectURL(blob);

    const link = document.createElement("a");
    link.href = url;
    link.download = "sortify-results.txt";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    URL.revokeObjectURL(url);
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
              Upload a text file and organize its lines into clean, structured groups.
            </p>
          </div>

          <UploadPanel
              selectedFile={selectedFile}
              onFileChange={handleFileChange}
              onUpload={handleUpload}
              loading={loading}
          />

          {error && <p className="error">{error}</p>}

          <Results data={responseData} onDownloadTxt={handleDownloadTxt} />
        </div>
      </div>
  );
}

export default App;