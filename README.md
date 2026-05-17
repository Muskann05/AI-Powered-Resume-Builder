# ResumeAI - AI Powered Resume Builder Backend

ResumeAI is a full-stack AI powered resume builder platform. This repository contains the Spring Boot microservice backend used by the Angular frontend. The project helps job seekers create professional resumes, manage resume sections, choose templates, improve content using AI, check ATS/job fit, export resumes, and receive notifications.

## Project Overview

The backend is built using a microservice architecture. Each major business feature is separated into an independent Spring Boot service. API Gateway acts as the single entry point for the frontend, while Eureka supports service discovery for local microservice communication.

Main user flow:

1. User registers or logs in.
2. User selects a resume template.
3. User creates a resume.
4. User adds and manages resume sections.
5. User improves content using AI.
6. User checks job match / ATS-related feedback.
7. User exports the resume.
8. User receives notifications.

## Key Features

- User registration and login
- JWT based authentication
- Google OAuth login support
- Resume creation and management
- Dynamic resume sections
- Resume template selection
- AI assisted content improvement
- Job match analysis
- Resume export support
- Notification handling
- Payment/subscription support
- API Gateway routing
- Eureka service discovery
- Swagger/OpenAPI documentation
- Actuator health checks
- SonarCloud code quality scan
- Docker and Render deployment support

## Architecture

```text
Angular Frontend
      |
      v
API Gateway
      |
      +--> Auth Service
      +--> Resume Service
      +--> Section Service
      +--> Template Service
      +--> AI Service
      +--> Export Service
      +--> Job Match Service
      +--> Notification Service
      |
      v
Aiven MySQL / Redis / RabbitMQ / External APIs
```

Detailed UML and workflow diagrams are available here:

- [ResumeAI UML Diagrams](docs/RESUMEAI_UML_DIAGRAMS.md)

## Microservices

| Service | Port | Responsibility |
| --- | ---: | --- |
| `eureka-server` | `8761` | Service registry and discovery |
| `api-gateway` | `8080` | Single backend entry point and route forwarding |
| `auth-service` | `8081` | Authentication, JWT, OAuth, users, payments |
| `resume-service` | `8082` | Resume metadata, resume lifecycle, gallery APIs |
| `section-service` | `8083` | Resume sections such as summary, skills, education, experience |
| `ai-service` | `8084` | AI content improvement and AI request history |
| `template-service` | `8085` | Resume templates, categories, template seed data |
| `export-service` | `8086` | Resume export jobs and downloadable formats |
| `notification-service` | `8088` | User notifications and async events |
| `jobmatch-service` | `8089` | Job matching, score, missing skills, recommendations |

## Tech Stack

| Area | Technology |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot 3 |
| Microservices | Spring Cloud, Eureka, OpenFeign |
| Gateway | Spring Cloud Gateway |
| Database | MySQL / Aiven MySQL |
| ORM | Spring Data JPA, Hibernate |
| Security | Spring Security, JWT, OAuth2 |
| Messaging | RabbitMQ |
| Cache / Token support | Redis |
| API Docs | Swagger / SpringDoc OpenAPI |
| Build Tool | Maven |
| Deployment | Docker, Render |
| Code Quality | SonarCloud |

## Repository Structure

```text
ResumeServices/
├── ai-service/
├── api-gateway/
├── auth-service/
├── eureka-server/
├── export-service/
├── jobmatch-service/
├── notification-service/
├── resume-service/
├── section-service/
├── template-service/
├── docs/
│   └── RESUMEAI_UML_DIAGRAMS.md
├── .github/
│   └── workflows/
└── sonar-project.properties
```

## API Gateway Routes

The frontend communicates with the backend through API Gateway.

| Public Path | Routed Service |
| --- | --- |
| `/api/auth/**` | `auth-service` |
| `/api/resumes/**` | `resume-service` |
| `/api/sections/**` | `section-service` |
| `/api/templates/**` | `template-service` |
| `/api/ai/**` | `ai-service` |
| `/api/exports/**` | `export-service` |
| `/api/notifications/**` | `notification-service` |
| `/api/job-matches/**` | `jobmatch-service` |

## Prerequisites

Install the following before running locally:

- Java 17
- Maven
- MySQL 8
- Redis
- RabbitMQ
- Git

## Local Setup

1. Clone the repository.

```bash
git clone https://github.com/Muskann05/AI-Powered-Resume-Builder.git
cd AI-Powered-Resume-Builder
```

2. Create databases in MySQL.

