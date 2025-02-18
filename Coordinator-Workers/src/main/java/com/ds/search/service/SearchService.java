package com.ds.search.service;

import com.ds.search.model.Document;
import com.ds.search.util.Tokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchService {
    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);
    private Map<String, Double> globalIDF = new HashMap<>();

    // Update global IDF values
    public void updateGlobalIDF(Map<String, Double> globalIDF) {
        this.globalIDF = globalIDF;
        logger.info("Updated global IDF with {} terms", globalIDF.size());
    }

    // Calculate Term Frequency (TF)
    public double calculateTF(String term, String documentContent) {
        List<String> tokens = Tokenizer.tokenize(documentContent);
        long termCount = tokens.stream()
                .filter(word -> word.contains(term)) // Allow partial matches
                .count();
        double tf = (double) termCount / tokens.size();
        logger.debug("Calculated TF for term '{}': {}", term, tf);
        return tf;
    }

    // Calculate TF-IDF for a term in a specific document using global IDF
    public double calculateTFIDF(String term, String documentContent) {
        double tf = calculateTF(term, documentContent);
        double idf = globalIDF.getOrDefault(term, 0.0); // Use global IDF
        double tfidf = tf * idf;
        logger.debug("Calculated TF-IDF for term '{}': {}", term, tfidf);
        return tfidf;
    }

    // Perform search and rank documents using global IDF
    public List<Document> search(String query, List<Document> documents) {
        List<String> queryTerms = Tokenizer.tokenize(query);
        logger.info("Performing search for query: {}", query);

        for (Document doc : documents) {
            // Calculate the relevance score as the sum of TF-IDF scores for all query terms
            double score = queryTerms.stream()
                    .mapToDouble(term -> calculateTFIDF(term, doc.getContent()))
                    .sum();
            doc.setRelevanceScore(score);
            logger.debug("Calculated relevance score for document '{}': {}", doc.getId(), score);
        }

        // Filter out documents with relevanceScore == 0
        // Sort documents by relevance score in descending order
        // Limit the results to 10
        List<Document> results = documents.stream()
                .filter(doc -> doc.getRelevanceScore() > 0) // Exclude documents with relevanceScore == 0
                .sorted((d1, d2) -> Double.compare(d2.getRelevanceScore(), d1.getRelevanceScore())) // Sort descending
                .limit(10) // Limit results to 10
                .collect(Collectors.toList());

        logger.info("Found {} relevant documents for query: {}", results.size(), query);
        return results;
    }
}