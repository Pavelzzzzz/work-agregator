# VacancyScout — k3s Deployment

## Требования

- k3s (>= 1.28)
- kubectl (настроен на k3s кластер)
- Docker (для сборки образов)
- Docker socket: `/var/run/docker.sock` (k3s использует docker runtime)

## Быстрый старт

```bash
# 1. Развернуть
./k3s/deploy.sh

# 2. Проверить
kubectl get pods -n vacancy-scout
kubectl get svc -n vacancy-scout
kubectl get ingress -n vacancy-scout
```

## Архитектура

```
                  ┌─────────────────┐
                  │   Traefik LB    │
                  │  (встроенный)   │
                  └────┬──────────┬─┘
                       │          │
              :80/vacancy.local  :8080/api
                       │          │
            ┌──────────┘          └──────────┐
            ▼                                ▼
   ┌─────────────────┐            ┌─────────────────┐
   │ vacancy-scout   │            │ vacancy-scout   │
   │ -frontend (x2)  │            │ -backend  (x2)  │
   └────────┬────────┘            └────────┬────────┘
            │                              │
            │                    ┌─────────▼─────────┐
            │                    │ vacancy-scout-db   │
            │                    │ (StatefulSet x1)   │
            │                    │ 10Gi PVC           │
            │                    └─────────┬─────────┘
            │                              │
            │                    ┌─────────▼─────────┐
            │                    │ vacancy-scout-redis│
            │                    │ (x1)              │
            │                    └───────────────────┘
```

## Манифесты

| Файл | Описание |
|------|----------|
| `00-namespace.yaml` | Namespace `vacancy-scout` |
| `01-secrets.yaml` | Секреты: DB, Redis, R2DBC URL |
| `02-database.yaml` | PostgreSQL 16 (StatefulSet + PVC 10Gi) |
| `03-redis.yaml` | Redis 7 (Deployment x1) |
| `04-backend.yaml` | Backend Spring Boot (Deployment x2) |
| `05-frontend.yaml` | Frontend Nginx (Deployment x2) |
| `06-ingress.yaml` | Traefik Ingress (vacancy.local) |

## Масштабирование

```bash
# Backend
kubectl scale deployment/vacancy-scout-backend -n vacancy-scout --replicas=5

# Frontend
kubectl scale deployment/vacancy-scout-frontend -n vacancy-scout --replicas=3
```

## Обновление

```bash
# Пересобрать и обновить
./k3s/deploy.sh

# Или вручную:
docker build -t vacancy-backend:latest -f backend/Dockerfile .
docker build -t vacancy-frontend:latest -f frontend/Dockerfile .
kubectl set image deployment/vacancy-scout-backend vacancy-backend=vacancy-backend:latest -n vacancy-scout
kubectl set image deployment/vacancy-scout-frontend frontend=vacancy-frontend:latest -n vacancy-scout
```

## Логи

```bash
# Backend
kubectl logs -f deployment/vacancy-scout-backend -n vacancy-scout

# Database
kubectl logs -f vacancy-scout-db-0 -n vacancy-scout

# Ингress
kubectl logs -f deployment/traefik -n kube-system
```

## Troubleshooting

### Pod не запускается
```bash
kubectl describe pod -n vacancy-scout -l app.kubernetes.io/component=backend
kubectl get events -n vacancy-scout --sort-by='.lastTimestamp'
```

### Секреты не применяются
```bash
kubectl get secret vacancy-secrets -n vacancy-scout -o yaml
kubectl delete secret vacancy-secrets -n vacancy-scout
kubectl apply -f k3s/01-secrets.yaml
```

### Database connection refused
```bash
# Подождать инициализации БД
kubectl wait --for=condition=ready pod/vacancy-scout-db-0 -n vacancy-scout --timeout=120s
```

### Очистка
```bash
kubectl delete namespace vacancy-scout
# PVC останется! Удалить вручную:
kubectl delete pvc postgres-data-vacancy-scout-db-0 -n vacancy-scout
```
