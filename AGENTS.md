# VacancyScout

Стек: Java 21, Spring Boot 3.3 WebFlux, R2DBC PostgreSQL 16, Redis,
Liquibase; Vue 3 + TypeScript + Vite + Pinia + Tailwind.
CI: GitHub Actions. Деплой: Docker Compose (dev), Kubernetes (prod).

Команды разработки:
- ./gradlew :backend:test
- ./gradlew :backend:integrationTest   # Testcontainers
- npm --prefix frontend test
- npm --prefix frontend run lint
- npm --prefix frontend run build

Перед коммитом: прогнать тесты и lint.
LSP в OpenCode V2 не запускается — проверяйте код этими командами.
AI-часть (LM Studio) в бэкенде пока не реализована, только упомянута в README.
