# restaurant-review-api

REST API для управления ресторанами и отзывами (оценками).  
Тестовое задание на позицию Java-разработчика.

---

## Стек

| Компонент | Версия |
|---|---|
| Java | 17 |
| Spring Boot | 3.2.5 |
| Maven | 3.8+ |
| H2 | in-memory |
| Lombok | via Spring Boot BOM |

---

## Архитектурное решение: JPA vs JDBC

В проекте **оба** подхода используются реально, а не для галочки:

| Что | Подход | Почему |
|---|---|---|
| CRUD для `City`, `Restaurant`, `Vote` | **Spring Data JPA** | Стандартный CRUD без дублирования SQL; JPA управляет связями и каскадами |
| Список ресторанов по городу, сортировка по рейтингу | **JdbcTemplate** | Нативный `ORDER BY` выразительнее, чем JPA Sort; хорошо показывает возможности JDBC |
| Пересчёт `averageRating` после каждого голоса | **JdbcTemplate** | Один `UPDATE … (SELECT AVG …)` не тащит все голоса в память; демонстрирует JPA+JDBC в одной транзакции |

Ключевая точка совместного использования — `VoteServiceImpl`: JPA сохраняет/удаляет голос, вызывает `flush()`, затем JDBC пересчитывает среднее в той же транзакции.

---

## Сборка и запуск

```bash
# Сборка (запускает тесты)
mvn clean install

# Только тесты
mvn test

# Запуск
mvn spring-boot:run
```

Приложение стартует на `http://localhost:8080`.

При запуске автоматически загружаются демо-данные: **3 города**, **5 ресторанов**, **12 отзывов**.

---

## H2 Console

URL: `http://localhost:8080/h2-console`

| Поле | Значение |
|---|---|
| JDBC URL | `jdbc:h2:mem:restaurantdb` |
| User Name | `sa` |
| Password | *(пусто)* |

---

## API

### Города

```bash
# Список всех городов
curl http://localhost:8080/api/cities

# Создать город
curl -X POST http://localhost:8080/api/cities \
  -H "Content-Type: application/json" \
  -d '{"name": "Novosibirsk"}'

# Обновить город
curl -X PUT http://localhost:8080/api/cities/1 \
  -H "Content-Type: application/json" \
  -d '{"name": "New Name"}'

# Удалить город
curl -X DELETE http://localhost:8080/api/cities/1
```

### Рестораны

```bash
# Список всех ресторанов
curl http://localhost:8080/api/restaurants

# Ресторан с отзывами
curl http://localhost:8080/api/restaurants/1

# Создать ресторан (cityId из /api/cities)
curl -X POST http://localhost:8080/api/restaurants \
  -H "Content-Type: application/json" \
  -d '{"name": "My New Restaurant", "cityId": 1}'

# Рестораны города, отсортированные по рейтингу ↓ (JDBC)
curl "http://localhost:8080/api/restaurants/by-city/Moscow?sort=rating_desc"

# Удалить ресторан
curl -X DELETE http://localhost:8080/api/restaurants/1
```

### Отзывы

```bash
# Добавить отзыв (rating: 0-5)
curl -X POST http://localhost:8080/api/restaurants/1/votes \
  -H "Content-Type: application/json" \
  -d '{"rating": 5, "comment": "Absolutely fantastic!"}'

# Список отзывов ресторана
curl http://localhost:8080/api/restaurants/1/votes

# Удалить отзыв (averageRating пересчитывается автоматически)
curl -X DELETE http://localhost:8080/api/votes/3
```

---

## Формат ошибок

Все ошибки возвращаются в едином формате:

```json
{
  "timestamp": "2024-05-01T12:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Restaurant with id=99 not found",
  "path": "/api/restaurants/99"
}
```

| Код | Ситуация |
|---|---|
| 400 | Невалидные данные (рейтинг вне 0-5, пустое имя) |
| 404 | Ресторан / город / голос не найден |
| 409 | Дубликат имени города |
| 500 | Непредвиденная ошибка (стектрейс только в логах) |

---

## Тесты

```bash
mvn test
```

- **`RestaurantServiceTest`** — unit-тесты сервисного слоя с Mockito
- **`VoteServiceTest`** — проверка логики пересчёта рейтинга (ключевой бизнес-кейс)
- **`RestaurantControllerIntegrationTest`** — интеграционные тесты через `MockMvc`:
  - добавление голоса → пересчёт `averageRating`
  - удаление голоса → пересчёт `averageRating`
  - сортировка по городу (JDBC-путь)
  - дубликат города → 409
  - несуществующий ресторан → 404
  - невалидный рейтинг → 400

---

## Структура пакетов

```
com.aston.restaurantreview
├── entity        City, Restaurant, Vote
├── repository    JPA-репозитории (CityRepository, RestaurantRepository, VoteRepository)
├── dao           RestaurantJdbcDao  ← JDBC
├── dto
│   ├── request   CityRequest, RestaurantRequest, VoteRequest
│   └── response  CityResponse, RestaurantResponse, RestaurantSummaryResponse, VoteResponse
├── service       интерфейсы CityService, RestaurantService, VoteService
│   └── impl      реализации
├── controller    CityController, RestaurantController, VoteController
├── exception     EntityNotFoundException, DuplicateCityException,
│                 ErrorResponse, GlobalExceptionHandler
└── config        DataInitializer (CommandLineRunner с демо-данными)
```
