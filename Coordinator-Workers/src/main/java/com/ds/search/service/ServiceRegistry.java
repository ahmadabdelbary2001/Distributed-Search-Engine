package com.ds.search.service;

import lombok.Getter;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ServiceRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ServiceRegistry.class);
    private final ZooKeeper zooKeeper;
    public boolean isCoordinator;

    @Value("${election.port}")
    private int port;

    // Get global IDF values (used by workers)
    @Getter
    private Map<String, Double> globalIDF; // Stores global IDF values

    public ServiceRegistry(ZookeeperService zookeeperService) {
        this.zooKeeper = zookeeperService.getZooKeeper();
        this.globalIDF = new HashMap<>();
    }

    public void registerAsCoordinator(String address) throws KeeperException, InterruptedException {
        isCoordinator = true;
        String coordinatorZNode = "/search-system/coordinator";
        if (zooKeeper.exists(coordinatorZNode, false) != null) {
            zooKeeper.delete(coordinatorZNode, -1);
        }
        zooKeeper.create(coordinatorZNode, address.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        logger.info("Registered as Coordinator: {}", address);
    }

    public void registerAsWorker(String address) throws KeeperException, InterruptedException {
        isCoordinator = false;
        zooKeeper.create("/search-system/workers/worker-", address.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        logger.info("Registered as Worker: {}", address);
    }

    public String getCoordinatorAddress() throws KeeperException, InterruptedException {
        byte[] data = zooKeeper.getData("/search-system/coordinator", false, null);
        String coordinatorAddress = new String(data);
        logger.info("Fetched Coordinator Address: {}", coordinatorAddress);
        return coordinatorAddress;
    }

    public List<String> getRegisteredWorkers() throws KeeperException, InterruptedException {
        List<String> workerAddresses = new ArrayList<>();
        List<String> workers = zooKeeper.getChildren("/search-system/workers", false);

        for (String worker : workers) {
            byte[] data = zooKeeper.getData("/search-system/workers/" + worker, false, null);
            workerAddresses.add(new String(data));
        }
        logger.info("Fetched Registered Workers: {}", workerAddresses);
        return workerAddresses;
    }

    // Calculate global IDF from aggregated document frequencies (DF)
    public Map<String, Double> calculateGlobalIDF(List<Map<String, Long>> localDFs, long totalDocuments) {
        Map<String, Long> globalDF = new HashMap<>();

        // Aggregate local DF values from all workers
        for (Map<String, Long> localDF : localDFs) {
            for (Map.Entry<String, Long> entry : localDF.entrySet()) {
                globalDF.put(entry.getKey(), globalDF.getOrDefault(entry.getKey(), 0L) + entry.getValue());
            }
        }

        // Calculate global IDF
        Map<String, Double> globalIDF = new HashMap<>();
        for (Map.Entry<String, Long> entry : globalDF.entrySet()) {
            double idf = Math.log((double) totalDocuments / entry.getValue());
            globalIDF.put(entry.getKey(), idf);
        }
        logger.info("Calculated Global IDF: {}", globalIDF);
        return globalIDF;
    }

    // Broadcast global IDF values to all workers
    public void broadcastGlobalIDF(Map<String, Double> globalIDF) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            List<String> workers = getRegisteredWorkers();
            for (String worker : workers) {
                String workerUrl = worker + "/api/worker/update-idf";
                restTemplate.postForObject(workerUrl, globalIDF, Void.class);
            }
            logger.info("Broadcasted Global IDF to all workers.");
        } catch (Exception e) {
            logger.error("Failed to broadcast global IDF: {}", e.getMessage(), e);
        }
    }

    // Update global IDF values (used by workers)
    public void updateGlobalIDF(Map<String, Double> globalIDF) {
        this.globalIDF = globalIDF;
        logger.info("Updated Global IDF: {}", globalIDF);
    }

    // Calculate and broadcast global IDF
    public void calculateAndBroadcastGlobalIDF(List<Map<String, Long>> localDFs, long totalDocuments) {
        Map<String, Double> globalIDF = calculateGlobalIDF(localDFs, totalDocuments);
        broadcastGlobalIDF(globalIDF);
    }
}