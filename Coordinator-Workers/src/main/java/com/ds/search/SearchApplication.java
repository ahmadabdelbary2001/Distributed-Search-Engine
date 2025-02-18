package com.ds.search;

import com.ds.search.election.LeaderElection;
import com.ds.search.election.OnElectionAction;
import com.ds.search.service.ZookeeperService;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SearchApplication implements CommandLineRunner {

	@Autowired
	private OnElectionAction onElectionAction;

	@Autowired
	private ZookeeperService zookeeperService;

	@Value("${election.port}")
	private int port;

	public static void main(String[] args) {
		SpringApplication.run(SearchApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		ZooKeeper zooKeeper = zookeeperService.getZooKeeper();
		LeaderElection leaderElection = new LeaderElection(zooKeeper, onElectionAction);
		leaderElection.volunteerForLeadership();
		leaderElection.electLeader();
	}
}