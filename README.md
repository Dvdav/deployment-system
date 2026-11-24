# Deployment System

Монорепозиторий проекта Deployment System.

## Локальная разработка

Для backend нужны JDK 21, Maven 3.6.3 или новее и запущенный Docker Engine/Desktop с Compose V2 или новее. Для frontend нужны Node.js 22 (от 22.12.0) и npm 10. Проверьте версии командами `mvn -v`, `docker version`, `docker compose version`, `node -v` и `npm -v`. Maven должен использовать JDK 21.

Из корня репозитория:

```sh
mvn -f backend/pom.xml clean verify
npm --prefix frontend ci
npm --prefix frontend run build
```

### PostgreSQL и запуск backend

Из корня репозитория создайте `.env` командой `cp -n .env.example .env` и задайте в нём уникальный локальный пароль `POSTGRES_PASSWORD`. Файл используется и Docker Compose, и shell, поэтому значения со специальными символами заключайте в одинарные кавычки. Проверьте, что Git игнорирует файл, командой `git check-ignore .env`.

```sh
docker compose -f compose.dev.yaml up -d --wait postgres
docker compose -f compose.dev.yaml ps
(
  set -a
  . ./.env
  set +a
  mvn -f backend/pom.xml spring-boot:run
)
```

PostgreSQL доступна только на `127.0.0.1`. Если порт 5432 занят, задайте свободный `POSTGRES_PORT` в `.env`. Backend использует тот же порт, имя БД и пользователя. При необходимости JDBC URL можно задать через `SPRING_DATASOURCE_URL` без учётных данных в URL. Spring Boot по умолчанию слушает порт 8080. Для frontend выполните в отдельном терминале `npm --prefix frontend run dev`. Стартовая страница доступна на `http://localhost:5173/`, если порт Vite свободен.

### Проверка миграции

После запуска backend проверьте схему и историю. На чистой базе для версии `1` должна появиться одна успешная запись. После повторного запуска backend запись должна остаться той же.

```sh
docker compose -f compose.dev.yaml exec -T postgres sh -c '
  psql -X -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
    -c "\\d system_info" \
    -c "SELECT installed_rank, version, description, success, installed_on FROM flyway_schema_history ORDER BY installed_rank"
'
```

`mvn -f backend/pom.xml clean verify` запускает интеграционные тесты на PostgreSQL 17.12 через Testcontainers. Тесты проверяют применение V1, запись и чтение через Spring JDBC, повторный запуск приложения и отказ запуска при ошибочной миграции. Проверено на Docker Desktop 4.100.1, Docker Engine 29.9.3 и Compose 5.7.1 на macOS arm64. Для скачивания образов PostgreSQL и Ryuk нужен доступ к Docker Hub. Если Testcontainers не находит Docker, проверьте доступ к Docker Engine и адрес сокета командой `docker context inspect`.

### Остановка и сброс локальной БД

Остановите backend и выполните `docker compose -f compose.dev.yaml down`. Команда сохраняет именованный volume. Следующий `up -d --wait postgres` использует тот же volume.

Для проверки на чистой локальной БД откройте `psql` командой ниже и выполните `INSERT INTO system_info (id, value) VALUES (1, 'persistence-check');`. После `down`, повторного `up` и запуска backend запрос `SELECT value FROM system_info WHERE id = 1;` должен вернуть `persistence-check`, а история Flyway должна сохранить исходную запись версии 1.

```sh
docker compose -f compose.dev.yaml exec postgres sh -c '
  exec psql -X -U "$POSTGRES_USER" -d "$POSTGRES_DB"
'
```

Если изменился пароль, смените его в существующей БД через интерактивную команду `\password` в `psql` и обновите `.env`. Изменение одного лишь `.env` действует при первой инициализации volume. При сбоях запуска проверьте `docker compose -f compose.dev.yaml ps` и `docker compose -f compose.dev.yaml logs postgres`, затем сверьте порт, имя базы и пользователя с `.env`.

Чтобы намеренно удалить все данные **локальной** базы, выполните `docker compose -f compose.dev.yaml down -v`. При следующем запуске PostgreSQL создаст пустую базу, а Flyway снова применит миграции.

### Структура репозитория

- `backend/` — Spring Boot приложение с базовым Java-пакетом `io.github.dvdav.deploymentsystem`.
- `frontend/` — приложение React, TypeScript, Vite и Material UI. Стартовый экран находится в `src/app/`.
- `docker/` — каталог для будущих Dockerfile.
- `docs/` — спецификации проекта.

## Спецификации

- [DS-001 — Foundation & Local Development Environment](docs/specifications/DS-001_Foundation_Technical_Spec_RU.md) — согласованное техническое задание, версия 1.1.
- [DS-006 — Deployment Planning: обзор и 6 частей](docs/specifications/DS-006_Deployment_Planning_Overview_RU.md) — архитектурные решения и декомпозиция для будущей реализации.
