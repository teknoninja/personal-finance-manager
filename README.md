# Personal Finance Manager

A Spring Boot 3 REST API for tracking income, expenses, categories, savings goals, and generating
financial reports — built for the Personal Finance Manager assignment.

## Tech Stack

| Component          | Technology                          |
|---------------------|--------------------------------------|
| Language            | Java 17                              |
| Framework           | Spring Boot 3.3.4                    |
| Security            | spring-boot-starter-security (session-based) |
| Persistence         | Spring Data JPA + H2 (file-based)    |
| Validation          | spring-boot-starter-validation (Jakarta Bean Validation) |
| Testing             | JUnit 5, Mockito, Spring MockMvc     |
| Build Tool          | Maven                                |

## Architecture

Layered architecture throughout:

```
Controller  →  Service  →  Repository  →  Database
   ↓              ↓
  DTOs        Entities
```

- **`controller/`** — thin REST controllers; no business logic. Resolve the authenticated user via
  `CurrentUserProvider` and delegate to services.
- **`service/`** — business logic, validation rules, and authorization checks (ownership).
- **`repository/`** — Spring Data JPA repositories.
- **`entity/`** — JPA entities (`User`, `Category`, `Transaction`, `SavingsGoal`).
- **`dto/request` / `dto/response`** — request/response objects, kept fully separate from entities.
- **`exception/`** — custom exceptions (`ValidationException`, `ResourceNotFoundException`,
  `ForbiddenOperationException`, `ConflictException`) + a global `@ControllerAdvice` handler that
  maps every expected error condition to a well-formed 4xx JSON body — no scenario in the spec
  should ever surface a raw 5xx.
- **`config/`** — Spring Security configuration, a custom JSON `AuthenticationEntryPoint` (so
  unauthenticated calls return `401` JSON instead of an HTML login redirect), and a
  `DataInitializer` that seeds the default categories on startup.

## Key Design Decisions

- **Session-based authentication.** Login authenticates credentials via `AuthenticationManager`
  and persists the resulting `SecurityContext` into the `HttpSession`, which issues a
  `JSESSIONID` cookie. Every subsequent request supplies that cookie. Logout invalidates the
  session. Passwords are hashed with BCrypt — never stored or returned in plaintext.
- **Data isolation.** Every transaction/goal/custom-category query is scoped to the authenticated
  user; accessing another user's owned resource returns `403 Forbidden`, a non-existent resource
  returns `404 Not Found`.
- **Categories.** Seven default categories (`Salary` / `Food`, `Rent`, `Transportation`,
  `Entertainment`, `Healthcare`, `Utilities`) are seeded once at startup and are global
  (`owner = null`); they can never be edited or deleted. Custom categories belong to exactly one
  user and must have a name that's unique for that user. A category still referenced by a
  transaction cannot be deleted (`400`); deleting someone else's or a default category is `403`.
- **Transactions.** `type` (`INCOME`/`EXPENSE`) is derived from the chosen category, so it's
  always consistent. The `date` field is immutable after creation, is validated in
  `YYYY-MM-DD` and rejected if it's in the future. Deletes are hard deletes, so they're
  automatically excluded from goal progress and report calculations, as required.
- **Savings goals.** Progress is computed live as `SUM(income) - SUM(expenses)` for transactions
  dated on/after the goal's `startDate`, so it always reflects the current transaction state (no
  cached/stale progress values).
- **Reports.** Monthly/yearly reports aggregate amounts per category name for income and expenses
  separately and compute net savings, computed live from the transaction table.
- **Global exception handling.** A single `@ControllerAdvice` translates bean-validation failures,
  auth failures, ownership violations, missing resources, and conflicts into the exact 4xx codes
  the spec calls for, each with a descriptive JSON error body.

## Running Locally

Requirements: JDK 17+, Maven 3.9+ (or use the included wrapper if you generate one with
`mvn -N io.takari:maven:wrapper`).

```bash
mvn clean package
java -jar target/personal-finance-manager.jar
```

The API starts on `http://localhost:8080/api` by default (configurable via the `PORT` env var).
Data is persisted to a local H2 file database under `./data/` between restarts (safe to delete to
reset all data). An H2 console is available at `/h2-console` for local debugging (JDBC URL:
`jdbc:h2:file:./data/financedb`, user `sa`, empty password).

