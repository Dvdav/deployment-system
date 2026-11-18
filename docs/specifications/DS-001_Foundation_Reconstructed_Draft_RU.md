# DS-001 — Foundation & Local Development Environment

**Статус:** восстановленный проект для сверки с исходным согласованным ТЗ  
**Источник:** протокол согласования DS-001 (9 архитектурных решений и модель приёмки)  
**Важное ограничение:** это не исходный документ `DS-001_Foundation_Technical_Spec_RU.md`, который ранее описывался как 424-строчное ТЗ. Он отсутствовал в доступных файлах при создании этой копии. Детали ниже отражают явно обсуждённые требования; технические детали, не утверждавшиеся отдельно, отмечены как предложения.

## 1. Цель и границы

Подготовить работающую техническую основу Deployment System: frontend в браузере, Spring Boot REST API, PostgreSQL с миграциями и два воспроизводимых режима запуска. На этом этапе **нет бизнес-функций Nexus, Components, Environments, Releases и Deployments**. Не создавать фиктивные интеграции.

Критерий результата: чистый запуск, фактическая проверка backend и БД в UI, автоматические тесты, Jenkins CI и сохранение данных БД при перезапуске.

## 2. Утверждённые архитектурные решения

| № | Решение |
|---|---|
| 1 | Один Git-репозиторий (Monorepo), отдельные backend и frontend |
| 2 | Оба режима запуска: Hybrid Development и Full Docker |
| 3 | Java 21 / Spring Boot, модульный монолит, package-by-feature |
| 4 | React + TypeScript + Vite, feature-based frontend |
| 5 | Material UI (MUI) как UI-библиотека |
| 6 | DS-001: переменные окружения, безопасная конфигурация; DS-002: шифрование внешних токенов |
| 7 | JUnit 5, Spring Boot Test, Testcontainers PostgreSQL, Vitest + React Testing Library, Playwright |
| 8 | Базовый Jenkins CI: сборки и тесты, без блокирующих security gates |
| 9 | Jenkins как платформа CI |
| 10 | Поэтапная приёмка каждой задачи + финальная интеграционная проверка |

Дополнительно согласованы PostgreSQL, Spring JDBC, Flyway, Maven и Docker Compose. Temporal отложен.

## 3. Структура репозитория

```text
deployment-system/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/java/...
│       ├── main/resources/db/migration/
│       └── test/
├── frontend/
│   ├── package.json
│   └── src/
│       ├── app/
│       ├── features/system/
│       └── shared/
├── docker/
├── docs/
│   ├── specifications/
│   └── architecture/
├── compose.yaml
├── .env.example
├── .gitignore
├── Jenkinsfile
└── README.md
```

Backend организовать по business feature, а не по глобальным слоям. В DS-001 нужна только базовая функциональность `system`. Будущие пакеты `nexus`, `component`, `environment` не наполнять заглушками. Frontend: `app` (layout/navigation), `features/system` (System Status), `shared` (только реально применяемые компоненты).

## 4. Подзадачи и критерии приёмки

### DS-001-01 — Monorepo & Bootstrap

**Разработать:** структуру backend/frontend, Maven на Java 21, React/TS/Vite, MUI, минимальные build scripts, `.gitignore`, README с предпосылками и командами сборки. Зафиксировать версии зависимостей, отказаться от неиспользуемых библиотек. Не писать функциональность Nexus.

**Приёмка:** обе части проекта отдельно собираются; README позволяет воспроизвести сборку; отсутствуют случайно закоммиченные credentials, `node_modules`, `target` и build artifacts.

### DS-001-02 — PostgreSQL & Flyway

**Разработать:** контейнер PostgreSQL, конфигурацию подключения backend из переменных окружения, Spring JDBC, версионированную миграцию `V1` (минимальная миграция без преждевременных бизнес-таблиц), автоматическое применение миграций при запуске.

**Приёмка:** на чистом volume создаётся `flyway_schema_history`, миграция применена; повторный запуск не повторяет применённую миграцию; ошибка миграции не игнорируется; база не доступна из внешней сети по умолчанию.

### DS-001-03 — Backend Health API

**Разработать:** технический API состояния. Разделить liveness (запущен процесс приложения) и readiness (включая реальную проверку PostgreSQL). Проверка БД выполняет `SELECT 1`. Обрабатывать ошибки контролируемо, без stack trace или credentials в ответе.

**Согласованный функциональный контракт:**

| Условие | Результат |
|---|---|
| Health/readiness, PostgreSQL работает | HTTP 200; `status: UP` |
| Health/readiness, PostgreSQL недоступен | HTTP 503; `status: DOWN` |
| Неизвестный маршрут | HTTP 404 |
| Backend недоступен по сети | UI различает сетевую ошибку и ответ HTTP 503 |

**Примечание:** первоначально использовался `GET /api/health`, позднее согласована необходимость отдельно различать liveness/readiness. **Конкретные конечные URL отдельного liveness/readiness пока требуют окончательной фиксации** — не объявлять их согласованными без подтверждения. Можно сохранить `GET /api/health` как readiness-compatible endpoint.

