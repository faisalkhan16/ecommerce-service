
# Ecommerce Service

**Backend Interview Assignment**

A Spring Boot backend service for managing **products**, **users**, and **orders** with **JWT authentication**, **role-based access control**, and **dynamic discount computation**.
The project is fully containerized using **Docker** and **Docker Compose** for easy setup and deployment.

---

## Table of Contents

* Overview
* Tech Stack
* Features (Mapped to Requirements)

  * Product Management
  * User Management
  * Order Management
  * Discount Rules (Dynamic)
    * Design Pattern: Chain of Responsibility
* Getting Started

  * Prerequisites
  * Run Locally
  * Run with Docker Compose

    * Development Environment
    * Production Environment
    * Quick Reference Commands
* Docker Compose File
* Environment Variables
* API Documentation
* Postman Collection
* Authentication & Authorization
* Default Admin User
* Testing
* Actuator
* Notes / Assumptions


---

## Overview

This project demonstrates a production-style backend application built with **Spring Boot**.  
It focuses on clean architecture, secure authentication, extensible business rules, and real-world deployment practices using Docker.

---

## Tech Stack

- Java 17
- Spring Boot 3
- Spring Data JPA
- PostgreSQL
- Flyway
- Redis
- Docker & Docker Compose
- Swagger / OpenAPI
- JUnit 5, Mockito, Testcontainers

---

## Features (Mapped to Requirements)

### 1) Product Management
- Full **CRUD** for products:
    - `id`, `name`, `description`, `price`, `quantity`, timestamps, **soft delete**
- Search & filter:
    - by **name**
    - **price range**
    - **availability** (in-stock/out-of-stock)
- (Optional/Bonus) Pagination & sorting supported

### 2) User Management
- Roles:
    - `USER`, `PREMIUM_USER`, `ADMIN`
- **JWT-based authentication**
- Role-based access control:
    - `ADMIN`: full CRUD on products
    - `USER` / `PREMIUM_USER`: view products, place orders
- Logout support:
    - access token is revoked by **blacklisting token jti in Redis until expiration**

### 3) Order Management
- Place orders for **multiple products**
- Stock validation:
    - reject if any item has insufficient stock
- Inventory updates:
    - product quantity is reduced after successful order placement
- Order contains:
    - items: `productId`, `quantity`, `unitPrice`, `discountApplied`, `totalPrice`
    - `orderTotal`

### 4) Discount Rules (Dynamic)
- `USER`: no discount
- `PREMIUM_USER`: **10% off** total order
- Orders **> $500**: extra **5% off** for any user
- Discount computation is **dynamic** (design pattern-based; see “Design Decisions”)

#### Design Pattern: Chain of Responsibility

The discount module in this project utilizes the **Chain of Responsibility** pattern to process various discount rules in a clean and decoupled manner.

#### Components

| Component | Class/Interface | Role |
| :--- | :--- | :--- |
| **Handler** | `DiscountHandler` | Defines the interface for handling discount logic. |
| **Concrete Handlers** | `PremiumUserDiscountHandler`, `HighValueOrderDiscountHandler` | Implement specific discount logic. |
| **Context** | `DiscountContext` | Holds the state and accumulated results. |
| **Client** | `DiscountService` | Executes the chain of handlers. |

#### How it works
1. `DiscountService` receives all available `DiscountHandler` implementations via Spring's dependency injection (ordered by `@Order`).
2. The `DiscountContext` is passed through each handler in the list.
3. Each handler applies its logic, updates the `discountTotal` in the context.
4. If a handler returns `STOP`, the chain terminates; otherwise, it continues to the next handler.

---

## Getting Started

### Prerequisites
- Java 17
- Maven or mvnw
- Docker & Docker Compose

---

## Run Locally

```bash
./mvnw clean test
./mvnw spring-boot:run
```

App runs at: http://localhost:8080

---

## Docker Compose - Run Instructions

This guide shows how to run the `ecommerce-service` application using Docker Compose for **development** and **production** environments.

---

### Development Environment

In development, you can **reset the database safely**. This will delete the dev PostgreSQL volume and rebuild everything from scratch.

#### Use `.env.dev` file

Make sure `.env.dev` exists in your project root with dev settings.

```bash
# Stop and remove containers + dev DB volume
docker compose --env-file .env.dev down -v

# Start containers and rebuild images
docker compose --env-file .env.dev up -d --build
```

* `down -v` → deletes **dev DB** (`postgres-data-dev`)
* `up -d --build` → rebuilds images and runs Flyway migrations
* Logs are saved to `./logs` folder

---

### Production Environment

In production, the database must be **preserved**. Do **not** delete the production volume.

#### Use `.env.prod` file

Make sure `.env.prod` exists in your project root with prod settings.

```bash
# Stop and remove containers (DB volume is preserved)
docker compose --env-file .env.prod down

# Start containers and rebuild images
docker compose --env-file .env.prod up -d --build
```

* DB volume (`postgres-data-prod`) is preserved
* Flyway validates migrations on startup
* Logs are saved to `./logs` folder

---

### Notes

* `.env.dev` and `.env.prod` contain environment-specific variables (DB name, volume, ports, JWT secret, etc.)
* Always **backup production database** before performing schema changes
* Use **dev profile** for testing and resets; **prod profile** for live deployment
* Docker volumes:

  * Dev → `postgres-data-dev` (can be deleted)
  * Prod → `postgres-data-prod` (never delete blindly)

