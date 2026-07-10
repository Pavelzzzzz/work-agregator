# VacancyScout — IT Vacancy Aggregator (Minsk)

Агрегатор IT-вакансий в Минске с AI-генерацией сопроводительных писем на основе резюме пользователя.

## Стек технологий

| Компонент | Технология |
|-----------|-----------|
| Backend | Java 21, Spring Boot 3.3+, WebFlux (Netty) |
| Database | PostgreSQL 16 + R2DBC (reactive) |
| Migrations | Liquibase |
| Cache | Redis (reactive) |
| AI | LM Studio (Qwen) — локальная LLM |
| Frontend | Vue 3 + TypeScript + Vite + Pinia + Tailwind |
| CI/CD | GitHub Actions |
| Deploy | Docker Compose (dev), Kubernetes (prod) |
| Testing | JUnit 5, Mockito, Testcontainers, WireMock, Jacoco |

## Быстрый старт

```bash
# 1. Запуск зависимостей (PostgreSQL, Redis)
docker-compose up -d db redis

# 2. Запуск приложения
docker-compose up -d backend frontend

# 3. Проверка
curl http://localhost:8080/api/companies
curl http://localhost:8080/api/vacancies/search
```

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
- Интеграционные тесты
- Валидация Liquibase changelog
- Сборка Docker образов

## Деплой

### Docker Compose (dev)
```bash
docker-compose up -d
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
cd backend
./gradlew test                    # Юнит-тесты
./gradlew jacocoTestReport        # Coverage report
./gradlew liquibaseValidate       # Проверка миграций
```

## Ingestion (Rabota.by)

Ingestion запускается по расписанию (каждый час) через `VacancyIngestionService`:

1. **RabotaByRssSource** — получает RSS-ленту `/search/vacancy/rss`, парсит XML, собирает список вакансий
2. **RabotaByVacancyDetailFetcher** — для каждой вакансии загружает страницу, извлекает JSON‑LD
3. **VacancyIngestionService** — дедуплицирует по `(sourceName, sourceId)`, автоматически создаёт компании (по названию), публикует SSE-события

RSS не требует OAuth/токенов, работает без регистрации приложения.

## Планируемые функции

- [ ] Полнотекстовый поиск вакансий (RU/EN) через tsvector
- [ ] Расширенные фильтры (навыки, зарплата, тип занятости)
- [ ] AI-генерация сопроводительных писем (LM Studio Qwen)
- [ ] Загрузка и парсинг резюме (PDF)
- [ ] Отправка откликов (email, копирование, PDF)
- [ ] Аутентификация (JWT/OAuth2)