### DS-001-04 — Frontend & MUI

**Разработать:** React+MUI shell с боковой навигацией и страницей System Status. Показывать отдельно фактическую доступность API и PostgreSQL, состояние загрузки и ошибку сети. Не выдавать локальные флаги или mock-значения за реальное состояние. Не готовые модули Nexus/Components/Environments отображать как отключённые.

**Приёмка:** при доступном backend UI показывает реальный статус; при отключённой БД сообщает о проблеме readiness; при отключённом backend показывает `Backend unreachable`, а не `Database DOWN`.

### DS-001-05 — Docker & Development Modes

**Разработать:**
- **Hybrid:** PostgreSQL в Docker; backend и frontend на машине разработчика, с отладкой и hot reload.
- **Full Docker:** PostgreSQL, backend, frontend в контейнерах, старт через `docker compose up --build -d`.

Конфигурация из переменных окружения, `.env.example` без значений секретов. PostgreSQL хранит данные на persistent volume. Локальные порты настраиваемые. README описывает оба сценария и команды проверки. Настройки не должны требовать внесения секретов в Dockerfile.

**Приёмка:** оба режима работают на чистой машине с заявленными prerequisites; после перезапуска данные БД сохраняются.

### DS-001-06 — Automated Testing

**Разработать:**
- JUnit 5: unit-тесты backend;
- Spring Boot Test + Testcontainers: интеграционная проверка с **настоящим PostgreSQL**, включая Flyway;
- Vitest + React Testing Library: рендеринг UI для UP/DOWN/Backend unreachable;
- Playwright: E2E от браузера через реальный Spring Boot API до реальной PostgreSQL **без mock API или H2**.

**Приёмка:** воспроизводимые команды запуска и результаты тестов; E2E подтверждает полный путь React → Spring Boot → PostgreSQL. Зафиксировать prerequisites Docker для Testcontainers и браузерные зависимости Playwright.

### DS-001-07 — Jenkins CI

**Разработать:** `Jenkinsfile` с этапами backend build/test, frontend type-check/test/build, интеграционного тестирования и Playwright E2E. Сохранять логи и тестовые отчёты. Запуск при Pull Request через доступную Jenkins-интеграцию с Git-хостингом.

**Приёмка:** pipeline запускается на подходящем Jenkins agent, корректно падает при ошибках сборки или тестов, публикует результаты. Jenkins agent должен иметь необходимый доступ к контейнерному runtime для Testcontainers и окружение для Playwright.

**Важно:** в DS-001 **не добавлять обязательные security gates**. Security-first остаётся правилом: минимальные зависимости, закреплённые версии, запрет секретов в Git/логах. SBOM, dependency scanning, container scanning и license checks запланировать до первого корпоративного развёртывания.

## 5. Конфигурация и секреты

Системные настройки читаются из environment variables. Пример конфигурации в `.env.example` не должен содержать реальные credentials. Настоящий `.env` игнорируется Git. Пароли не помещаются в исходники, Dockerfile, HTTP-ответы и логи. В DS-001 **нет** таблиц и интерфейса управления токенами Nexus; шифрование AES-256-GCM и хранение внешних токенов — область DS-002. Корпоративный Vault/KMS также отложен.

## 6. Проверки приёмки всего DS-001

1. Чистый запуск с пустыми Docker volumes.
2. Применение Flyway V1 и сохранение истории миграций.
3. Ответ 200/UP при доступной БД.
4. Ответ 503/DOWN при недоступной БД, отличимый от сетевого отказа backend.
5. Frontend отображает реальное состояние, не mock.
6. Перезапуск всех сервисов без потери persistent database volume.
7. Нет секретов в Git, логах и API.
8. Backend unit/integration, frontend unit и Playwright E2E проходят на Jenkins agent.
9. Каждый из семи шагов отдельно продемонстрирован и принят заказчиком.
10. Финальная сквозная проверка выполнена после седьмого шага.

## 7. Явно не входит в DS-001

Не реализовывать Nexus API, Component Registry, серверы/окружения, Jenkins/Ansible как внешние execution engines, Temporal, ServiceNow/Jira, Release Management, Deployment Execution, SSO/JWT или Vault/KMS. Jenkins в DS-001 — **только CI для самого проекта**.

## 8. Процесс передачи разработчику

Разработчик получает это ТЗ и выполняет задачи строго по очереди: DS-001-01 → DS-001-07. После каждой задачи предоставляет перечень изменений, доказательство прохождения тестов и демонстрацию. Следующий шаг — после приёмки предыдущего. Изменения согласованных архитектурных решений оформлять как поправки к ТЗ, не вводить молча.

---

**Проверка перед превращением в утверждённую версию:** сопоставить восстановленный документ с исходным `DS-001_Foundation_Technical_Spec_RU.md`; отдельно уточнить URL liveness/readiness и расхождения, если обнаружатся. Не подменять этот файл исходным без сверки.
