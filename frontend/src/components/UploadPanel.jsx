import { useRef, useState } from "react";

function UploadPanel({
    selectedFiles,
    onFileChange,
    onFileDrop,
    onUpload,
    uploadLimitLabel,
    loading,
}) {
    const [isDragging, setIsDragging] = useState(false);
    const fileInputRef = useRef(null);

    const stopDragDefaults = (event) => {
        event.preventDefault();
        event.stopPropagation();
    };

    const handleDragEnter = (event) => {
        stopDragDefaults(event);
        setIsDragging(true);
    };

    const handleDragOver = (event) => {
        stopDragDefaults(event);
        setIsDragging(true);
    };

    const handleDragLeave = (event) => {
        stopDragDefaults(event);
        if (event.currentTarget.contains(event.relatedTarget)) {
            return;
        }

        setIsDragging(false);
    };

    const handleDrop = (event) => {
        stopDragDefaults(event);
        setIsDragging(false);

        const files = Array.from(event.dataTransfer?.files || []).filter(
            (file) => file.name.toLowerCase().endsWith(".txt")
        );

        if (files.length > 0) {
            onFileDrop(files);
            if (fileInputRef.current) {
                fileInputRef.current.value = "";
            }
        }
    };

    const handleInputClick = (event) => {
        event.target.value = "";
    };

    const handleInputChange = (event) => {
        onFileChange(event);
        event.target.value = "";
    };

    return (
        <div
            className={`upload-panel ${isDragging ? "drag-active" : ""}`}
            onDragEnter={handleDragEnter}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
        >
            <div className="file-section">
                <p className="upload-guidance">{uploadLimitLabel}</p>

                <label htmlFor="file-upload" className="file-upload-label">
                    Choose Text Files
                </label>

                <input
                    ref={fileInputRef}
                    id="file-upload"
                    type="file"
                    accept=".txt"
                    multiple
                    onClick={handleInputClick}
                    onChange={handleInputChange}
                    className="file-input"
                />

                {selectedFiles.length > 0 && (
                    <div className="file-selection">
                        <p className="file-name">
                            Selected <span>{selectedFiles.length}</span> file{selectedFiles.length === 1 ? "" : "s"}
                        </p>

                        <ul className="file-list">
                            {selectedFiles.map((file) => (
                                <li key={`${file.name}-${file.lastModified}`}>{file.name}</li>
                            ))}
                        </ul>
                    </div>
                )}
            </div>

            <div className="upload-section">
                <button onClick={onUpload} disabled={loading}>
                    {loading ? (
                        <span className="loading-content">
                            <span className="spinner"></span>
                            Processing
                        </span>
                    ) : (
                        "Upload & Sort"
                    )}
                </button>
            </div>
        </div>
    );
}

export default UploadPanel;
