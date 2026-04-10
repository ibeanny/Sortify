package com.ibeanny.aisorter.service;

import com.ibeanny.aisorter.model.ClassifiedLine;
import com.ibeanny.aisorter.model.CombinedCategoryGroup;
import com.ibeanny.aisorter.model.ProcessedFileResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CombinedResultsBuilder {

    public List<CombinedCategoryGroup> build(List<ProcessedFileResult> processedFiles) {
        Map<String, List<String>> grouped = new LinkedHashMap<>();

        for (ProcessedFileResult fileResult : processedFiles) {
            for (ClassifiedLine item : fileResult.getItems()) {
                grouped.computeIfAbsent(item.getCategory(), key -> new ArrayList<>())
                        .add(item.getValue());
            }
        }

        List<CombinedCategoryGroup> combined = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
            combined.add(new CombinedCategoryGroup(entry.getKey(), entry.getValue()));
        }

        return combined;
    }
}
