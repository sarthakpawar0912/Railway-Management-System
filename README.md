# Railway Management System

A production-grade **Railway Booking Platform** built with **Spring Boot Microservices Architecture**. This system demonstrates real-world distributed systems design with service discovery, API gateway routing, JWT authentication, asynchronous messaging, and circuit breaker resilience patterns.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Key Concepts](#key-concepts)
  - [Microservices Architecture](#microservices-architecture)
  - [Eureka Server (Service Discovery)](#eureka-server-service-discovery)
  - [API Gateway](#api-gateway)
  - [OpenFeign (Inter-Service Communication)](#openfeign-inter-service-communication)
  - [JWT Authentication](#jwt-authentication)
  - [RabbitMQ (Asynchronous Messaging)](#rabbitmq-asynchronous-messaging)
  - [Resilience4j (Circuit Breaker)](#resilience4j-circuit-breaker)
- [Services](#services)
  - [Eureka Server](#1-eureka-server)
  - [API Gateway](#2-api-gateway)
  - [User Service](#3-user-service)
  - [Train Service](#4-train-service)
  - [Booking Service](#5-booking-service)
  - [Payment Service](#6-payment-service)
- [Tech Stack](#tech-stack)
- [Database Schema](#database-schema)
- [API Reference](#api-reference)
- [Getting Started](#getting-started)
- [Service Communication Flow](#service-communication-flow)

---

## Architecture Overview

```
                                    +------------------+
                                    |  Eureka Server   |
                                    |    (Port 8761)   |
                                    +--------+---------+
                                             |
                              Service Registration & Discovery
                                             |
                    +------------------------+------------------------+
                    |                        |                        |
              +-----+------+         +------+-------+         +------+-------+
              | User       |         | Train        |         | Payment      |
              | Service    |         | Service      |         | Service      |
              | (Port 8081)|         | (Port 8084)  |         | (Port 8083)  |
              +-----+------+         +------+-------+         +------+-------+
                    |                        |                        |
                    +------------------------+------------------------+
                                             |
                                    +--------+---------+
                                    |   API Gateway    |
                                    |   (Port 8087)    |
                                    +--------+---------+
                                             |
                                    +--------+---------+
                                    | Booking Service  |
                                    |   (Port 8082)    |
                                    +------------------+
                                             |
                              +--------------+--------------+
                              |                             |
                        OpenFeign (Sync)            RabbitMQ (Async)
                     to Train & Payment           Booking & Payment
                         Services                    Event Queues
```

All client requests enter through the **API Gateway**, which discovers service instances via **Eureka Server** and routes traffic using load-balanced URIs. Services communicate with each other through **OpenFeign** for synchronous calls and **RabbitMQ** for asynchronous event-driven messaging.

---

## Key Concepts

### Microservices Architecture

Microservices is an architectural pattern where an application is structured as a collection of **small, autonomous services**, each running in its own process and communicating over lightweight protocols (typically HTTP/REST or messaging queues).

**Why Microservices?**

| Monolithic | Microservices |
|---|---|
| Single deployable unit | Independent deployment per service |
| Single point of failure | Fault isolation — one service failure doesn't crash the system |
| Scaling requires scaling everything | Scale individual services based on load |
| Tightly coupled codebase | Loose coupling, high cohesion |
| One tech stack for all | Polyglot — each service can use different tech |

In this project, the railway system is decomposed into **6 independent services**: Eureka Server, API Gateway, User Service, Train Service, Booking Service, and Payment Service. Each service has its own codebase, configuration, and can be deployed independently.

---

### Eureka Server (Service Discovery)

**Netflix Eureka** is a service registry that enables **dynamic service discovery** in a microservices ecosystem.

**The Problem:** In a distributed system, services need to find each other. Hardcoding IP addresses and ports is fragile — services may scale up/down, move across hosts, or restart on different ports.

**The Solution:** Eureka acts as a **phone book** for microservices:

```
1. Each service REGISTERS itself with Eureka on startup
   → "Hi, I'm USER-SERVICE running on localhost:8081"

2. When a service needs to call another, it QUERIES Eureka
   → "Where is TRAIN-SERVICE?" → "It's at localhost:8084"

3. Eureka sends periodic HEARTBEATS to check service health
   → If a service stops responding, Eureka removes it from the registry
```

**How it works in this project:**

```properties
# Eureka Server Configuration (Port 8761)
eureka.client.register-with-eureka=false    # Server doesn't register with itself
eureka.client.fetch-registry=false          # Server doesn't need to fetch its own registry

# Each microservice registers as a client
eureka.client.service-url.defaultZone=http://localhost:8761/eureka
```

Once running, the Eureka Dashboard is accessible at `http://localhost:8761` where you can see all registered service instances.

---

### API Gateway

**Spring Cloud Gateway** serves as the **single entry point** for all client requests. Instead of clients knowing the address of every microservice, they only need to know the gateway address.

**What it does:**

| Feature | Description |
|---|---|
| **Routing** | Maps URL paths to backend services (e.g., `/users/**` → User Service) |
| **Load Balancing** | Distributes requests across multiple instances of a service |
| **Single Entry Point** | Clients only need one URL — the gateway handles the rest |
| **Cross-Cutting Concerns** | Centralized logging, rate limiting, CORS, and security |

**Route Configuration in this project:**

```
Client Request                    Gateway Routes To
─────────────                    ─────────────────
GET  /users/profile        →     USER-SERVICE     (lb://USER-SERVICE)
POST /bookings/book        →     BOOKING-SERVICE  (lb://BOOKING-SERVICE)
POST /payments/pay         →     PAYMENT-SERVICE  (lb://PAYMENT-SERVICE)
GET  /trains/search        →     TRAIN-SERVICE    (lb://TRAIN-SERVICE)
```

The `lb://` prefix tells the gateway to use **load-balanced** routing through Eureka — it doesn't hardcode ports but resolves service locations dynamically.

---

### OpenFeign (Inter-Service Communication)

**OpenFeign** is a declarative HTTP client that makes calling other microservices as simple as calling a local method.

**Without Feign** (manual REST calls):
```java
RestTemplate restTemplate = new RestTemplate();
String url = "http://localhost:8084/trains/book-seat?trainNumber=101&seatType=SLEEPER";
Map response = restTemplate.postForObject(url, null, Map.class);
```

**With Feign** (declarative interface):
```java
@FeignClient(name = "TRAIN-SERVICE")
public interface TrainClient {
    @PostMapping("/trains/book-seat")
    Map<String, String> bookSeat(@RequestParam String trainNumber, @RequestParam String seatType);
}
```

Feign resolves `TRAIN-SERVICE` through Eureka, handles serialization/deserialization, and integrates with Resilience4j for fault tolerance — all without boilerplate code.

**JWT Token Forwarding:** The `FeignInterceptor` automatically forwards the `Authorization` header from incoming requests to all outgoing Feign calls, ensuring authenticated requests propagate across services.

---

### JWT Authentication

**JSON Web Token (JWT)** provides stateless, token-based authentication across the microservices ecosystem.

**Authentication Flow:**

```
┌──────────┐      1. POST /users/register       ┌──────────────┐
│          │ ──────────────────────────────────→  │              │
│          │      (name, email, password)         │              │
│          │                                      │              │
│          │      2. POST /users/login            │    User      │
│  Client  │ ──────────────────────────────────→  │   Service    │
│          │      (email, password)                │  (Port 8081) │
│          │                                      │              │
│          │  ←── 3. JWT Token + User Info         │              │
│          │      { token, type, email, role }     │              │
│          │                                      └──────────────┘
│          │
│          │      4. GET /bookings/all
│          │      Authorization: Bearer <JWT>
│          │ ──────────────────────────────────→  API Gateway → Booking Service
└──────────┘
```

**Security Implementation Details:**

| Component | Responsibility |
|---|---|
| `JwtUtil` | Generates HS256-signed tokens, extracts email claims, validates token expiry |
| `JwtFilter` | Intercepts every request, extracts Bearer token, sets Spring Security context |
| `SecurityConfig` | Permits `/users/login` & `/users/register`, secures all other endpoints |
| `CustomUserDetailsService` | Loads user from database for Spring Security authentication |
| `BCryptPasswordEncoder` | Hashes passwords before storage — never stores plaintext |

---

### RabbitMQ (Asynchronous Messaging)

**RabbitMQ** is a message broker that enables **asynchronous, event-driven communication** between services.

**Why async messaging?** Synchronous REST calls create tight coupling — if the payment service is slow or down, the booking service blocks. With RabbitMQ, the booking service publishes an event and continues, while the payment service processes it independently.

**Message Flow in this project:**

```
Booking Service                    RabbitMQ                     Payment Service
─────────────                    ──────────                   ───────────────
                                 ┌──────────┐
  BookingEvent ──── publish ───→ │ booking  │ ──── consume ──→ Process Payment
  (bookingId,                    │  Queue   │                  (amount > 0 = SUCCESS)
   userEmail,                    └──────────┘
   trainNumber)                  ┌──────────┐
                                 │ payment  │ ←── publish ──── PaymentEvent
  Update Booking ←── consume ─── │  Queue   │                  (bookingId, status)
  Status                         └──────────┘
```

**Two queues are used:**
- `bookingQueue` — Booking Service → Payment Service (booking events)
- `paymentQueue` — Payment Service → Booking Service (payment results)

---

### Resilience4j (Circuit Breaker)

**Resilience4j** implements the **Circuit Breaker pattern** to prevent cascading failures when a downstream service is unavailable.

**How a Circuit Breaker works:**

```
State: CLOSED (normal)              State: OPEN (tripped)           State: HALF_OPEN
─────────────────────              ──────────────────────          ─────────────────────
All requests pass through    →     Requests FAIL FAST             A few test requests
If failure rate > threshold  →     (don't call the broken         are allowed through
    switch to OPEN                  service — return fallback)     If they succeed →
                                   After wait duration →              switch to CLOSED
                                       switch to HALF_OPEN        If they fail →
                                                                      switch to OPEN
```

**Configuration in Booking Service:**

```properties
# Circuit breaker for payment service calls
resilience4j.circuitbreaker.instances.paymentService.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.paymentService.minimum-number-of-calls=5
resilience4j.circuitbreaker.instances.paymentService.wait-duration-in-open-state=10s

# Retry mechanism
resilience4j.retry.instances.paymentService.max-attempts=3
resilience4j.retry.instances.paymentService.wait-duration=2s
```

If the Payment Service fails in more than 50% of calls (out of minimum 5), the circuit **opens** — subsequent calls fail immediately without hitting the broken service. After 10 seconds, it enters half-open state to test if the service has recovered.

---

## Services

### 1. Eureka Server

| Property | Value |
|---|---|
| **Port** | `8761` |
| **Role** | Service Discovery & Registry |
| **Spring Boot** | 3.2.5 |
| **Spring Cloud** | 2023.0.1 |

The Eureka Server is the backbone of the microservices ecosystem. All services register here on startup and discover each other through this central registry. It provides a web dashboard at `http://localhost:8761` showing all registered service instances, their health status, and metadata.

**Key Configuration:**
- Self-preservation mode is **disabled** for development (services are deregistered immediately when they go down)
- Does not register with itself or fetch its own registry

---

### 2. API Gateway

| Property | Value |
|---|---|
| **Port** | `8087` |
| **Role** | Request Routing & Load Balancing |
| **Spring Boot** | 3.2.5 |
| **Spring Cloud** | 2023.0.1 |

The single entry point for all external client requests. Routes are defined to map URL patterns to backend services using Eureka-resolved, load-balanced URIs.

**Route Definitions:**

| Path Pattern | Target Service | Load Balanced URI |
|---|---|---|
| `/users/**` | USER-SERVICE | `lb://USER-SERVICE` |
| `/bookings/**` | BOOKING-SERVICE | `lb://BOOKING-SERVICE` |
| `/payments/**` | PAYMENT-SERVICE | `lb://PAYMENT-SERVICE` |
| `/trains/**` | TRAIN-SERVICE | `lb://TRAIN-SERVICE` |

**Features:**
- Dynamic service discovery through Eureka integration
- Auto-discovery of routes via `spring.cloud.gateway.discovery.locator.enabled=true`
- Lower-case service ID support for cleaner URLs
- DEBUG-level logging for gateway routing decisions

---

### 3. User Service

| Property | Value |
|---|---|
| **Port** | `8081` |
| **Role** | Authentication & User Management |
| **Spring Boot** | 4.0.5 |
| **Spring Cloud** | 2025.1.1 |
| **Security** | JWT + BCrypt |

Handles user registration, login, and JWT token generation. This is the **authentication backbone** of the entire system.

**Endpoints:**

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/users/register` | Public | Register a new user |
| `POST` | `/users/login` | Public | Login and receive JWT token |
| `GET` | `/users/profile` | Bearer Token | Access secured profile endpoint |

**Entity: User**

| Field | Type | Constraints |
|---|---|---|
| `id` | Long | Auto-generated |
| `name` | String | — |
| `email` | String | Unique |
| `password` | String | BCrypt hashed |
| `role` | Enum | `ADMIN` / `USER` |

**Security Architecture:**

```
Request → JwtFilter → Extract Bearer Token → Validate with JwtUtil
                                                    ↓
                                          Load user via CustomUserDetailsService
                                                    ↓
                                          Set SecurityContext → Controller
```

**Registration Request:**
```json
{
  "name": "Sarthak Pawar",
  "email": "sarthak@example.com",
  "password": "securePassword123",
  "role": "USER"
}
```

**Login Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "email": "sarthak@example.com",
  "role": "USER",
  "message": "Login successful"
}
```

---

### 4. Train Service

| Property | Value |
|---|---|
| **Port** | `8084` |
| **Role** | Train Management & Seat Booking |
| **Spring Boot** | 4.0.5 |
| **Spring Cloud** | 2025.1.1 |

Manages train data, searches, seat availability, and the booking/waiting-list logic.

**Endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/trains/add` | Add a new train |
| `GET` | `/trains/search?source=X&destination=Y` | Search trains by route |
| `POST` | `/trains/book-seat?trainNumber=X&seatType=Y` | Book a seat on a train |
| `GET` | `/trains/availability/{trainNumber}` | Check seat availability |
| `GET` | `/trains/all` | Get all trains |

**Entity: Train**

| Field | Type | Description |
|---|---|---|
| `trainNumber` | String | Unique train identifier |
| `trainName` | String | Name of the train |
| `source` / `destination` | String | Route endpoints |
| `departureTime` / `arrivalTime` | LocalTime | Schedule |
| `distance` | Double | Route distance in km |
| `firstAcSeats` | int | Available 1AC seats |
| `secondAcSeats` | int | Available 2AC seats |
| `thirdAcSeats` | int | Available 3AC seats |
| `sleeperSeats` | int | Available Sleeper seats |
| `generalSeats` | int | Available General seats |

**Seat Types:** `FIRST_AC`, `SECOND_AC`, `THIRD_AC`, `SLEEPER`, `GENERAL`

**Seat Booking Logic:**

```
Request to book SLEEPER on Train 12345
        ↓
Check sleeperSeats > 0?
    ├── YES → Decrement sleeperSeats
    │         Assign seat number (e.g., "S-5")
    │         Return status: "CONFIRMED"
    │
    └── NO  → Increment sleeperWaiting
              Assign waiting number (e.g., "WL-3")
              Return status: "WAITING"
```

**Coach Naming Convention:**

| Seat Type | Coach Prefix | Example |
|---|---|---|
| First AC | `A1` | `A1-10` |
| Second AC | `A2` | `A2-15` |
| Third AC | `B1` | `B1-20` |
| Sleeper | `S` | `S-5` |
| General | `G` | `G-30` |

**Search Feature:** Finds trains by source and destination, calculates journey duration (handles overnight trains), and returns results sorted by departure time.

---

### 5. Booking Service

| Property | Value |
|---|---|
| **Port** | `8082` |
| **Role** | Booking Orchestration |
| **Spring Boot** | 4.0.5 |
| **Spring Cloud** | 2025.1.1 |
| **Patterns** | Feign, Circuit Breaker, Message Queue |

The **orchestrator** of the booking workflow. Coordinates between Train Service (seat allocation) and Payment Service (payment processing) using both synchronous and asynchronous communication.

**Endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/bookings/book` | Create a new booking |
| `GET` | `/bookings/{id}` | Get booking by ID |
| `GET` | `/bookings/user?email=X` | Get all bookings for a user |
| `GET` | `/bookings/all` | Get all bookings |
| `DELETE` | `/bookings/cancel/{id}` | Cancel a booking |

**Entity: Booking**

| Field | Type | Description |
|---|---|---|
| `id` | Long | Auto-generated |
| `userEmail` | String | Email of the booking user |
| `trainNumber` | String | Train identifier |
| `source` / `destination` | String | Journey route |
| `seatNumber` | String | Assigned seat (e.g., `S-5`, `WL-3`) |
| `coach` | String | Coach assignment |
| `seatType` | String | Class of travel |
| `status` | Enum | `PENDING` / `CONFIRMED` / `FAILED` / `CANCELLED` / `WAITING` |

**Booking Request:**
```json
{
  "userEmail": "sarthak@example.com",
  "trainNumber": "12345",
  "source": "Mumbai",
  "destination": "Delhi",
  "seatType": "SLEEPER"
}
```

**Booking Flow:**

```
Client POST /bookings/book
        ↓
┌─── Booking Service ───┐
│                        │
│  1. Call TrainClient   │──→ Train Service: POST /trains/book-seat
│     (via OpenFeign)    │←── Returns: { seatNumber, coach, status, seatType }
│                        │
│  2. Check status       │
│     ├── WAITING?       │──→ Save booking with WAITING status → Return
│     │                  │
│     └── CONFIRMED?     │
│                        │
│  3. Call PaymentClient │──→ Payment Service: POST /payments/pay
│     (via OpenFeign)    │←── Returns: { paymentId, status, message }
│     [Circuit Breaker]  │
│                        │
│  4. Payment SUCCESS?   │
│     ├── YES → CONFIRMED│
│     └── NO  → FAILED   │
│                        │
└────────────────────────┘
```

**Feign Clients:**

| Client | Target Service | Endpoint |
|---|---|---|
| `TrainClient` | TRAIN-SERVICE | `POST /trains/book-seat` |
| `PaymentClient` | PAYMENT-SERVICE | `POST /payments/pay` |
| `UserClient` | USER-SERVICE | (placeholder for future use) |

**JWT Token Propagation:** The `FeignInterceptor` extracts the `Authorization` header from the incoming HTTP request and attaches it to all outgoing Feign calls, ensuring authentication context flows across service boundaries.

---

### 6. Payment Service

| Property | Value |
|---|---|
| **Port** | `8083` |
| **Role** | Payment Processing |
| **Spring Boot** | 4.0.5 |
| **Spring Cloud** | 2025.1.1 |
| **Messaging** | RabbitMQ |

Processes payments both synchronously (REST API) and asynchronously (RabbitMQ events).

**Endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/payments/pay` | Process a payment synchronously |

**Entity: Payment**

| Field | Type | Description |
|---|---|---|
| `id` | Long | Auto-generated |
| `bookingId` | Long | Associated booking |
| `userEmail` | String | Payer's email |
| `amount` | Double | Payment amount |
| `status` | Enum | `SUCCESS` / `FAILED` / `PENDING` |

**Payment Request:**
```json
{
  "bookingId": 1,
  "userEmail": "sarthak@example.com",
  "amount": 500.0
}
```

**Dual Processing Modes:**

| Mode | Trigger | Flow |
|---|---|---|
| **Synchronous** | REST `POST /payments/pay` | Direct request-response via Feign from Booking Service |
| **Asynchronous** | RabbitMQ `bookingQueue` listener | Consumes `BookingEvent`, processes payment, publishes `PaymentEvent` to `paymentQueue` |

**Payment Logic:**
- Amount > 0 → `SUCCESS`
- Amount <= 0 → `FAILED`

This simulates a payment gateway. In production, this would integrate with actual payment providers (Razorpay, Stripe, etc.).

---

## Tech Stack

| Technology | Purpose |
|---|---|
| **Java 21** | Programming language (LTS release) |
| **Spring Boot 4.0.5** | Application framework (services) |
| **Spring Boot 3.2.5** | Application framework (gateway & eureka) |
| **Spring Cloud 2025.1.1** | Microservices toolkit (services) |
| **Spring Cloud 2023.0.1** | Microservices toolkit (gateway & eureka) |
| **Netflix Eureka** | Service discovery and registry |
| **Spring Cloud Gateway** | API gateway and request routing |
| **OpenFeign** | Declarative HTTP client for inter-service calls |
| **Spring Security** | Authentication and authorization framework |
| **JWT (jjwt 0.11.5)** | Stateless token-based authentication |
| **Spring Data JPA** | ORM and database abstraction |
| **MySQL** | Relational database |
| **RabbitMQ** | Message broker for async communication |
| **Resilience4j** | Circuit breaker and retry patterns |
| **Lombok** | Boilerplate code reduction |
| **Maven** | Build and dependency management |

---

## Database Schema

All services share a single MySQL database: `railwaybooking`

```sql
-- User Service
CREATE TABLE user (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(255),
    email       VARCHAR(255) UNIQUE,
    password    VARCHAR(255),     -- BCrypt hashed
    role        VARCHAR(50)       -- ADMIN / USER
);

-- Train Service
CREATE TABLE train (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    train_number        VARCHAR(255),
    train_name          VARCHAR(255),
    source              VARCHAR(255),
    destination         VARCHAR(255),
    departure_time      TIME,
    arrival_time        TIME,
    distance            DOUBLE,
    first_ac_seats      INT,
    second_ac_seats     INT,
    third_ac_seats      INT,
    sleeper_seats       INT,
    general_seats       INT,
    first_ac_waiting    INT,
    second_ac_waiting   INT,
    third_ac_waiting    INT,
    sleeper_waiting     INT,
    general_waiting     INT
);

-- Booking Service
CREATE TABLE booking (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_email      VARCHAR(255),
    train_number    VARCHAR(255),
    source          VARCHAR(255),
    destination     VARCHAR(255),
    seat_number     VARCHAR(255),
    coach           VARCHAR(255),
    seat_type       VARCHAR(50),
    status          VARCHAR(50)   -- PENDING / CONFIRMED / FAILED / CANCELLED / WAITING
);

-- Payment Service
CREATE TABLE payment (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id  BIGINT,
    user_email  VARCHAR(255),
    amount      DOUBLE,
    status      VARCHAR(50)       -- SUCCESS / FAILED / PENDING
);
```

---

## API Reference

### Base URL

All requests go through the API Gateway:

```
http://localhost:8087
```

### Authentication

```bash
# 1. Register
curl -X POST http://localhost:8087/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Sarthak Pawar",
    "email": "sarthak@example.com",
    "password": "password123",
    "role": "USER"
  }'

# 2. Login (save the token from response)
curl -X POST http://localhost:8087/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "sarthak@example.com",
    "password": "password123"
  }'
```

### Train Operations

```bash
# Add a train
curl -X POST http://localhost:8087/trains/add \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -d '{
    "trainNumber": "12345",
    "trainName": "Rajdhani Express",
    "source": "Mumbai",
    "destination": "Delhi",
    "departureTime": "16:00",
    "arrivalTime": "08:00",
    "distance": 1384,
    "firstAcSeats": 20,
    "secondAcSeats": 40,
    "thirdAcSeats": 60,
    "sleeperSeats": 100,
    "generalSeats": 200
  }'

# Search trains
curl http://localhost:8087/trains/search?source=Mumbai&destination=Delhi

# Check availability
curl http://localhost:8087/trains/availability/12345
```

### Booking Operations

```bash
# Book a ticket
curl -X POST http://localhost:8087/bookings/book \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -d '{
    "userEmail": "sarthak@example.com",
    "trainNumber": "12345",
    "source": "Mumbai",
    "destination": "Delhi",
    "seatType": "SLEEPER"
  }'

# Get user bookings
curl http://localhost:8087/bookings/user?email=sarthak@example.com \
  -H "Authorization: Bearer <JWT_TOKEN>"

# Cancel booking
curl -X DELETE http://localhost:8087/bookings/cancel/1 \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

---

## Getting Started

### Prerequisites

| Software | Version | Download |
|---|---|---|
| Java JDK | 21+ | [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) |
| Maven | 3.9+ | [Apache Maven](https://maven.apache.org/download.cgi) |
| MySQL | 8.0+ | [MySQL Community](https://dev.mysql.com/downloads/) |
| RabbitMQ | 3.12+ | [RabbitMQ](https://www.rabbitmq.com/download.html) |

### Step 1: Database Setup

```sql
CREATE DATABASE railwaybooking;
```

> Update `application.properties` in each service if your MySQL credentials differ from `root` / `1234567890`.

### Step 2: Start RabbitMQ

Ensure RabbitMQ is running on `localhost:5672` with default credentials (`guest` / `guest`).

### Step 3: Start Services (in order)

Services must be started in the correct order to ensure proper registration and discovery:

```bash
# Terminal 1 — Start Eureka Server FIRST
cd eureka-server
./mvnw spring-boot:run

# Terminal 2 — Start API Gateway
cd api-gateway
./mvnw spring-boot:run

# Terminal 3 — Start User Service
cd user-service
./mvnw spring-boot:run

# Terminal 4 — Start Train Service
cd train-service
./mvnw spring-boot:run

# Terminal 5 — Start Payment Service
cd payment-service
./mvnw spring-boot:run

# Terminal 6 — Start Booking Service (depends on Train & Payment)
cd booking-service
./mvnw spring-boot:run
```

### Step 4: Verify

1. Open Eureka Dashboard: `http://localhost:8761`
2. Verify all 5 services are registered (USER-SERVICE, TRAIN-SERVICE, BOOKING-SERVICE, PAYMENT-SERVICE, API-GATEWAY)
3. Test via Gateway: `http://localhost:8087/trains/all`

---

## Service Communication Flow

**Complete End-to-End Booking Flow:**

```
User                    API Gateway          User Service       Train Service      Booking Service     Payment Service      RabbitMQ
 │                         │                      │                   │                   │                   │                │
 │  1. POST /users/login   │                      │                   │                   │                   │                │
 │────────────────────────→│─────────────────────→│                   │                   │                   │                │
 │  ←── JWT Token ─────────│←─────────────────────│                   │                   │                   │                │
 │                         │                      │                   │                   │                   │                │
 │  2. POST /bookings/book │                      │                   │                   │                   │                │
 │  [Authorization: Bearer]│                      │                   │                   │                   │                │
 │────────────────────────→│─────────────────────────────────────────→│                   │                   │                │
 │                         │                      │                   │                   │                   │                │
 │                         │                      │    3. Feign Call  │                   │                   │                │
 │                         │                      │    book-seat      │                   │                   │                │
 │                         │                      │                   │←──────────────────│                   │                │
 │                         │                      │                   │──── seat info ───→│                   │                │
 │                         │                      │                   │                   │                   │                │
 │                         │                      │                   │    4. Feign Call  │                   │                │
 │                         │                      │                   │    process payment│                   │                │
 │                         │                      │                   │                   │──────────────────→│                │
 │                         │                      │                   │                   │←── payment result─│                │
 │                         │                      │                   │                   │                   │                │
 │  ←── Booking Response ──│←────────────────────────────────────────│                   │                   │                │
 │  { id, status, seat }   │                      │                   │                   │                   │                │
 │                         │                      │                   │                   │                   │                │
```

---

## Service Port Summary

| Service | Port | Description |
|---|---|---|
| Eureka Server | `8761` | Service Discovery Dashboard |
| API Gateway | `8087` | Single Entry Point for Clients |
| User Service | `8081` | Authentication & User Management |
| Booking Service | `8082` | Booking Orchestration |
| Payment Service | `8083` | Payment Processing |
| Train Service | `8084` | Train & Seat Management |
| MySQL | `3306` | Database |
| RabbitMQ | `5672` | Message Broker |

---

## Author

**Sarthak Pawar**

---

<p align="center">Built with Spring Boot Microservices</p>

