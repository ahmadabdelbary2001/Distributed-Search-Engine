package com.ds.search.election;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

public class LeaderElection {
    private static final Logger logger = LoggerFactory.getLogger(LeaderElection.class);
    private static final String ELECTION_ZNODE = "/election";
    private final ZooKeeper zooKeeper;
    private final OnElectionAction onElectionAction;
    private String currentZNodeName;

    public LeaderElection(ZooKeeper zooKeeper, OnElectionAction onElectionAction) {
        this.zooKeeper = zooKeeper;
        this.onElectionAction = onElectionAction;
    }

    public void volunteerForLeadership() throws KeeperException, InterruptedException {
        String zNodePrefix = ELECTION_ZNODE + "/c_";
        String zNodeFullPath = zooKeeper.create(zNodePrefix, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        currentZNodeName = zNodeFullPath.replace(ELECTION_ZNODE + "/", "");
        logger.info("ZNode name: {}", currentZNodeName);
    }

    public void electLeader() throws KeeperException, InterruptedException {
        while (true) {
            Stat predecessorStat = null;
            String predecessorZNodeName = "";

            // Get the list of children in the election znode
            var children = zooKeeper.getChildren(ELECTION_ZNODE, false);
            children.sort(String::compareTo); // Sort zNodes to find the smallest

            if (currentZNodeName.equals(children.getFirst())) {
                logger.info("I am the coordinator");
                onElectionAction.onElectedToBeCoordinator();
                return;
            } else {
                logger.info("I am not the coordinator. Watching my predecessor.");
                for (String child : children) {
                    if (child.equals(currentZNodeName)) {
                        break;
                    }
                    predecessorZNodeName = child;
                }

                final String watchNode = predecessorZNodeName;
                predecessorStat = zooKeeper.exists(ELECTION_ZNODE + "/" + watchNode, watchedEvent -> {
                    if (watchedEvent.getType() == Watcher.Event.EventType.NodeDeleted) {
                        try {
                            electLeader();
                        } catch (KeeperException | InterruptedException e) {
                            logger.error("Error during leader election: {}", e.getMessage(), e);
                        }
                    }
                });

                // If the current node is not the coordinator, register as a worker
                if (predecessorStat != null) {
                    onElectionAction.onWorker();
                }
            }

            if (predecessorStat == null) {
                electLeader();
            }

            CountDownLatch latch = new CountDownLatch(1);
            latch.await(); // Wait for changes
        }
    }
}