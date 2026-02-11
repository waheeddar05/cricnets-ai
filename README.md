# Cricnets AI

A booking platform for cricket net facilities, built with **Spring Boot 4**, **Spring AI**, and the **Model Context Protocol (MCP)**. Users book sessions through a REST API or by describing what they want in plain English — an LLM routes natural-language commands to the right tool automatically.

## Why this project exists

Cricket net facilities juggle multiple wicket types, ball machines, operator schedules, and overlapping time slots. Most booking systems treat this as a simple calendar problem. This one doesn't — it models operator capacity, machine constraints, and concurrent access as first-class concerns, then layers an AI interface on top so facility managers can run operations conversationally.

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                   Client Layer                      │
│   REST API  ·  Swagger UI  ·  MCP Client (external) │
└──────────┬──────────────────────────┬───────────────┘
           │                          │
           ▼                          ▼
┌─────────────────────┐   ┌─────────────────────────┐
│   REST Controllers  │   │   MCP Server (/mcp)     │
│  Auth · Booking ·   │   │  Streamable HTTP        │
│  Admin · Config     │   │  15 exposed tools       │
└──────────┬──────────┘   └──────────┬──────────────┘
           │                          │
           ▼                          ▼
┌─────────────────────────────────────────────────────┐
│               Tool Registry                         │
│  Reflection-based discovery · @McpTool annotation   │
│  Type coercion · Positional fallback resolution     │
└──────────────────────┬──────────────────────────────┘
                       │
           ┌───────────┼───────────┐
           ▼           ▼           ▼
┌──────────────┐ ┌──────────┐ ┌──────────────────┐
│BookingService│ │UserRepo  │ │NL Interpreter    │
│Slot logic    │ │RoleModel │ │Gemini tool router│
│Lock mgmt     │ │JWT Auth  │ │JSON-strict mode  │
└──────┬───────┘ └────┬─────┘ └──────────────────┘
       │              │
       ▼              ▼
