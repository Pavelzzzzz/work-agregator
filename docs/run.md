# Run guide — локальный стек разработки VacancyScout

## Предварительные требования

- Docker 24+ и Docker Compose v2
- Java 21 (для локального запуска без Docker)
- Node.js 20+ и npm (для фронтенда)
- Gradle 8.5+ (или используйте `gradlew`)

## 1. Быстрый старт через Docker Compose

### 1.1 Запуск инфраструктуры

```bash
# Запуск PostgreSQL и Redis
docker-compose up -d db redis

# Проверка
docker-compose ps
```

### 1.2 Запуск приложения

```bash
# Сборка и запуск backend + frontend
docker-compose up -d backend frontend

# Просмотр логов
docker-compose logs -f backend
docker-compose logs -f frontend
```

### 1.3 Проверка работоспособности

```bash
# API компаний
curl http://localhost:8080/api/companies

# API поиска вакансий
curl "http://localhost:8080/api/vacancies/search?q=Java&language=ru"

# SSE поток обновлений
curl -N -H "Accept: text/event-stream" \
  'http://localhost:8080/api/vacancies/updates?since=2026-01-01T00:00:00'

# Health check
curl http://localhost:8080/actuator/health
```

## 2. Локальный запуск (без Docker)

### 2.1 PostgreSQL и Redis

```bash
# PostgreSQL
docker run -d --name vacancy-db \
  -e POSTGRES_USER=vacancy_scout \
  -e POSTGRES_PASSWORD=vacancy_scout \
  -e POSTGRES_DB=vacancy_scout \
  -p 5432:5432 postgres:15

# Redis
docker run -d --name vacancy-redis \
  -p 6379:6379 redis:7
```

### 2.2 Backend

```bash
cd backend

# Сборка
./gradlew build

# Запуск
./gradlew bootRun
# или
java -jar build/libs/*.jar
```

### 2.3 Frontend

```bash
cd frontend

# Установка зависимостей
npm ci

# Запуск dev сервера
npm run dev
# Доступен на http://localhost:5173
```

## 3. Миграции Liquibase

Миграции выполняются автоматически при старте backend.
При необходимости можно запустить вручную:

```bash
cd backend
./gradlew liquibaseUpdate       # Применить миграции
./gradlew liquibaseValidate     # Проверить changelog
./gradlew liquibaseStatus       # Статус миграций
```

### Доступные миграции

| Версия | Файл | Описание |
|--------|------|----------|
| v1.0.0 | `v1.0.0__init_schema.xml` | Начальная схема БД (users, companies, vacancies, resumes, applications) |
| v1.1.0 | `v1.1.0__add_indexes.xml` | Индексы для поиска и производительности |
| v1.2.0 | `v1.2.0__sample_companies.xml` | Seed-данные компаний (EPAM, IBA, Wargaming) |
| v1.3.0 | `v1.3.0__add_search_vectors.xml` | tsvector для полнотекстового поиска RU/EN |
| v1.4.0 | `v1.4.0__vacancy_updates.xml` | Аудит обновлений вакансий для SSE |

## 4. Seed-данные

При старте приложения `CompanySeedLoader` создаёт одну демо-компанию,
если таблица `companies` пуста. Остальные компании создаются автоматически
при ингесте вакансий (по названию компании из вакансии).

Дополнительные компании можно добавить через API:

```bash
curl -X POST http://localhost:8080/api/companies \
  -H "Content-Type: application/json" \
  -d '{
    "name": "SoftServe",
    "careersUrl": "https://jobs.softserveinc.com",
    "websiteUrl": "https://www.softserveinc.com",
    "description": "IT consulting and software development",
    "isActive": true
  }'
```

## 5. Ingestion вакансий (Rabota.by)

Ingestion запускается автоматически каждый час через `VacancyIngestionService`.

### Ручной запуск

При старте backend ingestion запускается сразу (если `app.ingestion.enabled=true`, значение по умолчанию).

### Проверка результата

```bash
# После запуска ingestion (ждём до 1 минуты)
curl http://localhost:8080/api/vacancies/search?pageSize=5
curl http://localhost:8080/api/companies
```

### Как это работает

1. `RabotaByRssSource` → `GET /search/vacancy/rss` (XML)
2. `RabotaByVacancyDetailFetcher` → страница вакансии → JSON‑LD
3. `VacancyIngestionService` → дедупликация + создание компании + SSE

RSS-лента не требует токена доступа.

## 6. SSE поток обновлений

### Эндпоинт

```
GET /api/vacancies/updates?since=<ISO_LOCAL_DATE_TIME>
```

### Пример

```bash
curl -N -H "Accept: text/event-stream" \
  'http://localhost:8080/api/vacancies/updates?since=2026-07-01T00:00:00'
```

### Формат события

