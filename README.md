# MonitorX

MonitorX is a small transaction and fraud monitoring application built from
the three supplied projects. Its folders follow the original MonitorX layout,
its customer and fraud rules are based on FraudDetectionApp, and its compact
REST API and browser dashboard follow the simplified Argus approach.

## Features

- Customer and transaction REST endpoints
- Fraud checks for high amount, unusual hour, and country mismatch
- Risk scores and low, medium, or high alerts
- Alert review, resolution, and dismissal flow
- Responsive vanilla HTML, CSS, and JavaScript dashboard
- In-memory storage, so no database setup is needed

## Quick start with the included JAR

Install Java 17 or newer, then run:

```bash
java -jar target/monitorx-1.0.0.jar
```

Open <http://localhost:8080> and select **Load sample data**.

API documentation is available at
<http://localhost:8080/swagger-ui/index.html>. The generated OpenAPI document
is available at <http://localhost:8080/v3/api-docs>.

## Build and run from source

Install Java 17+ and Maven 3.9+, then run:

```bash
mvn clean package
java -jar target/monitorx-1.0.0.jar
```

## Run with Docker

Install Docker Desktop or Docker Engine with Compose, then run:

```bash
docker compose up --build
```

Open <http://localhost:8080>. Stop with `Ctrl+C`, followed by:

```bash
docker compose down
```

Activity resets whenever the application restarts because this simplified
edition intentionally uses memory storage.

## Project layout

```text
src/main/java/com/MonitorX/
|-- Controllers/       REST endpoints
|-- models/            Customer, transaction, alert, and summary objects
|-- Repository/        In-memory data storage
|-- Services/          Fraud rules and business logic
`-- MonitorXApplication.java

src/main/resources/
|-- static/
|   |-- index.html
|   |-- styles.css
|   `-- app.js
`-- application.properties
```

## API

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/summary` | Dashboard totals |
| `GET` | `/api/customers` | Available demo customers |
| `GET` | `/api/transactions` | All transactions |
| `POST` | `/api/transactions` | Check and record a transaction |
| `DELETE` | `/api/transactions/{id}` | Remove a transaction and its alert |
| `GET` | `/api/alerts` | Fraud alert queue |
| `PATCH` | `/api/alerts/{id}/status` | Change an alert status |
| `POST` | `/api/demo` | Reset and load sample activity |
