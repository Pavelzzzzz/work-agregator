#!/usr/bin/env bash
# Миграция тома PostgreSQL 15 -> 16 для docker-compose.
#
# PostgreSQL не умеет открыть data directory старшей мажорной версии, поэтому
# одной замены образа в docker-compose.yml недостаточно: существующий том
# нужно выгрузить и восстановить. Скрипт делает две копии:
#   1) физическую копию старого тома (для отката),
#   2) логический дамп pg_dump (для переноса данных).
#
# Запуск из корня репозитория:
#   ./scripts/migrate-postgres-15-to-16.sh
#   ./scripts/migrate-postgres-15-to-16.sh -y   # без подтверждения
#
# Переопределения через окружение:
#   PG_VOLUME, COMPOSE_PROJECT_NAME, POSTGRES_USER, POSTGRES_DB,
#   POSTGRES_PASSWORD, BACKUP_DIR

set -Eeuo pipefail

die() { echo "ERROR: $*" >&2; exit 1; }
info() { echo "==> $*"; }

AUTO_CONFIRM=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    -y|--yes) AUTO_CONFIRM=1 ;;
    -h|--help)
      sed -n '2,16p' "$0"
      exit 0
      ;;
    *) die "Unknown argument: $1" ;;
  esac
  shift
done

command -v docker >/dev/null 2>&1 || die "docker not found"
docker compose version >/dev/null 2>&1 || die "Требуется Docker Compose v2 (docker compose)"

PROJECT="${COMPOSE_PROJECT_NAME:-$(basename "$PWD" | tr '[:upper:]' '[:lower:]' | tr -cd '[:alnum:]')}"
VOLUME="${PG_VOLUME:-${PROJECT}_postgres_data}"
BACKUP_VOLUME="${VOLUME}_pg15_backup"
DB_USER="${POSTGRES_USER:-vacancy_scout}"
DB_NAME="${POSTGRES_DB:-vacancy_scout}"
DB_PASSWORD="${POSTGRES_PASSWORD:-vacancy_scout}"
BACKUP_DIR="${BACKUP_DIR:-./backups}"
STAMP="$(date +%Y%m%d-%H%M%S)"
DUMP_FILE="${BACKUP_DIR}/${DB_NAME}-pg15-${STAMP}.sql"
TMP_CONTAINER="vs-pg15-migrate-$$"

info "Project:       $PROJECT"
info "Data volume:   $VOLUME"
info "Backup volume: $BACKUP_VOLUME"
info "Dump file:     $DUMP_FILE"

docker volume inspect "$VOLUME" >/dev/null 2>&1 || die "Том '$VOLUME' не найден. Мигрировать нечего."

SRC_VERSION="$(docker run --rm -v "$VOLUME":/data:ro alpine:3.20 cat /data/PG_VERSION 2>/dev/null | tr -d '[:space:]' || true)"
[[ "$SRC_VERSION" == "15" ]] || die "В томе '$VOLUME' ожидался PostgreSQL 15, найден '${SRC_VERSION:-unknown}'. Миграция уже выполнена или том не является каталогом данных PostgreSQL — прерываю."

if [[ "$AUTO_CONFIRM" -eq 0 ]]; then
  echo
  echo "Будет остановлен стек, том '$VOLUME' заменён новым, данные перенесены в PostgreSQL 16."
  echo "Старые данные останутся в томе '$BACKUP_VOLUME' и в дампе '$DUMP_FILE'."
  read -r -p "Продолжить? [y/N] " REPLY
  [[ "$REPLY" =~ ^[Yy]$ ]] || { echo "Отменено."; exit 0; }
fi

mkdir -p "$BACKUP_DIR"

info "Остановка стека (тома сохраняются)"
docker compose down

info "Физическая копия старого тома -> $BACKUP_VOLUME"
docker volume rm -f "$BACKUP_VOLUME" >/dev/null 2>&1 || true
docker volume create "$BACKUP_VOLUME" >/dev/null
docker run --rm -v "$VOLUME":/from:ro -v "$BACKUP_VOLUME":/to alpine:3.20 \
  sh -c 'tar -C /from -cf - . | tar -C /to -xf -'

info "Логический дамп PostgreSQL 15"
docker run -d --name "$TMP_CONTAINER" \
  -e "POSTGRES_USER=$DB_USER" \
  -e "POSTGRES_PASSWORD=$DB_PASSWORD" \
  -e "POSTGRES_DB=$DB_NAME" \
  -v "$VOLUME":/var/lib/postgresql/data \
  postgres:15 >/dev/null
cleanup_tmp() { docker rm -f "$TMP_CONTAINER" >/dev/null 2>&1 || true; }
trap cleanup_tmp EXIT

READY=0
for _ in $(seq 1 60); do
  if docker exec "$TMP_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -tAc 'SELECT 1' >/dev/null 2>&1; then READY=1; break; fi
  sleep 1
done
[[ "$READY" -eq 1 ]] || die "Временный PostgreSQL 15 неготов (БД $DB_NAME не создана?)"

docker exec "$TMP_CONTAINER" pg_dump -U "$DB_USER" -d "$DB_NAME" --no-owner --no-privileges -f /tmp/vs-migrate.sql
docker cp "$TMP_CONTAINER":/tmp/vs-migrate.sql "$DUMP_FILE"
grep -q 'CREATE TABLE' "$DUMP_FILE" || die "Дамп пустой или повреждён: $DUMP_FILE"
cleanup_tmp
trap - EXIT

info "Замена тома и восстановление дампа в PostgreSQL 16"
docker volume rm "$VOLUME" >/dev/null
# Том создаст docker compose (с его метками), поэтому вручную его не создаём.
docker compose up -d db

READY=0
for _ in $(seq 1 60); do
  if docker compose exec -T db psql -U "$DB_USER" -d "$DB_NAME" -tAc 'SELECT 1' >/dev/null 2>&1; then READY=1; break; fi
  sleep 1
done
[[ "$READY" -eq 1 ]] || die "PostgreSQL 16 неготов (БД $DB_NAME не создана?)"

docker compose exec -T db psql -v ON_ERROR_STOP=1 -U "$DB_USER" -d "$DB_NAME" < "$DUMP_FILE" >/dev/null

CHANGELOG="$(docker compose exec -T db psql -tAc 'SELECT count(*) FROM databasechangelog' -U "$DB_USER" -d "$DB_NAME" | tr -d '[:space:]')"
info "Восстановлено записей Liquibase: $CHANGELOG"

info "Подъём полного стека"
docker compose up -d

cat <<EOF

Готово. PostgreSQL 16 работает с прежними данными.

Бэкапы:
  дамп:       $DUMP_FILE
  старый том: $BACKUP_VOLUME (физическая копия, для отката)

Откат (если вернуть postgres:15 в docker-compose.yml):
  docker compose down
  docker volume rm $VOLUME
  docker volume create $VOLUME
  docker run --rm -v $BACKUP_VOLUME:/from -v $VOLUME:/to alpine:3.20 \\
    sh -c 'tar -C /from -cf - . | tar -C /to -xf -'
  docker compose up -d
EOF
