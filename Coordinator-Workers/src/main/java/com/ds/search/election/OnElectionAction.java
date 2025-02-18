package com.ds.search.election;

import com.ds.search.service.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class OnElectionAction {
    private static final Logger logger = LoggerFactory.getLogger(OnElectionAction.class);
    private final ServiceRegistry serviceRegistry;
    private final int port;

    @Value("${webserver.url}")
    private String webServerUrl; // Address of the web server (configured in application.properties)

    @Autowired
    public OnElectionAction(ServiceRegistry serviceRegistry, @Value("${election.port}") int port) {
        this.serviceRegistry = serviceRegistry;
        this.port = port;
    }

    public void onElectedToBeCoordinator() {
        try {
            String currentNodeAddress = getCurrentNodeAddress();
            serviceRegistry.registerAsCoordinator(currentNodeAddress);
            logger.info("Elected as Coordinator: {}", currentNodeAddress);

            notifyWebServer(currentNodeAddress); // Notify the web server

            // Calculate and broadcast global IDF
            List<Map<String, Long>> localDFs = collectLocalDFsFromWorkers();
            long totalDocuments = calculateTotalDocuments(localDFs);
            serviceRegistry.calculateAndBroadcastGlobalIDF(localDFs, totalDocuments);
        } catch (Exception e) {
            logger.error("Failed to register as leader: {}", e.getMessage(), e);
        }
    }

    public void onWorker() {
        try {
            String currentNodeAddress = getCurrentNodeAddress();
            serviceRegistry.registerAsWorker(currentNodeAddress);
            logger.info("Registered as Worker: {}", currentNodeAddress);
        } catch (Exception e) {
            logger.error("Failed to register as worker: {}", e.getMessage(), e);
        }
    }

    private void notifyWebServer(String coordinatorAddress) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            String url = webServerUrl + "/api/web/coordinator"; // Web server endpoint to update coordinator
            restTemplate.postForObject(url, coordinatorAddress, String.class);
            logger.info("Notified web server of new coordinator: {}", coordinatorAddress);
        } catch (Exception e) {
            logger.error("Failed to notify web server of new coordinator: {}", e.getMessage(), e);
        }
    }

    private String getCurrentNodeAddress() throws Exception {
        return String.format("http://%s:%d", InetAddress.getLocalHost().getCanonicalHostName(), port);
    }

    private List<Map<String, Long>> collectLocalDFsFromWorkers() {
        List<Map<String, Long>> localDFs = new ArrayList<>();
        RestTemplate restTemplate = new RestTemplate();

        try {
            List<String> workers = serviceRegistry.getRegisteredWorkers();
            logger.info("Collecting local DFs from {} workers: {}", workers.size(), workers);

            for (String worker : workers) {
                try {
                    String workerUrl = worker + "/api/worker/get-local-df";
                    ResponseEntity<Map<String, Long>> response = restTemplate.exchange(
                            workerUrl,
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<>() {
                            });
                    Map<String, Long> localDF = response.getBody();
                    if (localDF != null) {
                        localDFs.add(localDF);
                        logger.info("Collected local DF from worker: {}", worker);
                    }
                } catch (Exception e) {
                    logger.error("Error collecting local DF from worker {}: {}", worker, e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to collect local DFs: {}", e.getMessage(), e);
        }

        logger.info("Collected local DFs from {} workers", localDFs.size());
        return localDFs;
    }

    private long calculateTotalDocuments(List<Map<String, Long>> localDFs) {
        long totalDocuments = 0;
        for (Map<String, Long> localDF : localDFs) {
            totalDocuments += localDF.values().stream().mapToLong(Long::longValue).sum();
        }
        logger.info("Calculated total documents: {}", totalDocuments);
        return totalDocuments;
    }
}