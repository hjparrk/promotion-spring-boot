# 🚀 Promotion Service (Microservices Architecture)

## 📌 Project Overview

Promotion Service is a **coupon issuance service** designed with a **microservices architecture (MSA)** approach. This project was initiated to **gain hands-on experience** by directly applying key technologies and to build a **scalable and distributed system capable of handling high traffic loads**.

Users can request coupons, and the system ensures secure and efficient coupon issuance using **Kafka-based asynchronous event processing, Redis caching, and distributed locking mechanisms**.

Additionally, **unit testing was implemented using JUnit and Mockito**, and **performance testing was conducted using JMeter** for evaluating the coupon issuance API.

---

## 🛠 Tech Stack

- **Backend**: Java 17, Spring Boot, Spring Security, Spring Data JPA
- **Microservices**: Spring Cloud Gateway, Spring Cloud Eureka
- **Database**: PostgreSQL, H2 (for testing)
- **Message Queue**: Kafka
- **Cache & Concurrency**: Redis, Redisson (Distributed Lock)
- **Fault Tolerance**: Resilience4j (Circuit Breaker)
- **Testing**: JUnit, Mockito, JMeter
- **Build Tool**: Gradle
- **Containerization**: Docker, Docker Compose

---

## 📌 Key Features

### ✅ API Gateway

- **Spring Cloud Gateway** for **microservice routing**
- **Redis-based Rate Limiting** to control API requests
- **JWT-based authentication and validation**
- **Resilience4j Circuit Breaker** for fault tolerance

### ✅ Coupon Service

- **Redisson-based distributed locking** for concurrency control
- **Redis caching** for fast coupon policy application
- **Kafka-based asynchronous coupon issuance processing**

---

## 📁 Project Structure

```bash
├── api-gateway                # API Gateway (Routing & Authentication)
├── discovery-service          # Eureka Service Discovery
├── coupon-service             # Business logic for coupon processing
├── user-service               # User management service
├── infrastructure             # Shared infrastructure (Docker, Config)
└── README.md                  # Project documentation
```

---

## 🔧 Setup & Execution Guide

### 1️⃣ Start Essential Services (Docker Compose)

```sh
docker-compose up -d
```

- `Redis`, `Kafka`, and `PostgreSQL` will be automatically started.

### 2️⃣ Start API Gateway

```sh
cd api-gateway && ./gradlew bootRun
```

### 3️⃣ Start Microservices

```sh
cd ../coupon-service && ./gradlew bootRun
cd ../user-service && ./gradlew bootRun
```

---

## ⚙️ Configuration (Examples)

### 📁 Docker Compose (`docker-compose.yaml` Example)

```yaml
version: '3.8'
services:
  redis:
    image: redis:latest
    container_name: redis
    ports:
      - "6379:6379"
    networks:
      - promotion_network

  kafka:
    image: confluentinc/cp-kafka:7.5.1
    hostname: kafka
    container_name: kafka
    ports:
      - "9092:9092"
    networks:
      - promotion_network

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: kafka-ui
    ports:
      - "9090:9090"
    networks:
      - promotion_network
    depends_on:
      - kafka

networks:
  promotion_network:
    driver: bridge
```

### 📁 API Gateway (`application.yaml` Example)

```yaml
server:
  port: 8000

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://USER-SERVICE
        - id: coupon-service
          uri: lb://COUPON-SERVICE
  redis:
    host: redis
    port: 6379
  security:
    jwt:
      secret: my-secret-key
```

### 📁 Discovery Service (`application.yaml` Example)

```yaml
server:
  port: 8761

spring:
  application:
    name: discovery-service

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
  server:
    wait-time-in-ms-when-sync-empty: 0
```

### 📁 Coupon Service (`application.yaml` Example)

```yaml
server:
  port: 8082

spring:
  application:
    name: coupon-service
  datasource:
    url: jdbc:h2:mem:coupondb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  kafka:
    bootstrap-servers: kafka:9092
  cache:
    type: redis
    redis:
      host: redis
      port: 6379
```

### 📁 User Service (`application.yaml` Example)

```yaml
server:
  port: 8004

spring:
  application:
    name: user-service
  datasource:
    url: jdbc:h2:mem:userdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    show-sql: true
  security:
    jwt:
      secret: SpringBootPromotionServiceWithRedisAndKafka
```

---

## 📌 Future Improvements

- **Implement Kafka-based event sourcing**
- **Integrate monitoring tools (Prometheus, Grafana)**

📄 This README is auto-generated based on the project structure. Feel free to modify it as needed. ✍️