---

### Quick Reference Commands

| Environment | Down Command                                 | Up Command                                          |
| ----------- | -------------------------------------------- | --------------------------------------------------- |
| Dev         | `docker compose --env-file .env.dev down -v` | `docker compose --env-file .env.dev up -d --build`  |
| Prod        | `docker compose --env-file .env.prod down`   | `docker compose --env-file .env.prod up -d --build` |

---

### Docker Compose File

```yaml
services:
  # ---------- PostgreSQL ----------
  postgres:
    image: postgres:16
    container_name: postgres
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    ports:
      - "${POSTGRES_PORT:-5432}:5432"
    volumes:
      - ${POSTGRES_VOLUME}:/var/lib/postgresql/data

  # ---------- Redis ----------
  redis:
    image: redis:7-alpine
    container_name: redis
    ports:
      - "${REDIS_PORT:-6379}:6379"

  # ---------- Application ----------
  ecommerce-service:
    image: faisalkhan16/ecommerce-service:latest
    container_name: ecommerce-service
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE}
      SERVER_PORT: ${SERVER_PORT}
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: ${REDIS_PORT}
      JWT_SECRET: ${JWT_SECRET}
      JWT_EXPIRATION_MINUTES: ${JWT_EXPIRATION_MINUTES}
    ports:
      - "${SERVER_PORT}:${SERVER_PORT}"
    volumes:
      - ./logs:/ecommerce-service/logs
    depends_on:
      - postgres
      - redis

# ---------- Volumes ----------
volumes:
  postgres-data-dev: {}
  postgres-data-prod: {}

```

---

### Environment Variables

| Variable                   | Description                         | Example                                      |
|----------------------------|-------------------------------------|----------------------------------------------|
| SPRING_PROFILES_ACTIVE      | Active Spring profile               | prod                                         |
| SERVER_PORT                 | Application server port             | 8080                                         |
| POSTGRES_DB                 | PostgreSQL database name            | ecommerce_prod                               |
| POSTGRES_USER               | PostgreSQL username                 | postgres                                     |
| POSTGRES_PASSWORD           | PostgreSQL password                 | postgres                                     |
| POSTGRES_VOLUME             | Docker volume name for PostgreSQL   | postgres-data-prod                           |
| REDIS_PORT                  | Redis server port                   | 6379                                         |
| SPRING_REDIS_HOST           | Redis server host                   | redis                                        |
| JWT_SECRET                  | JWT signing secret                  | pyj2QjD4mb7Rm7i4jo2Dx8XdTgmV5fIQ4JRORssXthc= |
| JWT_EXPIRATION_MINUTES      | JWT token validity in minutes       | 60                                           |

---

## API Documentation

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI: http://localhost:8080/v3/api-docs

## Postman Collection

Postman assets are available under the `postman/` folder:

- **Collection**: `postman/collections/ecommerce-api.postman_collection.json`  
  Contains all prepared API requests (auth, products, users, orders) with common headers and variables.

- **Environment**: `postman/environments/ecommerce-app.postman_environment.json`  
  Defines environment variables used by the collection (most importantly `base-url`, plus token variable like `auth-token` depending on your Postman setup).

### How to import
1. Open Postman
2. Click **Import**
3. Import both files:
    - `postman/collections/ecommerce-api.postman_collection.json`
    - `postman/environments/ecommerce-app.postman_environment.json`
4. In the top-right environment dropdown, select: **ecommerce-app**

### Set `base-url`
In Postman, go to **Environments** → **ecommerce-app** and set:

- `base-url` = `http://localhost:8080` (running locally with Maven)
- `base-url` = `http://localhost:8080` (running via Docker Compose)

Click **Save**, then run requests from the collection.

> Tip: If the collection uses `{{base-url}}/...` in request URLs, updating this single variable is enough to switch between local and Docker deployments.

---

## Authentication & Authorization

### Roles
- `ADMIN`: product management + access to protected operations
- `USER`: browse products, place orders
- `PREMIUM_USER`: browse products, place orders with premium discount

### Login
1) Authenticate via the login endpoint.
2) Use returned token in subsequent calls:

Header:
Authorization: Bearer <JWT_TOKEN_PLACEHOLDER>


### Logout
Logout revokes the presented token by blacklisting its `jti` in Redis until it expires.

---

## Default Admin User (Created on Startup)

On application startup (via database migration/seed), the system creates a default **ADMIN** user for initial login.

**Credentials**
- **Email:** `admin@example.com`
- **Password:** `abcd1234`

> After logging in, you can create/update other users (ADMIN-only) and proceed with product/order operations.
---

## Testing

Run unit + integration tests:
bash ./mvnw test


What’s covered:
- discount calculations
- order placement & stock validation
- security (JWT/role guards) where applicable
- repository/service behavior (integration tests)

---

## Actuator

If enabled:

- `http://localhost:8080/actuator`

Useful endpoints:
- health checks
- metrics
- environment info (depending on exposure configuration)

---

## Notes / Assumptions
- Product deletions are **soft deletes** (records remain but are excluded from normal queries)
- Monetary values use `BigDecimal` to avoid floating-point issues
- Swagger is the authoritative reference for endpoint shapes and payloads


---