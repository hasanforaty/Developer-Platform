# Developer Platform - Internal Tools Hub

A centralized platform for managing, monitoring, and controlling microservices ecosystems.

## 🎯 Project Vision

The Developer Platform serves as a "mission control center" for development teams working with microservices. Instead of juggling multiple tools, developers have ONE unified interface to:

- Monitor the health of all services at a glance
- Search logs across all microservices
- Toggle feature flags without deployments
- Monitor API usage and rate limits
- Run A/B tests on features

## 🏗️ Architecture

This is a multi-module Spring Boot project built with Kotlin:

### Modules

- **gateway**: API Gateway handling authentication, routing, and rate limiting
- **service-registry**: Service discovery and health monitoring
- **log-aggregation**: Centralized log collection and search
- **feature-flag-service**: Feature flag management and evaluation
- **rate-limiter**: API rate limiting and usage analytics
- **ab-testing-service**: A/B testing framework
- **common-lib**: Shared models, utilities, and configurations

## 🚀 Tech Stack

- **Language**: Kotlin 1.9.20
- **Framework**: Spring Boot 3.2.0
- **JDK**: Java 17
- **Build Tool**: Gradle (Kotlin DSL)
- **Database**: PostgreSQL
- **Cache**: Redis
- **Tracing**: Zipkin (Distributed Tracing)

## 📋 Prerequisites

- JDK 17 or higher
- Docker & Docker Compose
- Gradle 8.x (or use wrapper)

## 🛠️ Getting Started

### 1. Clone the repository

```bash
git clone <repository-url>
cd Developer-Platform
```

### 2. Start infrastructure services

```bash
docker-compose up -d
```

### 3. Build the project

```bash
./gradlew build
```

### 4. Run services

```bash
# Run all services (from respective module directories)
./gradlew :service-registry:bootRun
./gradlew :gateway:bootRun
# ... other services
```

## 📚 Documentation

- [Architecture Documentation](docs/architecture/README.md)
- [API Documentation](docs/api/README.md)
- [Development Journal](Journal/README.md) - Detailed development log with decision rationale

## 🧪 Testing

```bash
# Run all tests
./gradlew test

# Run integration tests
./gradlew integrationTest

# Check test coverage
./gradlew jacocoTestReport
```

## 📦 Project Structure

```
developer-platform/
├── gateway/                  # API Gateway
├── service-registry/         # Service Discovery
├── log-aggregation/          # Log Management
├── feature-flag-service/     # Feature Flags
├── rate-limiter/             # Rate Limiting
├── ab-testing-service/       # A/B Testing
├── common-lib/               # Shared Libraries
├── Journal/                  # Development Journal
├── docs/                     # Documentation
├── docker-compose.yml        # Infrastructure setup
└── build.gradle.kts          # Root build configuration
```

## 🎯 Current Status

**Phase**: 0 - Foundation
**Version**: 0.1.0
**Status**: In Development

See [Journal](Journal/README.md) for detailed progress.

## 📝 License

[Your License]

## 👥 Contributors

[Your Name]
