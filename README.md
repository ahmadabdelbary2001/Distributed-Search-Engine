# Distributed-Search-Engine

**Distributed-Search-Engine** is a distributed search engine project that leverages Apache ZooKeeper for leader election and coordination between nodes. The project is split into two main modules:

- **Coordinator-Workers**:  
  A Java Spring Boot application that handles leader election using ZooKeeper, distributes search queries to worker nodes, aggregates results, and performs ranking using techniques like TF-IDF.

- **MvcWebServer**:  
  An ASP.NET Core MVC application that provides a user-friendly web interface for submitting search queries and displaying the aggregated results.

## Project Architecture Overview

- **Coordinator-Workers Module**:  
  Uses ZooKeeper for electing a coordinator among registered nodes. The coordinator receives search queries, forwards them to worker nodes, collects and ranks the responses, and then returns the final results.

- **MvcWebServer Module**:  
  Acts as the front-end interface. It retrieves the current coordinator address (updated via ZooKeeper events) and forwards user search queries to the coordinator. The search results are then displayed to the user.

## Prerequisites

- **For Coordinator-Workers**:
  - Java 21 and Maven.
  - A running Apache ZooKeeper instance (e.g., at `172.29.3.101:2181`).

- **For MvcWebServer**:
  - .NET 6 (or later).

## Quick Start

1. **Coordinator-Workers Module**:  
   Navigate to the `Coordinator-Workers` directory and follow its README for configuration and execution.

2. **MvcWebServer Module**:  
   Navigate to the `MvcWebServer` directory and follow its README to set up and run the web server.


## License 
[MIT License](LICENSE)
