# java-filmorate

## 🎬 Über das Projekt
java-filmorate ist ein REST-Service (Backend) für ein soziales Netzwerk zur Filmbewertung, inspiriert von Empfehlungssystemen unter Freunden. Der Service ermöglicht es Nutzern, sich zu registrieren, Freunde hinzuzufügen, Filme zu bewerten und Likes zu vergeben.

### Link zum Datenbank-Diagramm
- https://github.com/SuleimanMusaev/java-filmorate/blob/main/db-diagram/Database_Diagram.png

### Funktionen:
- __Inhalts- und Benutzerverwaltung:__ Vollständiger CRUD-Zyklus für Benutzerprofile, Filmkataloge, Regisseurdaten und Rezensionen.
- __Soziale Interaktion:__ System für bidirektionale Freundschaftsanfragen mit Bestätigungsmechanismus und Anzeige gemeinsamer Freunde.
- __Bewertungen und Feedback:__ Like-System zur Erstellung von Popularitäts-Rankings und umfassende Rezensionsfunktionen für gesehene Filme.
- __Intelligente Empfehlungen:__ Generierung persönlicher Filmvorschläge basierend auf den Vorlieben und Bewertungen im Freundeskreis.
- __Erweiterte Suche und Filterung:__ Flexible Suche nach Filmtiteln oder Regisseuren sowie Top-Listen mit Filtern nach Genre oder Erscheinungsjahr.
- __Aktivitäts-Feed:__ Automatischer Feed der neuesten Ereignisse, um Aktivitäten von Freunden zu verfolgen (neue Likes, Rezensionen, Freundschaften).
- __Admin-Tools:__ Funktionen zum unwiderruflichen Löschen von Objekten (Benutzer oder Filme) aus dem System.
- __Universelle Schnittstelle:__ Standardisierte REST-API für eine einfache Integration mit Frontend-Anwendungen und Drittanbietern.

# java-filmorate

## 🎬 О проекте
java-filmorate — это REST-сервис (бекенд) для соцсети оценки фильмов, вдохновлённой системой «рекомендаций среди друзей». Сервис позволяет пользователям регистрироваться, добавлять друзей, оценивать фильмы и ставить лайки.

## Ссылка на диаграмму:
- https://github.com/SuleimanMusaev/java-filmorate/blob/main/db-diagram/Database_Diagram.png

### Возможности:
- __Управление контентом и пользователями:__ Полный цикл CRUD для профилей пользователей, каталога фильмов, данных о режиссёрах и отзывов.
- __Социальное взаимодействие:__ Система двусторонней дружбы с механизмом подтверждения заявок и возможностью просмотра списка общих друзей.
- __Рейтинги и обратная связь:__ Система лайков для формирования рейтинга популярности и полноценный функционал отзывов о просмотренных фильмах.
- __Интеллектуальный подбор:__ Генерация персональных рекомендаций на основе предпочтений и оценок круга друзей.
- __Расширенный поиск и фильтрация:__ Гибкий поиск фильмов по названиям и именам режиссёров, а также формирование топов популярных картин с фильтрацией по жанрам или годам выпуска.
- __Аналитика активности:__ Автоматическое формирование ленты последних событий для отслеживания действий друзей (новые лайки, отзывы, добавления в друзья).
- __Инструменты администрирования:__ Функционал для безвозвратного удаления объектов (пользователей или фильмов) из системы при необходимости.
- __Универсальный интерфейс:__ Стандартизированный REST API, обеспечивающий легкую интеграцию с фронтенд-приложениями и сторонними клиентами.


### Beispiele für API-Anfragen:
### Примеры работы с приложением:
```

Benutzer erstellen:
Создание пользователя:
{
  "login": "dolore",
  "name": "Nick Name",
  "email": "mail@mail.ru",
  "birthday": "1946-08-20"
}

Film hinzufügen:
Добавление фильма:
{
"name": "nisi eiusmod",
"description": "adipisicing",
"releaseDate": "1967-03-25",
"duration": 100
}

Filmliste abrufen:
Получить список фильмов:
GET /films

Freundesliste abrufen:
Получить список друзей:
GET /users/{{id}}/friends

Genres anzeigen:
Посмотреть жанры:
GET //genres

...und vieles mehr.
И многое другое...
```
