# EvidenceChain Workspace Rules

## Project scope and stack
- Build EvidenceChain in phases. Follow the explicitly approved phase scope; do not begin later phases early.
- Use Java 21, Spring Boot 3.x, Maven, Spring Security with stateless JWT authentication, Spring Data JPA, PostgreSQL, Flyway, Docker Compose, JUnit 5, and Spring Boot Test. Do not change the stack without approval.
- Keep application packages under `com.evidencechain`: `auth`, `cases`, `evidence`, `custody`, `crypto`, `report`, and `common`. Create only packages needed for the approved phase.

## Security and data
- Never put secrets or private keys in code or the repository. Read them from environment variables and document required variables in `.env.example`.
- Hash passwords with BCrypt. Never log passwords or authentication tokens.
- Use DTOs for API requests and responses. Never expose JPA entities through API responses.
- Use synthetic or public data only.

## Quality and records
- Each feature must have unit tests and at least one integration test.
- Forensic records must not be deleted through the API; use lifecycle/status changes where applicable.
- Enforce authorization in Spring Security, including method-level `@PreAuthorize` where required by the approved scope.
