# VacancyScout Frontend — Vue 3 + TypeScript + Vite

## Технологии

- **Vue 3** (Composition API, `<script setup>`)
- **TypeScript** — типизированный код
- **Vite** — быстрая сборка и HMR
- **Pinia** — управление состоянием
- **Vue Router** — маршрутизация
- **Axios** — HTTP клиент
- **Tailwind CSS** — утилитарные стили

## Структура

```
frontend/
├── src/
│   ├── api/              # REST API клиенты
│   │   ├── vacancyApi.ts
│   │   ├── companyApi.ts
│   │   ├── resumeApi.ts
│   │   ├── aiApi.ts
│   │   └── applicationApi.ts
│   ├── components/
│   │   ├── common/       # Header, Sidebar, StatusBadge, VacancyCard, Pagination
│   │   ├── dashboard/    # Dashboard, StatsPanel
│   │   ├── vacancies/    # VacancyList, VacancyFilters, VacancyDetail
│   │   ├── resume/       # ResumeUpload, ResumePreview
│   │   ├── ai/           # CoverLetterEditor, CoverLetterPreview
│   │   └── applications/ # ApplicationList, ApplicationDetail
│   ├── stores/           # Pinia stores
│   │   ├── vacancyStore.ts
│   │   ├── companyStore.ts
│   │   ├── resumeStore.ts
│   │   ├── aiStore.ts
│   │   └── applicationStore.ts
│   ├── composables/      # Reusable composables
│   │   ├── useVacancies.ts
│   │   ├── useResume.ts
│   │   ├── useAi.ts
│   │   ├── useApplications.ts
│   │   └── useSearch.ts
│   ├── router/
│   │   └── index.ts
│   ├── types/            # TypeScript types
│   │   ├── vacancy.ts
│   │   ├── company.ts
│   │   ├── resume.ts
│   │   ├── coverLetter.ts
│   │   └── application.ts
│   ├── main.ts
│   ├── App.vue
│   └── styles.css
├── package.json
├── vite.config.ts
├── tsconfig.json
├── tailwind.config.js
└── index.html
```

## Установка и запуск

```bash
cd frontend
npm ci
npm run dev
```

Приложение будет доступно на `http://localhost:5173`.
API proxy настроен на `http://localhost:8080`.

## Сборка для production

```bash
npm run build
# Результат в dist/
```

## API Integration

Все API вызовы проходят через клиенты в `src/api/`. Базовый URL настроен через Vite proxy.

### Пример использования

```typescript
import { useVacancyStore } from '@/stores/vacancyStore'

const store = useVacancyStore()
await store.fetchVacancies({ query: 'Java', language: 'ru' })
```

## Планируемые компоненты

- [ ] VacancyList с пагинацией и фильтрами
- [ ] SSE подписка на обновления вакансий
- [ ] AI редактор сопроводительных писем
- [ ] Загрузка резюме (PDF)
- [ ] Dashboard с графиками
- [ ] Аутентификация
