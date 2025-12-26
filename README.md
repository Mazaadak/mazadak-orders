# Orders Service
## Overview
- The Orders Service is responsible for managing order creation, lifecycle management, payment coordination, and order fulfillment workflows.

- It exposes a REST API, persists order state, coordinates saga workflows for distributed transactions, and emits domain events to other services in the Mazadak platform.

- The Orders Service is the owner of order state within the platform and orchestrates checkout workflows across multiple services.

## API Endpoints
- See [Orders Service Wiki Page](https://github.com/Mazaadak/.github/wiki/Order-Service) for a detailed breakdown of the service's API endpoints
- Swagger UI available at `http://localhost:18088/swagger-ui/index.html` when running locally

## How to Run
You can run it via [Docker Compose](https://github.com/Mazaadak/mazadak-infrastructure) or [Kubernetes](https://github.com/Mazaadak/mazadak-k8s/)

## Tech Stack
- **Spring Boot 3.5.6** (Java 21) 
- **PostgreSQL**
- **Apache Kafka**
- **Netflix Eureka** - Service Discovery
- **Docker & Kubernetes** - Deployment & Containerization
- **Micrometer, OpenTelemetry, Alloy, Loki, Prometheus, Tempo, Grafana** - Observability
- **OpenAPI/Swagger** - API Documentation
- **Temporal** - Saga Workflow Management

## For Further Information
Refer to [Orders Service Wiki Page](https://github.com/Mazaadak/.github/wiki/Order-Service).

