# restaurant-review-api

REST API для управления ресторанами и отзывами на Spring Boot 3 / Java 17.  
Тестовое задание: демонстрирует одновременное применение **Spring Data JPA** и **Spring JDBC** в одном проекте.

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-CRUD-6DB33F?style=flat-square&logo=spring&logoColor=white)
![Spring JDBC](https://img.shields.io/badge/Spring%20JDBC-JdbcTemplate-6DB33F?style=flat-square&logo=spring&logoColor=white)
![H2](https://img.shields.io/badge/H2-in--memory-4479A1?style=flat-square&logo=h2&logoColor=white)
![Lombok](https://img.shields.io/badge/Lombok-boilerplate-A50034?style=flat-square)
![JUnit5](https://img.shields.io/badge/JUnit%205-Mockito-25A162?style=flat-square&logo=junit5&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-build-C71A36?style=flat-square&logo=apachemaven&logoColor=white)
---

## Почему JPA и JDBC вместе

В проекте оба подхода применяются там, где они естественны:

| Задача | Инструмент | Обоснование |
|---|---|---|
| CRUD для `City`, `Restaurant`, `Vote` | Spring Data JPA | Декларативный CRUD, управление связями, каскады — без ручного SQL |
| Выборка ресторанов города по рейтингу | JdbcTemplate | `ORDER BY` в сыром SQL нагляднее, чем JPA `Sort`; хорошо масштабируется |
| Пересчёт `averageRating` после каждого голоса | JdbcTemplate | Один `UPDATE … (SELECT AVG …)` — не тащит всю коллекцию в память |

Ключевой момент совместной работы — `VoteServiceImpl`: JPA сохраняет/удаляет запись, вызывает `flush()`, после чего JDBC пересчитывает агрегат **в той же транзакции**.

---

## Быстрый старт

**Требования:** Java 17, Maven 3.8+

```bash
# Сборка + тесты
mvn clean install

# Только тесты
mvn test

# Запуск
mvn spring-boot:run
```

Приложение запускается на `http://localhost:8080`.  
При старте автоматически загружаются демо-данные: **3 города · 5 ресторанов · 12 отзывов**.

---

## H2 Console

`http://localhost:8080/h2-console`

| Поле | Значение |
|---|---|
| JDBC URL | `jdbc:h2:mem:restaurantdb` |
| User Name | `sa` |
| Password | *(пусто)* |

---

## Эндпоинты

### Города `/api/cities`

```bash
GET    /api/cities          # список всех городов
GET    /api/cities/{id}     # один город
POST   /api/cities          # создать
PUT    /api/cities/{id}     # обновить
DELETE /api/cities/{id}     # удалить
```

### Рестораны `/api/restaurants`

```bash
GET    /api/restaurants                          # все рестораны (краткая форма)
GET    /api/restaurants/{id}                     # ресторан + список отзывов
POST   /api/restaurants                          # создать
PUT    /api/restaurants/{id}                     # обновить
DELETE /api/restaurants/{id}                     # удалить
GET    /api/restaurants/by-city/{cityName}       # по городу, сортировка по рейтингу (JDBC)
                                                 # ?sort=rating_desc (по умолчанию) | rating_asc
                                                 # любое другое значение → 400 Bad Request
POST   /api/restaurants/{id}/votes               # добавить отзыв → пересчёт рейтинга
GET    /api/restaurants/{id}/votes               # отзывы ресторана
```

### Голоса `/api/votes`

```bash
DELETE /api/votes/{id}      # удалить отзыв → пересчёт рейтинга
```

---

## Примеры запросов

```bash
# Создать город
curl -X POST http://localhost:8080/api/cities \
  -H "Content-Type: application/json" \
  -d '{"name": "Novosibirsk"}'

# Создать ресторан (cityId из /api/cities)
curl -X POST http://localhost:8080/api/restaurants \
  -H "Content-Type: application/json" \
  -d '{"name": "Buro", "cityId": 1}'

# Добавить отзыв — rating: 0–5
curl -X POST http://localhost:8080/api/restaurants/1/votes \
  -H "Content-Type: application/json" \
  -d '{"rating": 5, "comment": "Отличная кухня!"}'

# Рестораны Москвы — по убыванию рейтинга (JDBC-запрос)
curl "http://localhost:8080/api/restaurants/by-city/Moscow?sort=rating_desc"

# По возрастанию рейтинга
curl "http://localhost:8080/api/restaurants/by-city/Moscow?sort=rating_asc"

# Удалить отзыв (averageRating ресторана пересчитывается автоматически)
curl -X DELETE http://localhost:8080/api/votes/3
```

---

## Обработка ошибок

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
| 400 | Невалидные данные (рейтинг вне 0–5, пустое имя, недопустимое значение `sort`) |
| 404 | Ресторан / город / голос не найден |
| 409 | Дубликат имени города; попытка удалить город, у которого есть рестораны |
| 500 | Непредвиденная ошибка (стектрейс только в логах) |

---

## Тесты

```bash
mvn test   # 21 тест, все зелёные
```

| Класс | Тип | Что проверяет |
|---|---|---|
| `RestaurantServiceTest` | Unit (Mockito) | CRUD, делегирование в JDBC DAO, исключения |
| `VoteServiceTest` | Unit (Mockito) | Добавление/удаление голоса, пересчёт рейтинга после каждого вызова |
| `RestaurantControllerIntegrationTest` | Интеграционный (MockMvc) | Полный HTTP-цикл: рейтинг после голосов, сортировка, 409/404/400 |

---

## Структура пакетов

```
com.aston.restaurantreview
├── config        DataInitializer — демо-данные при старте
├── controller    CityController, RestaurantController, VoteController
├── dao           RestaurantJdbcDao  ← JDBC (by-city + recalculate AVG)
├── dto
│   ├── request   CityRequest, RestaurantRequest, VoteRequest
│   └── response  CityResponse, RestaurantResponse, RestaurantSummaryResponse, VoteResponse
├── entity        City, Restaurant, Vote
├── exception     EntityNotFoundException, DuplicateCityException, CityHasRestaurantsException,
│                 ErrorResponse, GlobalExceptionHandler (@RestControllerAdvice)
├── repository    CityRepository, RestaurantRepository, VoteRepository  ← JPA
└── service
    ├── CityService, RestaurantService, VoteService  (интерфейсы)
    └── impl  (реализации)
```
