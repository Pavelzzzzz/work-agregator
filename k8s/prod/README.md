# VacancyScout — Kubernetes Prod окружение

## Состав

| Файл | Описание |
|------|----------|
| `namespace.yaml` | Пространство имён `vacancy-prod` |
| `backend-deployment.yaml` | Backend (3 replicas, порт 8080, ресурсы не указаны) |
| `backend-service.yaml` | ClusterIP сервис для backend |
| `frontend-deployment.yaml` | Frontend (2 replicas, порт 80, resources: 256–512Mi RAM, 0.25–0.5 CPU) |
| `frontend-service.yaml` | LoadBalancer сервис для frontend |
| `ingress.yaml` | Ingress (host: vacancy.prod.local) |

## Установка

```bash
kubectl apply -f k8s/prod/namespace.yaml
kubectl apply -f k8s/prod/backend-deployment.yaml
kubectl apply -f k8s/prod/backend-service.yaml
kubectl apply -f k8s/prod/frontend-deployment.yaml
kubectl apply -f k8s/prod/frontend-service.yaml
kubectl apply -f k8s/prod/ingress.yaml
```

## Особенности

- **3 реплики** backend для отказоустойчивости
- **2 реплики** frontend с ресурсными лимитами
- **LoadBalancer** для frontend — внешний доступ
- **Ingress** для маршрутизации (требует nginx-ingress-controller)
- Секреты должны быть созданы отдельно (см. `k8s/dev/secret.yaml` как шаблон)

## Проверка

```bash
kubectl get all -n vacancy-prod
kubectl get ingress -n vacancy-prod
```

## Масштабирование

```yaml
# HPA для backend
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: vacancy-backend-hpa
  namespace: vacancy-prod
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: vacancy-backend
  minReplicas: 3
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
```
