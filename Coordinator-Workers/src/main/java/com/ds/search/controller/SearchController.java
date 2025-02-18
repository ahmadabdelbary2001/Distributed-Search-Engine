package com.ds.search.controller;

import com.ds.search.model.Document;
import com.ds.search.model.SearchRequest;
import com.ds.search.service.CoordinatorService;
import com.ds.search.service.ServiceRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coordinator")
public class SearchController {
    @Value("${election.port}")
    private int port;
    @Autowired
    private ServiceRegistry serviceRegistry;

    @Autowired
    private CoordinatorService coordinatorService;

    public String getRole() {
        return serviceRegistry.isCoordinator ? "Coordinator" : "Worker";
    }

    @PostMapping("/search")
    public List<Document> searchDocuments(@RequestBody SearchRequest searchRequest) {
        try {
            String coordinatorAddress = serviceRegistry.getCoordinatorAddress();
            if (serviceRegistry.isCoordinator) {
                // Process the query as coordinator
                return coordinatorService.distributeSearchQuery(searchRequest.getQuery());
            } else {
                // Redirect query to the coordinator
                return redirectToCoordinator(searchRequest.getQuery(), coordinatorAddress);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    @GetMapping("/search")
    public List<Document> searchDocumentsByQuery(@RequestParam String query) {
        try {
            String coordinatorAddress = serviceRegistry.getCoordinatorAddress();
            if (serviceRegistry.isCoordinator) {
                // Process the query as coordinator
                return coordinatorService.distributeSearchQuery(query);
            } else {
                // Redirect query to the coordinator
                return redirectToCoordinator(query, coordinatorAddress);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    @GetMapping("/address")
    public String getCoordinatorAddress() {
        try {
            return serviceRegistry.getCoordinatorAddress();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch coordinator address.");
        }
    }

    private String getCurrentNodeAddress() {
        return String.format("http://%s:%d", "localhost", port);
    }

    private List<Document> redirectToCoordinator(String query, String coordinatorAddress) {
        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            String url = coordinatorAddress + "/api/coordinator/search";
            return restTemplate.postForObject(url, query, List.class);
        } catch (Exception e) {
            System.err.println("Failed to redirect query to coordinator: " + e.getMessage());
            return List.of();
        }
    }
}