```
data:{"id":"uuid","vacancyId":"uuid","title":"Java Developer","companyName":"EPAM","postedAt":"2026-07-10T12:00:00","sourceName":"RABOTA_BY","eventType":"NEW","source":"rabota.by"}
```

### Параметры

- `since` — ISO дата/время, с которого получать обновления (опционально)

## 7. API эндпоинты

### Вакансии

```bash
# Поиск с фильтрами
GET /api/vacancies/search?q=Java&language=ru&source=RABOTA_BY&employmentType=FULL_TIME&page=0&pageSize=20

# Параметры:
# q           - поисковый запрос
# language    - ru (русский) | en (английский)
# source      - RABOTA_BY
# employmentType - FULL_TIME | PART_TIME | CONTRACT | REMOTE
# skills      - Java,Spring (через запятую)
# companyName - название компании
# location    - город
# remoteOnly  - true/false
# page        - номер страницы (0+)
# pageSize    - размер страницы (1-100)
```

### Компании

```bash
GET    /api/companies          # Список компаний
GET    /api/companies/{id}     # Детали компании
POST   /api/companies          # Создать компанию
PUT    /api/companies/{id}     # Обновить компанию
DELETE /api/companies/{id}     # Удалить компанию
```

## 8. Тестирование

### Backend

```bash
cd backend

# Юнит-тесты
./gradlew test

# Проверка стиля
./gradlew spotlessCheck checkstyleMain pmdMain

# Все тесты с coverage
./gradlew test jacocoTestReport

# Открыть отчет Jacoco
open build/reports/jacoco/test/html/index.html
```

### Frontend

```bash
cd frontend
npm run lint              # ESLint
npm run format:fix        # Prettier
```

## 9. CI/CD Pipeline

GitHub Actions workflow — сборка, тесты, линтеры (Spotless, Checkstyle, PMD, ESLint), сборка Docker-образов.

GitHub Actions workflow (`.github/workflows/ci.yml`):

| Шаг | Описание |
|-----|----------|
| Checkout | Клонирование репозитория |
| JDK 21 | Установка Java 21 (Temurin) |
| Cache | Кэширование Gradle зависимостей |
| Build backend | `./gradlew build` |
| Build frontend | `npm ci && npm run build` |
| Unit tests | `./gradlew test` |
| Integration tests | `./gradlew integrationTest` |
| Liquibase validate | `./gradlew liquibaseValidate` |
| Jacoco report | `./gradlew jacocoTestReport` |

## 10. Docker сборка

### Backend Dockerfile

Многостадийная сборка:
1. **builder** — `gradle:8.5-jdk21` собирает JAR
2. **run** — `openjdk:21-jre-slim` запускает JAR

### Frontend Dockerfile

1. **build** — `node:20-alpine` собирает статику через Vite
2. **run** — `nginx:stable-alpine` отдаёт статику

### Переменные окружения

| Переменная | Описание | По умолчанию |
|------------|----------|-------------|
| `DB_PASSWORD` | Пароль PostgreSQL | `changeme` |
| `R2DBC_URL` | Reactive JDBC URL | `r2dbc:postgresql://db:5432/vacancy_scout` |
| `LM_STUDIO_BASE_URL` | URL LM Studio API | `http://localhost:1234/v1` |

## 11. Kubernetes развёртывание

### Dev окружение

```bash
kubectl apply -f k8s/dev/namespace.yaml
kubectl apply -f k8s/dev/
kubectl get pods -n vacancy-dev
```

### Prod окружение

```bash
kubectl apply -f k8s/prod/namespace.yaml
kubectl apply -f k8s/prod/
kubectl get pods -n vacancy-prod
```

### Манифесты

| Файл | Описание |
|------|----------|
| `namespace.yaml` | Пространство имён |
| `secret.yaml` | Секреты (пароль БД, URL) |
| `backend-deployment.yaml` | Deployment backend (dev: 1 replica, prod: 3 replicas) |
| `backend-service.yaml` | Service backend |
| `frontend-deployment.yaml` | Deployment frontend (dev: 1 replica, prod: 2 replicas) |
| `frontend-service.yaml` | Service frontend (prod: LoadBalancer) |
| `ingress.yaml` | Ingress с nginx controller |

## 12. Troubleshooting

### База данных не подключается

```bash
# Проверка PostgreSQL
docker-compose logs db
docker exec -it vacancy-scout-db-1 psql -U vacancy_scout -d vacancy_scout
```

### Миграции не применяются

```bash
# Проверка Liquibase
cd backend
./gradlew liquibaseStatus
./gradlew liquibaseValidate
```

### SSE не работает

```bash
# Проверка эндпоинта
curl -N -H "Accept: text/event-stream" \
  'http://localhost:8080/api/vacancies/updates?since=2026-01-01T00:00:00'

# Проверка логов backend
docker-compose logs backend
```

### Frontend не подключается к API

Проверьте proxy настройки в `frontend/vite.config.ts`.
Frontend ожидает backend на `http://localhost:8080`.
