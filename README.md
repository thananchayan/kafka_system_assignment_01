# Kafka Order Processing Assignment

This project implements a Kafka-based **Order Processing System** using **Spring Boot**, **Apache Kafka**, **Avro**, and **Confluent Schema Registry**.  
It demonstrates:

- Avro-based message serialization and schema management
- A REST API for publishing single and batch orders
- Real-time price analytics (running average, min, max, total)
- Retry mechanism for failed order processing
- Dead Letter Queue (DLQ) handling and monitoring
- Basic health and statistics endpoints for assignment reporting

---

## 1. Architecture Overview

The system is built around a simple microservice-style architecture:

- **Spring Boot Application**
    - Exposes REST endpoints to send orders and query analytics
    - Publishes orders to Kafka using Avro serialization
    - Consumes orders from Kafka, validates and processes them
    - Computes price analytics (running average, min, max, total)
    - Retries failed orders and finally sends them to a DLQ

- **Kafka Infrastructure (via Docker Compose)**
    - **Zookeeper** – coordinates Kafka broker
    - **Kafka Broker** – hosts topics for orders, retry, and DLQ
    - **Schema Registry** – manages Avro schemas for `Order` messages

---

## 2. Technologies Used

- **Java** 17+
- **Spring Boot** 3.x
- **Apache Kafka**
- **Confluent Schema Registry**
- **Avro** (SpecificRecord)
- **Docker & Docker Compose**
- **Lombok** for boilerplate reduction
- **springdoc-openapi** for Swagger UI (optional but recommended)

---