## Running Tests

```bash
mvn test
```

This runs:
- **Unit tests** (`src/test/.../service/*Test.java`) — Mockito-based tests for every service,
  covering the success path and every documented error path (validation failures, 403/404/409
  scenarios).
- **Integration test** (`src/test/.../controller/FinanceManagerIntegrationTest.java`) — a full
  MockMvc/Spring context test that drives the real HTTP layer end-to-end: register → login (with
  session cookie) → categorize → transact → update → goal creation/progress → monthly report →
  delete → verify report updates → logout → verify session invalidated.

A JaCoCo report is generated at `target/site/jacoco/index.html` after `mvn test`.

## Deploying to Render

1. Push this repository to GitHub (public).
2. In Render, create a **new Web Service** from that repo.
3. Settings:
   - **Runtime:** Docker *or* Java (Native Environment)
   - **Build Command:** `mvn clean package -DskipTests`
   - **Start Command:** `java -jar target/personal-finance-manager.jar`
   - **Environment variable:** Render automatically injects `PORT`; the app already reads it via
     `application.yml` (`server.port: ${PORT:8080}`).
4. Once deployed, your base URL will look like `https://<your-service>.onrender.com/api`.
5. Run the provided test script against it:
   ```bash
   bash financial_manager_tests.sh https://<your-service>.onrender.com/api
   ```
   > Note: the free Render tier spins down when idle, so the first request after inactivity can
   > take ~30–60s to wake the instance — this is expected and not an application bug.

## API Overview

All endpoints are prefixed with `/api`. See the assignment spec for full request/response
payloads; summary below.

| Method | Endpoint                              | Auth required | Description |
|--------|----------------------------------------|:---:|--------------|
| POST   | `/auth/register`                       | ❌ | Register a new user |
| POST   | `/auth/login`                          | ❌ | Log in, returns session cookie |
| POST   | `/auth/logout`                         | ✅ | Invalidate session |
| POST   | `/transactions`                        | ✅ | Create a transaction |
| GET    | `/transactions`                        | ✅ | List transactions (filter: `startDate`, `endDate`, `categoryId`, `type`) |
| PUT    | `/transactions/{id}`                   | ✅ | Update a transaction (not `date`) |
| DELETE | `/transactions/{id}`                   | ✅ | Delete a transaction |
| GET    | `/categories`                          | ✅ | List default + own custom categories |
| POST   | `/categories`                          | ✅ | Create a custom category |
| DELETE | `/categories/{name}`                   | ✅ | Delete own custom category |
| POST   | `/goals`                               | ✅ | Create a savings goal |
| GET    | `/goals`                               | ✅ | List own goals |
| GET    | `/goals/{id}`                          | ✅ | Get one goal |
| PUT    | `/goals/{id}`                          | ✅ | Update target amount/date |
| DELETE | `/goals/{id}`                          | ✅ | Delete a goal |
| GET    | `/reports/monthly/{year}/{month}`      | ✅ | Monthly income/expense/net report |
| GET    | `/reports/yearly/{year}`               | ✅ | Yearly income/expense/net report |

### Error format

Every error response follows the same shape:

```json
{
  "timestamp": "2026-08-10T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": ["amount must be a positive value"]
}
```

## Project Structure

```
src/main/java/com/financemanager/
├── FinanceManagerApplication.java
├── config/          SecurityConfig, CustomAuthenticationEntryPoint, DataInitializer
├── controller/       AuthController, TransactionController, CategoryController,
│                      GoalController, ReportController
├── dto/request/      RegisterRequest, LoginRequest, TransactionRequest, ...
├── dto/response/      RegisterResponse, TransactionResponse, GoalResponse, ...
├── entity/           User, Category, Transaction, SavingsGoal, CategoryType, TransactionType
├── exception/        Custom exceptions + GlobalExceptionHandler
├── repository/        UserRepository, CategoryRepository, TransactionRepository, SavingsGoalRepository
└── service/           AuthService, CategoryService, TransactionService, GoalService,
                        ReportService, CurrentUserProvider, CustomUserDetailsService
```
