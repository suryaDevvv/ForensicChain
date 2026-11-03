# EvidenceChain — Phase 1

EvidenceChain Phase 1 provides stateless JWT authentication, role-based user administration, and case management backed by PostgreSQL. This phase does not include evidence upload, encryption, custody ledgers, or AI analysis.

## Requirements

- Java 21
- Maven 3.9+
- Docker Desktop with Docker Compose

## Local setup

1. Copy `.env.example` to `.env`.
2. Set `POSTGRES_PASSWORD` and `DB_PASSWORD` to the same local database password. Set `BOOTSTRAP_ADMIN_USERNAME` and `BOOTSTRAP_ADMIN_PASSWORD`; the bootstrap password must be at least 12 characters.
3. Set `JWT_SECRET_BASE64` to Base64 encoding of at least 32 random bytes. For example, in PowerShell:

   ```powershell
   $bytes = New-Object byte[] 32
   [System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
   [Convert]::ToBase64String($bytes)
   ```

   Put the generated value in `.env`. Keep `.env` private; it is ignored by Git.

4. Start PostgreSQL and run the tests:

   ```powershell
   docker compose up -d
   mvn test
   ```

   Integration tests use PostgreSQL through Testcontainers and need Docker running. The first test run downloads the PostgreSQL image.

5. Start the application:

   ```powershell
   mvn spring-boot:run
   ```

The app reads `.env` when it starts. Flyway applies migrations automatically. Hibernate validates the migrated schema and does not create or alter tables. The default profile is `dev`; integration tests use the `test` profile.

## Bootstrap administrator

On startup, if the database has no ADMIN user, the app creates one from `BOOTSTRAP_ADMIN_USERNAME` and `BOOTSTRAP_ADMIN_PASSWORD`. The password is stored as a BCrypt hash and is never logged. If an ADMIN already exists, the bootstrap variables are not used. If no ADMIN exists and either bootstrap value is missing or the password is too short, startup fails.

## API

All requests and responses use JSON. New public registrations always receive the INVESTIGATOR role. Only ADMIN users can create users with a chosen role. Cases start OPEN, and CLOSED is terminal: every later `PUT` returns 409 Conflict. Case creation uses the authenticated username to set `created_by`; that field is not accepted from the request.

| Method | Path | Access | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Register an INVESTIGATOR |
| `POST` | `/api/auth/login` | Public | Return a bearer JWT |
| `POST` | `/api/admin/users` | ADMIN | Create an ADMIN, INVESTIGATOR, or AUDITOR |
| `POST` | `/api/cases` | ADMIN, INVESTIGATOR | Create a case |
| `GET` | `/api/cases` | Any authenticated role | List cases, newest first |
| `GET` | `/api/cases/{id}` | Any authenticated role | Read one case |
| `PUT` | `/api/cases/{id}` | ADMIN, INVESTIGATOR | Update an OPEN case or close it |

There is no delete endpoint. Errors use a consistent JSON response with `timestamp`, `status`, `code`, `message`, and `path` fields. Login returns the same 401 error for an unknown username and an incorrect password.

## Example requests (PowerShell)

Start the app in one terminal. In another terminal, register and log in an investigator, then call a protected endpoint:

```powershell
$registerBody = '{"username":"investigator1","password":"Investigator-example-123"}'
curl.exe -i -X POST http://localhost:8080/api/auth/register -H 'Content-Type: application/json' -d $registerBody

$loginBody = '{"username":"investigator1","password":"Investigator-example-123"}'
$investigatorToken = ((curl.exe -sS -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' -d $loginBody) | ConvertFrom-Json).accessToken
curl.exe -i http://localhost:8080/api/cases -H "Authorization: Bearer $investigatorToken"
```

To create an AUDITOR, log in with the bootstrap ADMIN and use the credentials configured in `.env`:

```powershell
$adminLoginBody = '{"username":"admin","password":"REPLACE_WITH_BOOTSTRAP_ADMIN_PASSWORD"}'
$adminToken = ((curl.exe -sS -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' -d $adminLoginBody) | ConvertFrom-Json).accessToken
$auditorBody = '{"username":"auditor1","password":"Auditor-example-password-123","role":"AUDITOR"}'
curl.exe -i -X POST http://localhost:8080/api/admin/users -H 'Content-Type: application/json' -H "Authorization: Bearer $adminToken" -d $auditorBody

$auditorLoginBody = '{"username":"auditor1","password":"Auditor-example-password-123"}'
$auditorToken = ((curl.exe -sS -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' -d $auditorLoginBody) | ConvertFrom-Json).accessToken
$caseBody = '{"title":"Synthetic verification case","description":"Public test data"}'
curl.exe -i -X POST http://localhost:8080/api/cases -H 'Content-Type: application/json' -H "Authorization: Bearer $auditorToken" -d $caseBody
```

The final request above returns 403. A protected request without a bearer token returns 401:

```powershell
curl.exe -i http://localhost:8080/api/cases
```

## Phase 1 data model

- `users`: `id`, `username`, `password_hash`, `role`, `created_at`.
- `cases`: `id`, `title`, `description`, `status`, `created_by`, `created_at`.

Later phases should add their schema changes as new Flyway migrations; do not edit the already-applied V1 migration.
