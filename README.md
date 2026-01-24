# Cricnets AI

Cricnets AI is an AI-powered cricket nets booking platform built with Spring Boot and Spring AI. It integrates the Model Context Protocol (MCP) to provide an intelligent interface for managing cricket net bookings.

## Features

- **Cricket Net Bookings**: Manage bookings for cricket net sessions, including different ball types (Leather, Tennis, etc.).
- **AI-Powered Chat**: Interact with the booking system using natural language via Spring AI.
- **Model Context Protocol (MCP)**:
    - **MCP Server**: Exposes booking tools (get available slots, book session, cancel booking, etc.) to MCP clients.
    - **MCP Client**: Capability to connect to external MCP servers to extend functionality.
- **REST API**: Standard endpoints for booking management and chat.
- **Docker Support**: Easily spin up the application and its PostgreSQL database using Docker Compose.

## Prerequisites

- **Java 21** or higher
- **Docker** and **Docker Compose**
- **Gradle** (optional, wrapper provided)

## Tech Stack

- **Framework**: Spring Boot 4.0.1
- **AI**: Spring AI 2.0.0-M1 (with support for OpenAI, Anthropic, Gemini)
- **Database**: PostgreSQL
- **Caching/Memory**: Redis (for persistent chat memory)
- **Documentation**: SpringDoc OpenAPI (Swagger UI)

## Getting Started

### Local Setup

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd cricnets-ai
   ```

2. **Configure Environment**:
   The application uses `src/main/resources/application-local.properties` for local development. Ensure you have a PostgreSQL instance running or use the provided Docker Compose.

3. **Run with Docker Compose**:
   The easiest way to start the required services (PostgreSQL) is via Docker Compose:
   ```bash
   docker-compose up -d
   ```

4. **Build and Run**:
   ```bash
   ./gradlew bootRun
   ```

### Running with Docker

You can run the entire stack (App + DB) using Docker:
```bash
docker-compose up --build
```

## API Endpoints

### Chat
- `GET /chat?message=...`: Interact with the AI assistant.

### Bookings
- `GET /api/bookings/slots?date=YYYY-MM-DD`: Get available slots for a specific date.
- `POST /api/bookings`: Book a new session.
- `GET /api/bookings`: List all bookings.
- `GET /api/bookings/{id}`: Get details of a specific booking.
- `DELETE /api/bookings/{id}`: Cancel a booking.
- `GET /api/bookings/player/{playerName}`: Get bookings for a specific player.
- `GET /api/bookings/upcoming`: Get all upcoming bookings.

### MCP (Model Context Protocol)
- `HTTP Server`: The MCP server is exposed at `/mcp`.
- `Tools`: The server provides the following tools:
    - `get_available_slots`
    - `book_session`
    - `get_player_bookings`
    - `cancel_booking`
    - `get_upcoming_bookings`

## Documentation

Once the application is running, you can access the Swagger UI for full API documentation:
[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## Configuration

Key configurations can be found in `src/main/resources/application.properties`.
- API keys for AI models (OpenAI, Anthropic, Gemini) should be configured if auto-configuration is re-enabled.
- MCP client/server settings are available under `spring.ai.mcp.*`.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
