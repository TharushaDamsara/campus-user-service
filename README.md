# campus-user-service

User management and authentication service for CampusFlow. Handles registration, login,
JWT issuance, and user profile data.

This repository is a **Git submodule** of [campus-backend-services](https://github.com/TharushaDamsara/campus-backend-services).

**Student:** Tharusha Damsara (241711004)
**GCP Project ID:** campusflow-eca-2026

## Technology Stack

- Java 25
- Spring Boot 3.5.3 (Web, Data JPA, Security, Validation, Actuator)
- PostgreSQL (relational database)
- Spring Cloud Netflix Eureka Client
- Spring Cloud Config Client
- JJWT (JWT issuing) + BCrypt
- Google Cloud Firestore (audit events)
- springdoc-openapi (API docs)

## Setup / Getting Started

```bash
mvn clean install
mvn spring-boot:run
```

Runs on port `8081` by default. Requires PostgreSQL, and the CampusFlow Config Server +
Eureka Server to be running.
# campus-user-service
