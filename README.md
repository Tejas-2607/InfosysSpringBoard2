# Test Framework API

![Java](https://img.shields.io/badge/Java-21-orange.svg) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-green.svg) ![React](https://img.shields.io/badge/React-19-blue.svg) ![Maven](https://img.shields.io/badge/Maven-3.9.11-blue.svg) ![TestNG](https://img.shields.io/badge/TestNG-7.10.2-yellow.svg)

A comprehensive automated regression test suite framework built with Spring Boot backend and React frontend. This project provides APIs for managing test suites, executing tests (UI/API), generating analytics, and producing reports. It supports parallel execution, data-driven testing, and integration with tools like Selenium, RestAssured, RabbitMQ, and  reports.

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Setup and Installation](#setup-and-installation)
- [Running the Application](#running-the-application)
- [Testing](#testing)
- [CI/CD](#cicd)
- [Contributing](#contributing)
- [License](#license)

## Features

- **Test Suite Management**: Create, import (CSV), and manage test suites with UI/API test cases.
- **Parallel Execution**: Run tests concurrently using thread pools (UI: max 4 threads, API: max 8 threads).
- **Analytics Dashboard**: View pass rates, trends, flaky tests, and performance metrics (React UI).
- **Reporting**: Generate HTML/CSV reports integration.
- **Authentication**: JWT-based security with role-based access (Admin/User).
- **Data-Driven Testing**: Support for CSV, Excel, JSON, and API data providers.
- **Queueing**: RabbitMQ for asynchronous test execution.
- **Edge Case Handling**: Retries, flaky detection, and stability metrics.
- **Frontend UI**: React-based dashboard for login, suites, runs, analytics, and settings.

## Tech Stack

### Backend
- Java 21
- Spring Boot 3.5.6 (with JPA, Web, Security, AMQP, Retry)
- MySQL 8.0.33 (Database)
- RabbitMQ (Queueing)
- JWT (Authentication)
- Lombok (Boilerplate reduction)
- Maven (Build tool)
- Thymeleaf (Templating)
- Prometheus (Metrics)

### Frontend
- React 19
- Vite (Build tool)
- Tailwind CSS (Styling)
- Axios (API calls)
- Lucide React (Icons)

### Testing
- TestNG 7.10.2 (Test runner)
- Selenium 4.23.0 (UI automation)
- RestAssured 5.4.0 (API testing)
- WireMock (API mocking)
- Jacoco (Code coverage)
- JUnit 5 (Unit tests)

## Prerequisites

- Java 21 JDK
- Maven 3.9+
- Node.js 18+ (for frontend)
- MySQL 8+ (or use in-memory DB for dev)
- RabbitMQ (for queuing)
- Chrome browser (for Selenium tests)

## Setup and Installation

1. **Clone the Repository**:
   ```
   git clone https://github.com/your-repo/test-framework-api.git
   cd test-framework-api
   ```

2. **Backend Setup**:
   - Update `application.properties` with your MySQL/RabbitMQ credentials.
   - Build the project:
     ```
     ./mvnw clean install
     ```

3. **Frontend Setup**:
   - Navigate to frontend:
     ```
     cd Frontend
     npm install
     ```

4. **Database Setup**:
   - Create a MySQL database: `test_framework_db`.
   - Run the app to auto-create tables (via JPA).

## Running the Application

1. **Backend**:
   ```
   ./mvnw spring-boot:run
   ```
   - API available at `http://localhost:8080/api`.

2. **Frontend**:
   ```
   cd Frontend
   npm run dev
   ```
   - UI available at `http://localhost:5173`.

3. **Full Stack**:
   - Run backend first, then frontend.
   - Login with default credentials (or register via API).

## License

This project is not open-source licensed (no MIT/Apache/etc.) because it is an academic/portfolio submission.

You may study, reference, and learn from the code, but please do not claim it as your own work.

Authored & Developed by:

Tejas
