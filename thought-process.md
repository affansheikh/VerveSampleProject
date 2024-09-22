# Architectural Decisions and Thought Process

## Overview

This project implements a scalable and resilient **RESTful service** with one endpoint (`/api/verve/accept`) that handles incoming requests. The application is designed to process **10,000 requests per second** while maintaining **unique ID deduplication** across multiple instances and behind a **load balancer**. It also publishes the count of unique requests received every minute to **Kafka**. Additionally, it leverages **Redis** for cross-instance deduplication of requests.

The **GET** endpoint:
- Accepts an integer `id` as a query parameter.
- Optionally accepts an `endpoint` string.
- Uses **Redis** and **ConcurrentHashMap** to deduplicate the request based on the `id`.
- Returns:
    - `"ok"` if the request is successfully processed.
    - `"failed"` if the request is a duplicate or an error occurs.

In case the optional endpoint is provided, a **POST** request is made to that endpoint with unique requests count and ids as request body.


## Key Design Decisions

### 1. High Throughput and Concurrency
To ensure high throughput and concurrency, the application uses:
- **Spring Boot**: A well-established framework to handle concurrent web requests efficiently.
- **ConcurrentHashMap**: A thread-safe data structure to store unique request IDs in memory for short-lived data (1 minute).
  - This map data structure would solve the problem if there was only one instance of the app.

### 2. Cross-Instance Deduplication Using Redis
When the service is behind a load balancer and requests are distributed across multiple instances, **Redis** is used for deduplication:
- **RedisTemplate**: Used to store request IDs with a **Time to Live (TTL)** of 1 minute, ensuring unique ID tracking across instances.
- **Key-Value Store**: Each request ID is stored as a key in Redis with a TTL. If the key exists, the request is considered a duplicate; otherwise, it's processed and stored.

The problem with In-memory data structures like ConcurrentHashMap was that it will only work within a single instance. When there are multiple instances, each one will have its own in-memory state, so deduplication won't be effective across instances. 
Hence, a distributed cache across the instances was used. Another solution would have been to persist the ids in a Database with unique constraints but that would have been too 
complicated for the scope of this task.

### 3. Distributed Streaming Service: Apache Kafka
Kafka was used as the streaming service. Kafka is widely used for high-throughput, distributed messaging and is a great fit for this scenario.
- **KafkaMessagePublisher**: The publisher periodically sends the count of unique requests every minute to a Kafka topic named **unique-requests-count**.


### 4. Scheduling and Unique Count Logging
The service sends the count of unique requests received every minute. This task is implemented using:
- **Spring Scheduling (`@Scheduled`)**: To trigger the task every 60 seconds.
- The unique count is:
    - **Sent to Kafka**: A message is sent to a Kafka topic for further processing.
    - **Sent to Log file**: A message is sent to a local log file _**app.log**_ in resources directory.


### 5. Configuration
- **Externalized Configurations**: All environment-specific configurations (such as Kafka broker, Redis settings) are stored in `application.properties`, making the application highly configurable and portable.

---

*Note:* To check the state of the codebase after each iteration/extension, please refer to the respective git commits.
