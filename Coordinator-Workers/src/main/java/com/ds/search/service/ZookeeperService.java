package com.ds.search.service;

import lombok.Getter;
import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Getter
@Service
public class ZookeeperService {
    private static final Logger logger = LoggerFactory.getLogger(ZookeeperService.class);
    private final ZooKeeper zooKeeper;

    public ZookeeperService(@Value("${zookeeper.connection}") String zookeeperConnection) throws IOException {
        if (zookeeperConnection == null || zookeeperConnection.isEmpty()) {
            throw new IllegalArgumentException("Zookeeper connection string is missing or invalid.");
        }
        this.zooKeeper = new ZooKeeper(zookeeperConnection, 3000, event -> {
            logger.info("Zookeeper event triggered: {}", event);
        });
        logger.info("Connected to Zookeeper at: {}", zookeeperConnection);
    }

    public void createNodeIfNotExists(String path) throws KeeperException, InterruptedException {
        if (zooKeeper.exists(path, false) == null) {
            zooKeeper.create(path, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            logger.info("Created Zookeeper node: {}", path);
        } else {
            logger.info("Zookeeper node already exists: {}", path);
        }
    }
}