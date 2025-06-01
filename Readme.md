# Sporty Bets Service

A Spring Boot application for managing bets, settlements, and messaging using RocketMQ and Kafka.

## Architecture Notes

In this single microservice, two domain entities have been combined - BettingEvent and Bet - which would probably be in
different services in real life. In a production environment:

- Service 1 would publish event results when the event finished
- Service 2 would consume this event and process bets

These domains are split into different packages within the codebase. For simple processing logic, two date fields were
added to the bets table: one for initiating settlement and one for completing it.

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker (for RocketMQ)
- Docker Compose (optional, for manual RocketMQ startup)

### Running the Application

1. **Start Kafka and RocketMQ (Docker required):**

   The application expects Kafka and RocketMQ to be available. You can start it using Docker Compose:

   ```bash
   docker-compose up -d
   ```

   This will start both the NameServer and Broker containers.

2. **Run the Application:**

   ```bash
   mvn spring-boot:run
   ```

### Development Profile

To start the application with prepopulated data, use the `dev` Spring profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

This will load sample data into the database for development and testing.

## Notes

- **Database**: Embedded, runs in-memory for local development and tests.
- **Kafka, RocketMQ**: Requires Docker, runs in containers. Ensure Docker is running before starting the app.

## Configuration

- Application properties can be found in `src/main/resources/application.properties`.
- RocketMQ and Kafka topics are configurable via environment variables or properties.

---

## Triggering the API

You can interact with the application's REST API using tools like curl or Postman.
To publish event outcome and initiate bets settlement, send a POST request to the appropriate endpoint (replace
parameters with actual data
curl -X POST "http://localhost:8080/api/events/result" \
-H "Content-Type: application/json" \
-d '{"eventId": "<eventId>", "eventWinnerId": "<winnerId>", "eventName":"eventName"}'

```

For more details, see the source code and comments.
