# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Stack
Spring Boot **4.1.x** (not 3.x), Java 21, Maven wrapper, Spring Data JPA + Hibernate 7, PostgreSQL 18 (via `compose.yaml`), H2 for tests only, Bean Validation, Lombok. Development is on Windows: use `.\mvnw.cmd` (PowerShell), or `./mvnw` from Git Bash.

## Commands
- Build + all tests: `.\mvnw.cmd clean verify`
- Tests only: `.\mvnw.cmd test`
- Single test class: `.\mvnw.cmd test "-Dtest=ExpenseControllerTest"`
- Single test method: `.\mvnw.cmd test "-Dtest=ExpenseServiceTest#findById_missing_throwsNotFound"` (keep the quotes; PowerShell mangles unquoted `-D` args)
- Run app: `.\mvnw.cmd spring-boot:run` (port 8080). Docker must be running: `spring-boot-docker-compose` starts the `postgres` service from `compose.yaml` and wires the datasource automatically, so there are no `spring.datasource.*` properties. Containers are stopped (not removed) on app shutdown, and data persists in the `postgres-data` volume.
- DB shell: `docker exec -it expense-tracker-postgres-1 psql -U expenses -d expenses` (host port 5432, db/user/password all `expenses`)
- Wipe DB: `docker compose down -v`

There is no linter or formatter configured.

## Architecture
Package-by-feature under `com.example.expensetracker`:
- `expense/`: the whole feature, layered controller → service → repository. `ExpenseController` (`/api/expenses`) delegates to `ExpenseService`, which owns transactions (class-level `readOnly = true`, with `@Transactional` on writes) and entity↔DTO mapping. Updates rely on JPA dirty checking; there is no explicit `save` in `update`.
- `expense/dto/`: request and response DTOs are Java **records**. `ExpenseRequest` carries all validation annotations. `ExpenseResponse.from(Expense)` is the only mapping point. Never expose the `Expense` entity from the controller.
- `common/error/GlobalExceptionHandler`: every error response is an RFC 9457 `ProblemDetail`. 404 comes from `ExpenseNotFoundException`. A 400 for validation includes an `errors` property (field → message). A 400 is also returned for type mismatch (bad id or `?category=`) and for unreadable or malformed JSON. New exceptions should be mapped here instead of using `ResponseStatusException`.

The `Expense` entity uses Lombok `@Getter/@Setter` with a protected no-args constructor. `id` has no setter, so tests set it via `ReflectionTestUtils.setField`. The `date` field maps to column `expense_date` (`date` is reserved in SQL). Schema is managed by Hibernate (`ddl-auto=update`) with no migrations. Renaming or retyping columns will not be applied to existing data; reset with `docker compose down -v` or introduce Flyway.

## Testing conventions
- Tests never need Docker: Docker Compose support is skipped in tests by default, and `@SpringBootTest` falls back to embedded H2 (test-scoped dependency).
- Service tests: plain JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`), no Spring context.
- Controller tests: `@WebMvcTest(ExpenseController.class)` + `@MockitoBean` (not the removed `@MockBean`). The `GlobalExceptionHandler` is picked up automatically.
- Boot 4 package moves: `@WebMvcTest` is in `org.springframework.boot.webmvc.test.autoconfigure`. Test slices come from separate starters (`spring-boot-starter-webmvc-test`, `-data-jpa-test`). Jackson 3 (`tools.jackson.*`) is the default, so request bodies in tests are JSON text blocks instead of using an `ObjectMapper`.

## Manual testing
`requests.http` covers every endpoint plus the error cases. The first POST stores `{{id}}` for later requests. It needs IntelliJ's HTTP Client, an Ultimate feature; the user's IDE currently has Ultimate modules disabled. Use `curl.exe` or `Invoke-WebRequest` as a fallback.

## Git
- Never commit directly to main. If on main, create a feature branch first (feature/<short-name>).