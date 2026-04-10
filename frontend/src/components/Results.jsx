import { useEffect, useRef, useState } from "react";

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

function Results({ data, onDownloadTxt, onDownloadPerFileTxt, onDownloadSingleFileTxt }) {
    const [isDownloadMenuOpen, setIsDownloadMenuOpen] = useState(false);
    const downloadMenuRef = useRef(null);

    useEffect(() => {
        if (!isDownloadMenuOpen) {
            return undefined;
        }

        const handlePointerDown = (event) => {
            if (!downloadMenuRef.current?.contains(event.target)) {
                setIsDownloadMenuOpen(false);
            }
        };

        const handleEscape = (event) => {
            if (event.key === "Escape") {
                setIsDownloadMenuOpen(false);
            }
        };

        document.addEventListener("pointerdown", handlePointerDown);
        document.addEventListener("contextmenu", handlePointerDown);
        document.addEventListener("keydown", handleEscape);

        return () => {
            document.removeEventListener("pointerdown", handlePointerDown);
            document.removeEventListener("contextmenu", handlePointerDown);
            document.removeEventListener("keydown", handleEscape);
        };
    }, [isDownloadMenuOpen]);

    if (!data) return null;

    const simplifiedJson = {
        combinedCategories: data.combinedCategories,
        totalFiles: data.totalFiles,
        totalLines: data.totalLines,
    };

    return (
        <div className="response-box">
            <h2>Results</h2>

            <div className="stats">
                <div className="stat-card">
                    <span className="stat-label">Files</span>
                    <span className="stat-value">{data.totalFiles}</span>
                </div>

                <div className="stat-card">
                    <span className="stat-label">Lines</span>
                    <span className="stat-value">{data.totalLines}</span>
                </div>
            </div>

            {data.files?.length > 0 && (
                <div className="categories-section">
                    <h3>Files</h3>
                    <div className="category-grid">
                        {data.files.map((file) => (
                            <details className="category-card collapsible-card" key={file.fileName}>
                                <summary className="card-summary">
                                    <h4>{file.fileName}</h4>
                                </summary>
                                <div className="card-content">
                                    {Object.entries(groupItemsByCategory(file.items)).map(([category, values]) => (
                                        <div className="file-category-group" key={`${file.fileName}-${category}`}>
                                            <h5>{category}</h5>
                                            <ul>
                                                {values.map((value, index) => (
                                                    <li key={`${file.fileName}-${category}-${index}`}>{value}</li>
                                                ))}
                                            </ul>
                                        </div>
                                    ))}
                                </div>
                            </details>
                        ))}
                    </div>
                </div>
            )}

            {data.combinedCategories?.length > 0 && (
                <div className="categories-section">
                    <h3>Combined Results</h3>
                    <details className="category-card collapsible-card combined-card">
                        <summary className="card-summary">
                            <h4>All Files Combined</h4>
                        </summary>
                        <div className="card-content">
                            {data.combinedCategories.map((group, index) => (
                                <div className="file-category-group" key={`${group.category}-${index}`}>
                                    <h5>{group.category}</h5>
                                    <ul>
                                        {group.values.map((value, i) => (
                                            <li key={`${group.category}-${i}`}>{value}</li>
                                        ))}
                                    </ul>
                                </div>
                            ))}
                        </div>
                    </details>
                </div>
            )}

            <div className="results-actions">
                {data.files?.length > 0 && (
                    <div
                        className={`download-menu ${isDownloadMenuOpen ? "open" : ""}`}
                        ref={downloadMenuRef}
                    >
                        <button
                            type="button"
                            className="download-menu-summary"
                            onClick={() => setIsDownloadMenuOpen((open) => !open)}
                            aria-expanded={isDownloadMenuOpen}
                            aria-haspopup="true"
                        >
                            <span>Download Individual Files</span>
                        </button>
                        {isDownloadMenuOpen && (
                            <div className="download-menu-content">
                            {data.files.map((file) => (
                                <button
                                    key={file.fileName}
                                    type="button"
                                    className="download-menu-button"
                                    onClick={() => {
                                        onDownloadSingleFileTxt(file);
                                        setIsDownloadMenuOpen(false);
                                    }}
                                >
                                    {file.fileName}
                                </button>
                            ))}
                            <button
                                type="button"
                                className="download-menu-button download-menu-button-all"
                                onClick={() => {
                                    onDownloadPerFileTxt();
                                    setIsDownloadMenuOpen(false);
                                }}
                            >
                                Download All Individual Files
                            </button>
                            </div>
                        )}
                    </div>
                )}
                <button className="download-merged-button" onClick={onDownloadTxt}>Download Merged TXT</button>
            </div>

            <details className="raw-json">
                <summary>Show raw JSON</summary>
                <pre>{JSON.stringify(simplifiedJson, null, 2)}</pre>
            </details>
        </div>
    );
}

export default Results;
