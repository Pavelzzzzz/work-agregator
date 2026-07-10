# VacancyScout — Kubernetes Dev окружение

## Состав

| Файл | Описание |
|------|----------|
| `namespace.yaml` | Пространство имён `vacancy-dev` |
| `secret.yaml` | Секреты (пароль БД, R2DBC URL) |
| `backend-deployment.yaml` | Backend (1 replica, порт 8080) |
| `backend-service.yaml` | ClusterIP сервис для backend |
| `frontend-deployment.yaml` | Frontend (1 replica, порт 80) |
| `frontend-service.yaml` | ClusterIP сервис для frontend |
| `ingress.yaml` | Ingress для внешнего доступа (host: vacancy.dev.local) |

## Установка

```bash
kubectl apply -f k8s/dev/namespace.yaml
kubectl apply -f k8s/dev/secret.yaml
kubectl apply -f k8s/dev/backend-deployment.yaml
kubectl apply -f k8s/dev/backend-service.yaml
kubectl apply -f k8s/dev/frontend-deployment.yaml
kubectl apply -f k8s/dev/frontend-service.yaml
kubectl apply -f k8s/dev/ingress.yaml
```

## Проверка

```bash
kubectl get all -n vacancy-dev
kubectl logs -f -n vacancy-dev deployment/vacancy-backend
kubectl logs -f -n vacancy-dev deployment/vacancy-frontend
```

## Переменные окружения

| Переменная | Источник |
|------------|----------|
| `DB_PASSWORD` | `secret.yaml` → `vacancy-secrets.db-password` |
| `R2DBC_URL` | `secret.yaml` → `vacancy-secrets.r2dbc-url` |
| `LM_STUDIO_BASE_URL` | Hardcoded: `http://lm-studio:1234/v1` |
