package com.ds.search.util;

import com.ds.search.model.Document;

import java.util.List;
import java.util.stream.Collectors;

public class ResultAggregator {
    public static List<Document> aggregateResults(List<Document> results) {
        return results.stream()
                .sorted((d1, d2) -> Double.compare(d2.getRelevanceScore(), d1.getRelevanceScore()))
                .limit(10)
                .collect(Collectors.toList());
    }
}