┌─────────────────────────────────────────────────────┐
│                   PostgreSQL                        │
│  Bookings · Users · SystemConfig · BookingLocks     │
└─────────────────────────────────────────────────────┘
```

## Key technical decisions

### MCP server + client in one application

The app runs an MCP server that exposes 15 tools over streamable HTTP at `/mcp`. Any MCP-compatible client (Claude Desktop, custom agents, other Spring AI apps) can connect and operate the booking system without knowing the REST API. Simultaneously, the app acts as an MCP client — it can consume tools from external MCP servers to extend its own capabilities.

### Reflection-based tool registry

Tools are plain Java methods annotated with `@McpTool`. The `ToolRegistry` discovers them at startup via reflection, builds a typed spec catalog, and handles invocation with automatic type coercion (strings to `LocalDate`, `LocalDateTime`, enums, primitives). This means adding a new tool is a single annotated method — no wiring, no registration boilerplate.

### Natural-language command routing

The `/mcp-client/interpret` endpoint accepts free-text commands like *"book me a tennis net tomorrow at 3pm"*. A Gemini model receives the tool catalog as a structured prompt and returns strict JSON identifying the tool and arguments. The registry executes it. No fine-tuning, no function-calling API — just prompt engineering with JSON-mode output.

### Pessimistic locking for concurrent bookings

Each wicket type has its own lock row in a `BookingLock` table. When a booking request arrives, the service acquires a pessimistic lock scoped to that wicket before checking availability. This prevents double-booking under concurrent load without serializing requests across unrelated wickets.

### Operator capacity modeling

Machine bookings (leather ball machine, tennis ball machine) consume operator capacity. The system tracks how many operators are busy in any overlapping time window. If capacity is exhausted, tennis machines auto-downgrade to self-operated mode; leather machines reject the booking. This logic lives in `BookingService`, not in the database, to keep scheduling rules testable.

## MCP tools

**Booking tools** — available to any MCP client:

| Tool | Description |
|---|---|
| `get_available_slots` | Slots for a date and wicket type |
| `book_session` | Book with full parameters (wicket, ball, machine, operator) |
| `book_multiple_slots` | Batch-book with contiguous slot consolidation |
| `get_user_bookings` | Bookings by user email |
| `cancel_booking` | Cancel by ID |
| `get_upcoming_bookings` | All future bookings |

**Admin tools** — facility management:

| Tool | Description |
|---|---|
| `list_all_users` | All registered users |
| `search_users` | Search by name or email |
| `toggle_user_status` | Enable/disable accounts |
| `update_user_role` | Assign USER, ADMIN, or SUPER_ADMIN |
| `get_dashboard_stats` | Total users, bookings, upcoming count |
| `get_system_configs` | Runtime configuration values |
| `update_system_config` | Change slot duration, business hours, etc. |
| `list_all_bookings` | All bookings in the system |
| `mark_booking_as_done` | Mark a booking as completed |

## REST API

### Authentication
- `POST /api/auth/google-login` — Google OAuth login, returns JWT

### Bookings
- `GET /api/bookings/slots?date=YYYY-MM-DD&wicketType=...` — available slots (public)
- `POST /api/bookings` — create booking (authenticated)
- `POST /api/bookings/multi` — batch create (authenticated)
- `GET /api/bookings/mine` — current user's bookings (authenticated)
- `GET /api/bookings/upcoming` — upcoming bookings (public)
- `GET /api/bookings` — all bookings (admin)
- `GET /api/bookings/{id}` — booking detail (admin)
- `DELETE /api/bookings/{id}` — cancel (admin or owner)
- `POST /api/bookings/{id}/done` — mark done (admin)

### Admin
- `GET /api/admin/users` — list users
- `GET /api/admin/users/search?query=...` — search users
- `DELETE /api/admin/users/{id}` — delete user (super admin)
- `POST /api/admin/users/{id}/role` — update role (super admin)
- `POST /api/admin/users/{id}/toggle-status` — enable/disable (super admin)
- `POST /api/admin/invite` — invite admin (super admin)
- `GET /api/admin/stats` — dashboard statistics

### MCP client
- `GET /mcp-client/tools` — list all registered tools
- `POST /mcp-client/tools/{name}` — execute a tool with arguments
- `POST /mcp-client/interpret` — natural language command routing

### Docs & health
- `GET /swagger-ui.html` — interactive API docs
- `GET /actuator/health` — health check

## Tech stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 4.0.1, Java 21 |
| AI | Spring AI 2.0.0-M1, Google Gemini |
| MCP | spring-ai-starter-mcp-server-webmvc, spring-ai-starter-mcp-client |
| Database | PostgreSQL, Spring Data JPA, Hibernate |
| Auth | JWT (jjwt 0.12.6), Google OAuth 2.0, Spring Security |
| Docs | SpringDoc OpenAPI 2.1.0 |
| Infra | Docker multi-stage build, Docker Compose, Railway |

## Running locally

**Prerequisites:** Java 21+, Docker

```bash
# Start PostgreSQL
docker compose up -d postgres

# Run the application
./gradlew bootRun
```

Or run the full stack:

```bash
docker compose up --build
```

The API is available at `http://localhost:8080`. Swagger UI at `http://localhost:8080/swagger-ui.html`.

### Configuration

Business rules are configurable at runtime through `SystemConfig` or in `application.properties`:

```properties
booking.slot-duration-minutes=30
booking.business-hours.start=07:00
booking.business-hours.end=23:00
```

MCP server settings:

```properties
spring.ai.mcp.server.name=wam-cricnets-ai
spring.ai.mcp.server.http.path=/mcp
spring.ai.mcp.server.protocol=streamable
```

## Testing

```bash
./gradlew test
```

Tests cover booking logic (overlap detection, slot alignment, multi-booking consolidation), operator capacity limits, concurrency scenarios, and tool registry reflection/invocation.

## License

MIT
