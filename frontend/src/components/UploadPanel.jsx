import { useEffect, useState } from "react";

function UploadPanel({ selectedFile, onFileChange, onUpload, loading }) {
    return (
        <div className="upload-panel">
            <div className="file-section">
                <label htmlFor="file-upload" className="file-upload-label">
                    Choose Text File
                </label>

                <input
                    id="file-upload"
                    type="file"
                    accept=".txt"
                    onChange={onFileChange}
                    className="file-input"
                />

                {selectedFile && (
                    <p className="file-name">
                        Selected: <span>{selectedFile.name}</span>
                    </p>
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