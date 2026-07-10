# Changelog

## [0.3.0] - 2026-07-10

### Full-Text Search & Advanced Filters

- **Полнотекстовый поиск** — через `vacancy_translations.search_vector` (tsvector) с поддержкой русского и английского языков.
- **Расширенные фильтры** — source, employmentType, companyName, location, remoteOnly, minSalary, maxSalary, skills — все фильтры применяются на стороне БД.
- **Динамический SQL** — `VacancySearchService` строит запрос через `DatabaseClient` с учётом только активных фильтров.
- **Пагинация на уровне БД** — `LIMIT/OFFSET` + отдельный `COUNT(*)` для точного total.
- **Frontend** — поля ввода для skills (comma-separated), salary min/max, employment type select, remote checkbox.

### Ingestion (Rabota.by)

- `RabotaByRssSource` — парсинг RSS-ленты `/search/vacancy/rss` (без OAuth).
- `RabotaByVacancyDetailFetcher` — извлечение JSON‑LD со страницы вакансии.
- `VacancyIngestionService` — шедулер (каждый час), дедупликация по `(sourceName, sourceId)`, SSE-публикация, авто-создание компаний по названию.
- `CompanySeedLoader` — сокращён до одной демо-компании; seed-компании больше не хардкодятся.

### Backend

- Исправления Checkstyle: `MethodName` разрешает подчёркивания.
- Исправления PMD: удалены несовместимые правила для PMD 7.
- `CompanySeedLoader` — фикс R2DBC `INSERT` (Persistable).

### Frontend

- Исправлены ESLint-ошибки парсинга (optional chaining, optional catch, TypeScript в Vue SFC).
- ESLint flat config: корректная настройка `vue-eslint-parser` + `@typescript-eslint/parser`.
- Убраны `any` в catch-блоках Pinia-хранилищ.
- Форматирование авто-исправлено (attributes order, hyphens, self-closing).
- Линт проходит с 0 errors и 0 warnings.

### CI

- ESLint 9 flat config (удалён флаг `--ext`).
- `--max-warnings 0` для строгой валидации.

## [0.2.0] - 2026-07-10

### Frontend
- Full UI layout: NavBar, Dashboard, Vacancy search, Companies list.
- Pinia stores (`company`, `vacancy`) with API clients (Axios).
- TypeScript types (`Company`, `Vacancy`, `SearchFilters`, `SearchResponse`, `VacancyUpdateEvent`).
- Vite proxy (`/api` → backend) for local dev.
- Proper Tailwind CSS setup (postcss + tailwind config).
- SSE subscription for live vacancy updates on Dashboard.

### Backend
- `Company` model: `record` → `class` with `Persistable<UUID>` for correct R2DBC INSERT handling.
- `CompanySeedLoader`: fixed `saveAll().subscribe()` → `saveAll().then().block()`, removed hardcoded UUID.
- `liquibase.url` configured directly in `application.yml` (bypasses JDBC `DataSource` bean in reactive mode).
- GIN indexes via raw SQL (`CREATE INDEX ... USING GIN`) instead of unsupported `indexType`.
- `v1.2.0__sample_companies.xml` removed (dead code, duplicated `CompanySeedLoader`).
- `@JsonIgnore` on `isNew()` — no internal persistence state in API responses.

### CI
- Fixed `ci.yml`: uses root `gradlew` with `:backend:` prefix, removed non-existent `integrationTest` step.
- Fixed ESLint scripts (removed `--ext` flag, unsupported in ESLint 9).
- Fixed Checkstyle config XML (duplicate `name` attributes).
- PMD ruleset tuned for PMD 7.x compatibility.

### Docker
- Fixed frontend port mapping: `5173:5173` → `5173:80` (nginx listens on port 80).
- Added Vite proxy config for development builds.

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
