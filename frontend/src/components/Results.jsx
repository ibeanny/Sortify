import { useEffect, useRef, useState } from "react";
import { groupItemsByCategory } from "../utils/results";

function matchesSearch(item, query) {
    if (!query) {
        return true;
    }

    const haystack = `${item.category || ""} ${item.value || ""}`.toLowerCase();
    return haystack.includes(query);
}

function Results({
    data,
    categoryOptions,
    onCategoryChange,
    onDownloadTxt,
    onDownloadPerFileTxt,
    onDownloadSingleFileTxt,
}) {
    const [isDownloadMenuOpen, setIsDownloadMenuOpen] = useState(false);
    const [isEditMode, setIsEditMode] = useState(false);
    const [searchQuery, setSearchQuery] = useState("");
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

    const normalizedQuery = searchQuery.trim().toLowerCase();
    const filteredFiles = (data.files || [])
        .map((file) => ({
            ...file,
            items: (file.items || []).filter((item) => matchesSearch(item, normalizedQuery)),
        }))
        .filter((file) => file.items.length > 0);

    const filteredCombinedCategories = (data.combinedCategories || [])
        .map((group) => ({
            ...group,
            values: (group.values || []).filter((value) => {
                if (!normalizedQuery) {
                    return true;
                }

                return `${group.category} ${value}`.toLowerCase().includes(normalizedQuery);
            }),
        }))
        .filter((group) => group.values.length > 0);

    const simplifiedJson = {
        combinedCategories: data.combinedCategories,
        totalFiles: data.totalFiles,
        totalLines: data.totalLines,
    };

    return (
        <div className="response-box">
            <div className="results-header">
                <h2>Results</h2>
                <div className="results-meta" aria-label="Result summary">
                    <span><strong>{data.totalFiles}</strong> files</span>
                    <span className="results-meta-divider" aria-hidden="true">/</span>
                    <span><strong>{data.totalLines}</strong> lines</span>
                </div>
                <div className="results-search">
                    <input
                        type="search"
                        value={searchQuery}
                        onChange={(event) => setSearchQuery(event.target.value)}
                        placeholder="Search categories or lines"
                        aria-label="Search sorted results"
                    />
                </div>
            </div>

            {filteredFiles.length > 0 && (
                <div className="categories-section">
                    <div className="section-heading">
                        <div className="section-heading-row">
                            <h3>Files</h3>
                            <button
                                type="button"
                                className={`results-edit-button ${isEditMode ? "active" : ""}`}
                                onClick={() => setIsEditMode((current) => !current)}
                            >
                                {isEditMode ? "Done Editing" : "Edit Categories"}
                            </button>
                        </div>
                        <p>Open each file to review its grouped contents.</p>
                    </div>
                    <div className="category-grid">
                        {filteredFiles.map((file) => (
                            <details className="category-card collapsible-card" key={file.fileName}>
                                <summary className="card-summary">
                                    <h4>{file.fileName}</h4>
                                </summary>
                                <div className="card-content">
                                    <div className="card-content-inner">
                                        {Object.entries(groupItemsByCategory(file.items)).map(([category, values]) => (
                                            <div className="file-category-group" key={`${file.fileName}-${category}`}>
                                                <h5>{category}</h5>
                                                <ul>
                                                    {values.map((item) => {
                                                        const selectOptions = categoryOptions.includes(item.category)
                                                            ? categoryOptions
                                                            : [item.category, ...categoryOptions];

                                                        return (
                                                            <li key={item.clientId} className="editable-line-item">
                                                                <span className="line-item-text">{item.value}</span>
                                                                {isEditMode && (
                                                                    <label className="line-item-category">
                                                                        <span>Category</span>
                                                                        <select
                                                                            value={item.category}
                                                                            onChange={(event) =>
                                                                                onCategoryChange(file.fileName, item.clientId, event.target.value)
                                                                            }
                                                                        >
                                                                            {selectOptions.map((option) => (
                                                                                <option key={option} value={option}>
                                                                                    {option}
                                                                                </option>
                                                                            ))}
                                                                        </select>
                                                                    </label>
                                                                )}
                                                            </li>
                                                        );
                                                    })}
                                                </ul>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            </details>
                        ))}
                    </div>
                </div>
            )}

            {filteredCombinedCategories.length > 0 && (
                <div className="categories-section">
                    <div className="section-heading">
                        <h3>Combined Results</h3>
                        <p>One merged view across every uploaded file.</p>
                    </div>
                    <details className="category-card collapsible-card combined-card">
                        <summary className="card-summary">
                            <h4>All Files Combined</h4>
                        </summary>
                        <div className="card-content">
                            <div className="card-content-inner">
                                {filteredCombinedCategories.map((group, index) => (
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
                        </div>
                    </details>
                </div>
            )}

            {normalizedQuery && filteredFiles.length === 0 && filteredCombinedCategories.length === 0 && (
                <p className="results-empty-state">No lines matched your search.</p>
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
