package com.ibeanny.aisorter.model;

import java.util.List;

public class CleanedTextFile {
    private String fileName;
    private List<String> cleanedLines;

    public CleanedTextFile() {
    }

    public CleanedTextFile(String fileName, List<String> cleanedLines) {
        this.fileName = fileName;
        this.cleanedLines = cleanedLines;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<String> getCleanedLines() {
        return cleanedLines;
    }

    public void setCleanedLines(List<String> cleanedLines) {
        this.cleanedLines = cleanedLines;
    }

    public int getLineCount() {
        return cleanedLines == null ? 0 : cleanedLines.size();
    }
}
