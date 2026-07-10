# VacancyScout — Архитектура

## Общая схема

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Frontend (Vue 3)                             │
│  ┌──────────┐ ┌──────────┐ ┌────────────┐ ┌──────────────────┐    │
│  │ Dashboard │ │Vacancies │ │ Resume     │ │ Applications     │    │
│  └──────────┘ └──────────┘ └────────────┘ └──────────────────┘    │
└─────────────────────────────────────────────────────────────────────┘
                                  │ REST + SSE
┌─────────────────────────────────────────────────────────────────────┐
│                    Spring Boot (WebFlux + Netty)                    │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  VacancyController │ CompanyController │ VacancyUpdatesSSE  │   │
│  └─────────────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  VacancySearchService │ CompanyService │ ApplicationService  │   │
│  │  CoverLetterGenerator │ ResumeParser   │ ExportService       │   │
│  └─────────────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  VacancyScannerService (hh.ru, rabota.by, LinkedIn, DJINNI)  │   │
│  │  CompanyWebsiteSource | RateLimiter | Deduplication          │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
                              │ R2DBC / Redis
┌─────────────────────────────────────────────────────────────────────┐
│              PostgreSQL + Redis + LM Studio (Qwen)                 │
└─────────────────────────────────────────────────────────────────────┘
```

## Реактивный стек (WebFlux)

Все слои приложения построены на реактивных принципах:

- **WebFlux (Netty)** — неблокирующий HTTP сервер
- **R2DBC** — реактивный драйвер для PostgreSQL
- **Redis (Reactive)** — кэширование и rate limiting
- **Project Reactor** — `Mono`/`Flux` для асинхронных операций
- **SSE (Server-Sent Events)** — реактивный стриминг обновлений

## Virtual Threads (Java 21)

Несмотря на реактивный стек, Virtual Threads используются для:
- Фоновых задач сканирования вакансий
- Парсинга PDF/DOCX файлов
- Обработки блокирующих операций (если есть legacy код)

## Поток данных

### Сканирование вакансий

```
Scheduler (каждый час)
    ↓
VacancyScannerService
    ├── hh.ru (API/парсинг)
    ├── rabota.by (API)
    ├── LinkedIn (парсинг)
    ├── DJINNI (парсинг)
    └── Company websites (парсинг)
    ↓
RateLimiter → Deduplication → VacancyRepository
    ↓
VacancyUpdateStream.publish(event) → SSE clients
```

### Поиск вакансий

```
Client → GET /api/vacancies/search?q=Java&language=ru
    ↓
VacancyController → VacancySearchService
    ↓
VacancyRepository (R2DBC)
    ├── searchInRussian (tsvector RU)
    └── searchInEnglish (tsvector EN)
    ↓
SearchResponse → Client (JSON)
```

### AI генерация отклика

```
Client → POST /api/ai/cover-letter/{vacancyId}
    ↓
CoverLetterGeneratorService
    ├── Загрузка резюме пользователя
    ├── Получение требований вакансии
    ├── Формирование prompt для LLM
    └── LM Studio (Qwen) → Сопроводительное письмо
    ↓
Client → Редактирование → Отправка
```

## База данных

### Схема (основные таблицы)

```
users ──┐
        ├── resumes
        ├── applications ── vacancies
        └── application_history

companies ── (vacancies через company_name)

vacancies ── vacancy_updates (SSE audit)
```

### Индексы

- GIN индексы на `skills` и `search_vector_ru/en` для полнотекстового поиска
- B-tree индексы на `posted_at`, `company_name`, `source_name`
- Уникальный индекс на `(source_name, source_id)` для дедупликации

## Безопасность

- **MVP**: Открытый доступ (без аутентификации)
- **Перспектива**: JWT/OAuth2 аутентификация
- **SSE**: Открытый поток (можно ограничить через аутентификацию)

## Масштабирование

- **Горизонтальное**: Kubernetes HPA (3–10 реплик backend)
- **SSE**: При масштабировании — Redis Streams/Kafka вместо in-memory Sinks
- **Поиск**: PostgreSQL реплика для чтения (read replica)
