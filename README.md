# Kafka-Based Order Processing System
A Distributed Messaging Assignment using Spring Boot, Apache Kafka, Schema Registry, Avro, DLQ & Retry Mechanisms

---

## 📌 Introduction
This project implements a **Kafka-based Order Processing System** using **Spring Boot** and **Apache Kafka**.  
It demonstrates key distributed system concepts such as:

- Avro message serialization
- Kafka producers & consumers
- Retry topic mechanism
- Dead Letter Queue (DLQ) handling
- Real-time price analytics
- REST API for sending orders, fetching stats, and monitoring failures

The system is designed as a complete end-to-end pipeline suitable for academic assignments, practical labs, or real-world event-driven architectures.

---

## 🏗️ 1. Architecture Overview

### **System Components**
The system uses the following main components:

- **Publisher Service**  
  Sends orders to Kafka using Avro serialization.

- **Order Processing Service**  
  Consumes orders, validates them, performs retry logic, and sends failed ones to DLQ after max retries.

- **Retry Consumer**  
  Processes failed messages from retry topic and attempts recovery.

- **Dead Letter Queue Service**  
  Stores and logs unprocessed messages.

- **Price Analytics Service**  
  Computes running average, min, max, and total price of processed orders.

- **REST API Controller**  
  Exposes endpoints for sending orders, batch processing, viewing statistics, and system health.

### **Kafka Topics**
- `orders-topic` (Main topic)
- `orders-retry-topic` (Retry topic)
- `orders-dlq-topic` (Dead Letter Queue)

### **High-Level Flow**
```
REST API → Kafka Publisher → Orders Topic → Order Processing  
      → Retry Topic (on failure) → DLQ (after max retries)
```

### **Docker Architecture**
```
+---------------------+
|  Spring Boot App    |
|  Producer/Consumers |
+---------------------+
           |
           v
+---------------------+
| Apache Kafka Broker |
+---------------------+
           |
           v
+---------------------+
| Schema Registry     |
+---------------------+
```

Docker Compose is used to deploy **Kafka**, **Zookeeper**, and **Schema Registry** locally.

---

## 🛠️ 2. Technologies Used

### **Backend Technologies**
- **Java 21+**
- **Spring Boot 3**
- **Spring Kafka**
- **Kafka Avro Serializer / Deserializer**
- **Schema Registry (Confluent Platform)**

### **Messaging & Serialization**
- **Apache Kafka**
- **Zookeeper**
- **Apache Avro** (Schema-based serialization)

### **Containerization**
- **Docker**
- **Docker Compose**

### **Developer Tools**
- IntelliJ IDEA / VS Code
- Postman / Insomnia for API testing
- Maven Wrapper (`mvnw`)

---

## ⚙️ 3. Setup Instructions

### **Step 1 — Clone the Project**
```bash
git clone https://github.com/thananchayan/kafka_system_assignment_01.git
cd kafka_system_assignment_01
```

---

### **Step 2 — Start Kafka + Zookeeper + Schema Registry**
```bash
docker-compose up -d
```

Verify containers:
```bash
docker ps
```

You should see:
- zookeeper
- kafka
- schema-registry

---

### **Step 3 — Build and Run Spring Boot Application**
```bash
./mvnw clean package
./mvnw spring-boot:run
```

App will run at:
```
http://localhost:8080
```

---

### **Step 4 — Test REST Endpoints**

#### **Send Single Order**
```http
POST http://localhost:8080/api/v1/orders/send
```
Body:
```json
{
  "orderId": "O1001",
  "product": "Laptop",
  "price": 150000
}
```

#### **Send Batch Orders**
```
POST /api/v1/orders/send-batch
```

#### **View Price Analytics**
```
GET /api/v1/orders/stats
```

#### **Reset Analytics**
```
POST /api/v1/orders/stats/reset
```

#### **View DLQ Messages**
```
GET /api/v1/orders/failed
```

#### **Health Check**
```
GET /api/v1/orders/health
```

---

## 📄 Avro Schema Location
Avro schema is stored in:

```
src/main/avro/Order.avsc
```

Used for generating the `Order` model class.

---

## 👨‍💻 Developer Information
**Name:**   THANANCHAYAN  
**Reg No:** EG/2020/4227


---
