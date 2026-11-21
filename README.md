# VacancyScout — IT Vacancy Aggregator (Minsk)

Агрегатор IT-вакансий в Минске с AI-генерацией сопроводительных писем на основе резюме пользователя.

## Стек технологий

| Компонент | Технология |
|-----------|-----------|
| Backend | Java 21, Spring Boot 4.2, WebFlux (Netty) |
| Database | PostgreSQL 16 + R2DBC (reactive) |
| Migrations | Liquibase |
| Cache | Redis (reactive) |
| AI | LM Studio (Qwen) — локальная LLM |
| Frontend | Vue 3.5 + TypeScript 5.9 + Vite 8 + Pinia 4 + Tailwind 4 |
| CI/CD | GitHub Actions |
| Deploy | Docker Compose (dev), Kubernetes (prod) |
| Testing | JUnit 5, Mockito, Testcontainers 2, WireMock, Jacoco |

## Быстрый старт

```bash
# 1. Запуск зависимостей (PostgreSQL, Redis)
docker compose up -d db redis

# 2. Запуск приложения
docker compose up -d backend frontend

# 3. Проверка
curl http://localhost:8080/api/companies
curl http://localhost:8080/api/vacancies/search
```

> **Обновление с PostgreSQL 15 → 16:** если у вас уже есть том `postgres_data`
> от `postgres:15`, `docker compose up` упадёт с `database files are incompatible
> with server`. До запуска выполните `./scripts/migrate-postgres-15-to-16.sh`
> (дамп + физическая копия старого тома + восстановление). Подробности —
> [docs/run.md](docs/run.md#14-обновление-с-postgresql-15-на-16-обязательно-при-старом-томе).

## Структура проекта

```
it-vacancy-aggregator/
├── backend/                     # Java 21 Spring Boot WebFlux
│   ├── src/main/java/com/vacancyscout/
│   │   ├── config/              # WebFlux, R2DBC, Redis, Liquibase, LM Studio
│   │   ├── controller/          # REST API endpoints
│   │   │   ├── VacancyController.java
│   │   │   ├── CompanyController.java
│   │   │   └── VacancyUpdatesController.java  # SSE
│   │   ├── ingestion/           # Rabota.by RSS + JSON‑LD парсинг
│   │   │   ├── RabotaByRssSource.java
│   │   │   ├── RabotaByVacancyDetailFetcher.java
│   │   │   └── VacancyIngestionService.java
│   │   ├── service/             # Business logic
│   │   │   ├── VacancySearchService.java
│   │   │   └── company/
│   │   ├── model/               # Company, Vacancy, Resume, Application
│   │   ├── repository/          # R2DBC repositories
│   │   ├── dto/                 # SearchFilters, SearchResponse, VacancyUpdateEvent
│   │   ├── stream/              # VacancyUpdateStream (SSE)
│   │   └── loader/              # CompanySeedLoader (1 demo company)
│   └── src/main/resources/db/changelog/
│       ├── db.changelog-master.xml
│       ├── v1.0.0__init_schema.xml
│       ├── v1.1.0__add_indexes.xml
│       ├── v1.2.0__sample_companies.xml
│       ├── v1.3.0__add_search_vectors.xml
│       └── v1.4.0__vacancy_updates.xml
├── frontend/                    # Vue 3 + TypeScript + Vite
│   ├── src/
│   │   ├── api/                 # REST API clients
│   │   ├── components/          # Dashboard, Vacancies, Resume, AI, Applications
│   │   ├── stores/              # Pinia stores
│   │   ├── router/              # Vue Router
│   │   └── composables/         # Reusable logic
├── k8s/                         # Kubernetes manifests
│   ├── dev/                     # Development environment
│   └── prod/                    # Production environment
├── docs/                        # Documentation
│   └── run.md                   # Local run guide
├── docker-compose.yml           # Local development stack
├── backend/Dockerfile
├── frontend/Dockerfile
└── .github/workflows/ci.yml     # CI/CD pipeline
```

## API Endpoints

### Вакансии
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/vacancies/search` | Поиск вакансий (q, language, source, employmentType, skills, companyName, location, remoteOnly, page, pageSize) |
| GET | `/api/vacancies/updates?since=` | SSE stream обновлений вакансий |

### Компании
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/companies` | Список активных компаний |
| GET | `/api/companies/{id}` | Детали компании |
| POST | `/api/companies` | Создать компанию |
| PUT | `/api/companies/{id}` | Обновить компанию |
| DELETE | `/api/companies/{id}` | Удалить компанию |

## База данных (Liquibase миграции)

### v1.0.0 — Начальная схема
- `users` — пользователи (email, password_hash, name, language)
- `companies` — IT компании Минска (name, careers_url, website_url, scan_status)
- `vacancies` — вакансии (source, title, company, salary, skills, requirements)
- `resumes` — резюме пользователей (content, skills, experience, education)
- `applications` — отклики на вакансии (status, cover_letter)
- `application_history` — история изменений откликов

### v1.1.0 — Индексы
- Индексы по source, company, posted_at, skills (GIN), employment_type, salary, is_active

### v1.2.0 — Seed компании
- EPAM Systems, IBA Group, Wargaming

### v1.3.0 — Поисковые вектора
- `search_vector_ru/en` — tsvector для полнотекстового поиска
- `requirements_en`, `description_en`, `title_translated` — английские версии

### v1.4.0 — Аудит обновлений
- `vacancy_updates` — таблица для аудита изменений вакансий (SSE events)

## SSE (Server-Sent Events)

Поток обновлений вакансий в реальном времени через SSE.

```bash
curl -N -H "Accept: text/event-stream" \
  'http://localhost:8080/api/vacancies/updates?since=2026-01-01T00:00:00'
```

Формат события `VacancyUpdateEvent`:
```json
{
  "id": "UUID",
  "vacancyId": "UUID",
  "title": "Java Developer",
  "companyName": "EPAM Systems",
  "postedAt": "2026-07-10T12:00:00",
  "sourceName": "RABOTA_BY",
  "eventType": "NEW",
  "source": "rabota.by"
}
```

## CI/CD (GitHub Actions)

`.github/workflows/ci.yml` включает:
- Сборка backend (Gradle) и frontend (npm)
- Юнит-тесты с Jacoco coverage report
- Typecheck, линтеры и сборка frontend
- Валидация Liquibase changelog

## Деплой

### Docker Compose (dev)
```bash
docker compose up -d
```

### Kubernetes (dev/prod)
```bash
# Dev
kubectl apply -f k8s/dev/namespace.yaml
kubectl apply -f k8s/dev/

# Prod
kubectl apply -f k8s/prod/namespace.yaml
kubectl apply -f k8s/prod/
```

## Тестирование

```bash
./gradlew :backend:test            # Юнит-тесты
./gradlew :backend:integrationTest # Интеграционные тесты (Testcontainers, нужен Docker)
./gradlew :backend:jacocoTestReport # Coverage report
./gradlew :backend:validateChangelogs # Проверка миграций (нужен запущенный PostgreSQL)

npm --prefix frontend test         # Фронтенд-тесты
npm --prefix frontend run typecheck
npm --prefix frontend run lint
```

## Ingestion (Rabota.by)

Ingestion запускается по расписанию (каждый час) через `VacancyIngestionService`:

1. **RabotaByRssSource** — получает RSS-ленту `/search/vacancy/rss`, парсит XML, собирает список вакансий
2. **RabotaByVacancyDetailFetcher** — для каждой вакансии загружает страницу, извлекает JSON‑LD
3. **VacancyIngestionService** — дедуплицирует по `(sourceName, sourceId)`, автоматически создаёт компании (по названию), публикует SSE-события

RSS не требует OAuth/токенов, работает без регистрации приложения.

## Сканирование компаний

`CompanyScanScheduler` каждые 5 минут проверяет компании со статусами `PENDING`,
`ERROR` и `NO_URL` (пачками по 20, не более 4 параллельно):

1. берёт первый доступный адрес: `careersUrl` → `websiteUrl` → URL последней вакансии-источника;
2. делает GET-запрос с таймаутом 5 с и записывает результат в `companies`:
   - `ACTIVE` — ответ 2xx, `scanError` очищается;
   - `ERROR` — HTTP-ошибка или проблема с сетью, причина пишется в `scanError`;
   - `NO_URL` — адрес найти не удалось;
3. всегда обновляет `lastScanAt`.

Статусы `ERROR` и `NO_URL` перепроверяются при следующем запуске, `PENDING` — один раз.
Настройки задаются в `application.yml` (`company.scan.*`), отключение —
`company.scan.enabled: false`.

## Планируемые функции

- [ ] AI-генерация сопроводительных писем (LM Studio Qwen)
- [ ] Загрузка и парсинг резюме (PDF)
- [ ] Отправка откликов (email, копирование, PDF)
- [ ] Аутентификация (JWT/OAuth2)