```sql
CREATE DATABASE resumeai_auth;
CREATE DATABASE resumeai_resume;
CREATE DATABASE resumeai_section;
CREATE DATABASE resumeai_template;
CREATE DATABASE resumeai_ai;
CREATE DATABASE resumeai_export;
CREATE DATABASE resumeai_jobmatch;
CREATE DATABASE resumeai_notification;
```

3. Update each service configuration.

Each service has its own `src/main/resources/application.properties`. For local development, configure:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/<database_name>
spring.datasource.username=root
spring.datasource.password=<your_password>
```

4. Start services in this order.

```bash
cd eureka-server
mvn spring-boot:run
```

Then start:

```bash
cd api-gateway
mvn spring-boot:run
```

Then start the required business services:

```bash
cd auth-service
mvn spring-boot:run
```

```bash
cd resume-service
mvn spring-boot:run
```

```bash
cd section-service
mvn spring-boot:run
```

```bash
cd template-service
mvn spring-boot:run
```

Run other services as needed:

```bash
cd ai-service
mvn spring-boot:run
```

```bash
cd export-service
mvn spring-boot:run
```

```bash
cd jobmatch-service
mvn spring-boot:run
```

```bash
cd notification-service
mvn spring-boot:run
```

## Important Environment Variables

Use environment variables in Render or production. Do not commit real secrets to GitHub.

```env
SPRING_DATASOURCE_URL=jdbc:mysql://<host>:<port>/<database>?ssl-mode=REQUIRED
SPRING_DATASOURCE_USERNAME=<database_user>
SPRING_DATASOURCE_PASSWORD=<database_password>

GOOGLE_CLIENT_ID=<google_oauth_client_id>
GOOGLE_CLIENT_SECRET=<google_oauth_client_secret>
LINKEDIN_CLIENT_ID=<linkedin_client_id_or_dummy_if_not_used>
LINKEDIN_CLIENT_SECRET=<linkedin_client_secret_or_dummy_if_not_used>

APP_JWT_SECRET=<strong_jwt_secret>
APP_OAUTH2_REDIRECT_URL=<frontend_oauth_success_url>
APP_FRONTEND_RESET_PASSWORD_URL=<frontend_reset_password_url>

SPRING_DATA_REDIS_HOST=<redis_host>
SPRING_DATA_REDIS_PORT=<redis_port>

SPRING_RABBITMQ_HOST=<rabbitmq_host>
SPRING_RABBITMQ_PORT=<rabbitmq_port>

GEMINI_API_KEY=<gemini_api_key>
RAZORPAY_KEY_ID=<razorpay_key_id>
RAZORPAY_KEY_SECRET=<razorpay_key_secret>
```

## Build

Build an individual service:

```bash
cd auth-service
mvn clean package
```

Skip tests during deployment build:

```bash
mvn clean package -DskipTests
```

## Swagger and Health Check

Each service exposes Swagger and actuator endpoints when enabled:

```text
http://localhost:<port>/swagger-ui.html
http://localhost:<port>/actuator/health
```

Example:

```text
http://localhost:8081/swagger-ui.html
http://localhost:8081/actuator/health
```

## Deployment

The backend services can be deployed on Render as Docker-based Web Services.

Recommended production setup:

1. Create Aiven MySQL database.
2. Deploy required Spring Boot services on Render.
3. Add Render environment variables for database, OAuth, JWT, Redis, RabbitMQ, and external APIs.
4. Deploy Angular frontend as a static site.
5. Set frontend API base URL to the deployed API Gateway URL.
6. Configure CORS in API Gateway for the deployed frontend domain.

## Code Quality

SonarCloud is used for code quality analysis.

Current quality workflow:

- GitHub Actions builds backend services.
- SonarCloud scans Java source code.
- Dashboard reports reliability, maintainability, security, duplication, and coverage.

Coverage requires test coverage reports. If tests are skipped, SonarCloud can still show code quality, but coverage may appear as `0.0%` or not computed.

## Documentation

| Document | Purpose |
| --- | --- |
| `docs/RESUMEAI_UML_DIAGRAMS.md` | UML diagrams, workflow, deployment view |
| `sonar-project.properties` | SonarCloud project configuration |
| `.github/workflows/sonarcloud-backend.yml` | Backend SonarCloud GitHub Actions workflow |

## Project Explanation for Evaluation

ResumeAI solves the problem of manual resume creation by providing a complete resume-building workflow. A user can register, select a template, create a resume, add sections, improve content using AI, check job fit, and export the final resume. The backend follows a microservice architecture, where each feature is developed as a separate service. This makes the system modular, maintainable, and easier to deploy independently.

## Author

Developed by Muskan Gupta.

