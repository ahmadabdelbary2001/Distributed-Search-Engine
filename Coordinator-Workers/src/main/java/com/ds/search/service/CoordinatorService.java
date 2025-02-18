package com.ds.search.service;

import com.ds.search.model.Document;
import com.ds.search.model.SearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class CoordinatorService {
    private static final Logger logger = LoggerFactory.getLogger(CoordinatorService.class);
    private final ServiceRegistry serviceRegistry;

    public CoordinatorService(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    public List<Document> distributeSearchQuery(String query) {
        List<Document> aggregatedResults = new ArrayList<>();
        RestTemplate restTemplate = new RestTemplate();

        try {
            List<String> workers = serviceRegistry.getRegisteredWorkers();
            logger.info("Distributing search query to {} workers: {}", workers.size(), workers);

            for (String worker : workers) {
                try {
                    String workerUrl = worker + "/api/worker/search";

                    // Create HTTP entity with the query
                    HttpEntity<SearchRequest> requestEntity = new HttpEntity<>(new SearchRequest(query));

                    // Use ParameterizedTypeReference to deserialize JSON response
                    ResponseEntity<List<Document>> response = restTemplate.exchange(
                            workerUrl,
                            HttpMethod.POST,
                            requestEntity,
                            new ParameterizedTypeReference<>() {
                            });

                    List<Document> workerResults = response.getBody();
                    if (workerResults != null) {
                        aggregatedResults.addAll(workerResults);
                        logger.info("Received {} results from worker: {}", workerResults.size(), worker);
                    }
                } catch (Exception e) {
                    logger.error("Error communicating with worker {}: {}", worker, e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            logger.error("Failed to distribute search query: {}", e.getMessage(), e);
        }

        logger.info("Aggregated {} results from all workers", aggregatedResults.size());
        return aggregateAndRankResults(aggregatedResults);
    }

    private List<Document> aggregateAndRankResults(List<Document> results) {
        List<Document> rankedResults = results.stream()
                .sorted((d1, d2) -> Double.compare(d2.getRelevanceScore(), d1.getRelevanceScore()))
                .limit(10)
                .toList();

        logger.info("Ranked and limited results to top 10");
        return rankedResults;
    }
}