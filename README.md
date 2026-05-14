# Personal Finance Tracker API

Spring Boot backend for tracking income and expenses, monthly budgets per category, and a dashboard with spend summaries and over-budget alerts.

## Prerequisites

- **Java 21**
- **Maven** (or use the included `./mvnw` / `mvnw.cmd`)
- **PostgreSQL 16** (local install or Docker)

## Database setup

### Option A: Docker Compose

From the project root:

```bash
docker compose up -d
```

This starts PostgreSQL with database `finance_tracker`, user `finance_user`, and password `finance_pass` (see `docker-compose.yml`), matching the default `spring.datasource` values in `src/main/resources/application.yml`.

### Option B: Your own PostgreSQL

Create a database and user, then set `spring.datasource.url`, `username`, and `password` in `application.yml` or via environment variables / profile-specific config.

## Run the application

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

The API listens on **http://localhost:8080** by default.

## OpenAPI / Swagger

With the app running:

| Resource            | URL                                      |
|---------------------|------------------------------------------|
| OpenAPI JSON        | http://localhost:8080/v3/api-docs        |
| Swagger UI          | http://localhost:8080/swagger-ui.html    |

Use **Authorize** in Swagger UI and paste: `Bearer <your_jwt>` after registering or logging in.

JWT signing uses `app.jwt.secret` and `app.jwt.expiration-ms` in `application.yml` (change the secret for production).

Emails are stored and matched in **lowercase**; login accepts any casing that normalizes to the same address.

## Configuration (environment variables)

| Variable | Purpose | Default (local) |
|----------|---------|-------------------|
| `SPRING_DATASOURCE_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/finance_tracker` |
| `SPRING_DATASOURCE_USERNAME` | DB user | `finance_user` |
| `SPRING_DATASOURCE_PASSWORD` | DB password | `finance_pass` |
| `DATABASE_URL` | Render/Railway-style Postgres URL; Docker entrypoint converts it to a JDBC URL when `SPRING_DATASOURCE_URL` is not set | unset |
| `APP_JWT_SECRET` | HS256 signing key (use a long random value in production) | dev placeholder in `application.yml` |
| `APP_JWT_EXPIRATION_MS` | JWT lifetime | `3600000` |
| `APP_CORS_ALLOWED_ORIGIN_PATTERNS` | Comma-separated CORS origin patterns (e.g. `https://myapp.com`,`http://localhost:*`) | `http://localhost:*,http://127.0.0.1:*` |
| `PORT` / `SERVER_PORT` | HTTP port. `PORT` is checked first for PaaS deployment compatibility | `8080` |
| `SPRING_PROFILES_ACTIVE` | Set to `prod` for production defaults | unset (dev-style logging) |

With **`spring.profiles.active=prod`**, SQL logging and Swagger/OpenAPI are turned off (`application-prod.yml`).

## Docker (API + PostgreSQL)

From the project root (set a strong `APP_JWT_SECRET` in your shell or `.env` file before running):

```bash
docker compose up --build
```

- API: **http://localhost:8080**
- Postgres: `localhost:5432` (same credentials as in `docker-compose.yml`)

The `app` service waits for Postgres to become healthy before starting.

## Deployment

This project is ready for a Docker-based deployment on platforms such as Render or Railway.

### Render Blueprint

A `render.yaml` file is included for one-click-style Render deployment. It provisions:

- Docker web service for the Spring Boot API
- Render PostgreSQL database
- `/api/health` health check
- Generated production JWT secret
- Production profile (`SPRING_PROFILES_ACTIVE=prod`)

After Render creates the service, your live API base URL will be shown in the Render dashboard, for example:

```text
https://finance-tracker-api.onrender.com
```

Health check:

```http
GET https://finance-tracker-api.onrender.com/api/health
```

For a browser or frontend client, update `APP_CORS_ALLOWED_ORIGIN_PATTERNS` to include the deployed frontend origin.

### Manual Docker/PaaS deploy

If you deploy without `render.yaml`, set these environment variables:

```text
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=postgresql://user:password@host:5432/database
APP_JWT_SECRET=<long random secret>
APP_CORS_ALLOWED_ORIGIN_PATTERNS=https://your-frontend.example.com
```

You may set `SPRING_DATASOURCE_URL` directly instead of `DATABASE_URL` if your host lets you provide a JDBC URL:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/database
```

## Feature status

MVP implemented:

- User signup/login with JWT auth
- Add, edit, delete, list, and filter transactions by date/category/type
- Monthly budgets per category
- Dashboard total spent, category breakdown, and over-budget alerts
- PostgreSQL + JPA/Hibernate
- JUnit/Mockito unit tests and MockMvc integration tests
- Docker app + database setup
- Swagger/OpenAPI docs for local/dev profile

Not implemented yet:

- Scheduled monthly summary email/report
- CSV import/export
- Redis caching for dashboard
- User/admin role support
- A committed live API URL; add it here after deploying

## Tests

Runs against an **in-memory H2** database (`src/test/resources/application-test.yml`); you do **not** need Postgres for tests.

The `Budget` entity maps calendar fields to columns **`budget_year`** and **`budget_month`** (reserved words like `year` / `month` are avoided for H2 compatibility). Hibernate `ddl-auto: update` on PostgreSQL will create or migrate these columns as needed.

```bash
./mvnw test
```

- **Unit tests** (Mockito): `src/test/java/.../service/*Test.java`
- **Integration tests** (MockMvc, full context): `src/test/java/.../integration/CoreApiIntegrationTest.java`

## Sample HTTP requests

Replace `TOKEN` with the `token` value from `/api/auth/register` or `/api/auth/login`.

### Register

```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "name": "Alex",
  "email": "alex@example.com",
  "password": "secret12"
}
```

### Login

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "alex@example.com",
  "password": "secret12"
}
```

### Create transaction

```http
POST http://localhost:8080/api/transactions
Authorization: Bearer TOKEN
Content-Type: application/json

{
  "type": "EXPENSE",
  "amount": 35.99,
  "category": "Food",
  "date": "2026-05-10",
  "note": "groceries"
}
```

### List transactions (filters and optional pagination)

```http
GET http://localhost:8080/api/transactions?fromDate=2026-05-01&toDate=2026-05-31&category=Food&type=EXPENSE&sort=date,desc
Authorization: Bearer TOKEN
```

Paged example:

```http
GET http://localhost:8080/api/transactions?page=0&size=10&sort=date,desc
Authorization: Bearer TOKEN
```

### Monthly budgets

```http
POST http://localhost:8080/api/budgets
Authorization: Bearer TOKEN
Content-Type: application/json

{
  "year": 2026,
  "month": 5,
  "category": "Food",
  "limitAmount": 200.00
}
```

```http
GET http://localhost:8080/api/budgets?year=2026&month=5
Authorization: Bearer TOKEN
```

### Delete budget

```http
DELETE http://localhost:8080/api/budgets/123
Authorization: Bearer TOKEN
```

### Dashboard (current month if `year` and `month` omitted)

```http
GET http://localhost:8080/api/dashboard?year=2026&month=5
Authorization: Bearer TOKEN
```

### Health (no auth)

```http
GET http://localhost:8080/api/health
```

## cURL quick copy

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Alex","email":"alex@example.com","password":"secret12"}'
```

```bash
TOKEN="<paste token here>"
curl -s http://localhost:8080/api/transactions -H "Authorization: Bearer $TOKEN"
```
