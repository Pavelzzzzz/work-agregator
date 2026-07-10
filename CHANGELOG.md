# Changelog

## [0.1.0] - 2026-07-10

### Added
- Project skeleton: Gradle (Java 21, Spring Boot 3.3, WebFlux, R2DBC, Liquibase), Vue 3 + TypeScript + Pinia + Tailwind.
- Docker Compose dev stack (`docker-compose.yml`) + K8s manifests (`k8s/dev/`, `k8s/prod/`).
- CI pipeline (GitHub Actions): build, test, lint, liquibaseValidate, Jacoco report, Docker image.

### Models & DTOs
- `Vacancy` — core vacancy entity with builder pattern (21 fields).
- `VacancyTranslation` — multi‑lang title/description/requirements/responsibilities + tsvector for full‑text search.
- `Company` — scan configuration with scan status tracking.
- `SearchFilters`, `SearchResponse`, `VacancyUpdateEvent` DTOs.

### Database
- Liquibase changelogs v1.0.0–v1.5.0: schema init, indexes, sample companies, search vectors, vacancy updates, vacancy_translations.
- Full‑text search via PostgreSQL tsvector (Russian/English).
- Vacancy updates audit table for SSE streaming.

### Backend
- `VacancySearchService` — paginated full‑text search with language/location/employment type/skills filters.
- `CompanyService` — CRUD for scan sources.
- `VacancyUpdateStream` — reactive SSE stream via `Sinks.Many`.
- REST endpoints: `GET /api/vacancies/search`, `GET /api/vacancies/updates` (SSE), `CRUD /api/companies`.
- `CompanySeedLoader` — seeds EPAM, IBA, Wargaming at startup.
- `LiquibaseConfig` — conditional Liquibase runner.

### AI Integration
- Spring AI with LM Studio (Qwen 2.5 7B) for cover letter generation (configured via `application.properties`).

### Linters & Code Quality
- **Backend**: Spotless (Google Java format), Checkstyle, PMD.
- **Frontend**: ESLint + typescript-eslint + eslint-plugin-vue, Prettier.
- All lint steps integrated into `./gradlew check` and CI pipeline.

### Infrastructure
- Dockerfiles: multi‑stage backend (JRE slim), frontend (Nginx).
- K8s: namespaces, deployments, services, ingress, HPA (prod), secret templates.
- `docs/run.md`, `docs/architecture.md`, `frontend/README.md`.
