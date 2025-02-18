package com.ds.search.controller;

import com.ds.search.model.Document;
import com.ds.search.model.SearchRequest;
import com.ds.search.service.SearchService;
import com.ds.search.util.Tokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/worker")
public class WorkerController {
    @Value("${election.port}")
    private int port;

    private final SearchService searchService;

    public WorkerController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/role")
    public String getRole() {
        return "Worker";
    }

    @PostMapping("/search")
    public List<Document> searchDocuments(@RequestBody SearchRequest searchRequest) {
        List<Document> documents = loadDocumentsFromFiles();
        return searchService.search(searchRequest.getQuery(), documents);
    }

    @GetMapping("/get-local-df")
    public Map<String, Long> getLocalDF() {
        List<Document> documents = loadDocumentsFromFiles();
        return calculateLocalDF(documents);
    }

    private List<Document> loadDocumentsFromFiles() {
        List<Document> documents = new ArrayList<>();
        try {
            Path folderPath = Paths.get("src/main/resources/documents");

            Files.list(folderPath).forEach(filePath -> {
                if (filePath.toString().endsWith(".txt")) {
                    try {
                        String content = Files.readString(filePath);
                        documents.add(new Document(filePath.getFileName().toString(), content));
                    } catch (IOException e) {
                        System.err.println("Error reading file: " + filePath + " - " + e.getMessage());
                    }
                }
            });
        } catch (IOException e) {
            System.err.println("Error reading documents directory: " + e.getMessage());
        }
        return documents;
    }

    private Map<String, Long> calculateLocalDF(List<Document> documents) {
        Map<String, Long> localDF = new HashMap<>();
        for (Document doc : documents) {
            List<String> terms = Tokenizer.tokenize(doc.getContent());
            terms.stream()
                    .distinct() // Count each term only once per document
                    .forEach(term -> localDF.put(term, localDF.getOrDefault(term, 0L) + 1));
        }
        return localDF;
    }

    @PostMapping("/update-idf")
    public void updateGlobalIDF(@RequestBody Map<String, Double> globalIDF) {
        searchService.updateGlobalIDF(globalIDF);
    }
}