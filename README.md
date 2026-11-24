# Deployment System

Монорепозиторий проекта Deployment System.

## Локальная разработка

Для backend нужны JDK 21 и Maven 3.6.3 или новее. Для frontend нужны Node.js 22 (от 22.12.0) и npm 10. Проверьте версии командами `mvn -v`, `node -v` и `npm -v`. Maven должен использовать JDK 21.

Из корня репозитория:

```sh
mvn -f backend/pom.xml clean package
npm --prefix frontend ci
npm --prefix frontend run build
```

Для локального запуска выполните команды в отдельных терминалах:

```sh
mvn -f backend/pom.xml spring-boot:run
npm --prefix frontend run dev
```

Spring Boot по умолчанию слушает порт 8080. Стартовая страница Deployment System доступна на `http://localhost:5173/`, если порт Vite свободен. На этапе DS-001-01 приложение запускается с отключённой автоконфигурацией источника данных. Подключение PostgreSQL и миграции Flyway предусмотрены в DS-001-02.

- `backend/` — Spring Boot приложение с базовым Java-пакетом `io.github.dvdav.deploymentsystem`.
- `frontend/` — приложение React, TypeScript, Vite и Material UI. Стартовый экран находится в `src/app/`.
- `docker/` — каталог для будущих Dockerfile.
- `docs/` — спецификации проекта.

## Спецификации

- [DS-001 — Foundation & Local Development Environment](docs/specifications/DS-001_Foundation_Technical_Spec_RU.md) — согласованное техническое задание, версия 1.1.
- [DS-006 — Deployment Planning: обзор и 6 частей](docs/specifications/DS-006_Deployment_Planning_Overview_RU.md) — архитектурные решения и декомпозиция для будущей реализации.
