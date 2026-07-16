# Samlib Client

Неофициальный Android-клиент для чтения и публикации на [samlib.ru](http://samlib.ru) —
библиотеке любительской прозы и поэзии. Разбор HTML-страниц сайта (у samlib нет API),
офлайн-чтение с кешем, подписки на авторов с уведомлениями об обновлениях, TTS-озвучка,
редактирование собственных разделов/произведений.

## Возможности

- Чтение и офлайн-кеш произведений, история чтения, закладки
- Подписки на авторов + фоновая проверка обновлений (WorkManager) с уведомлениями
- Поиск, топ авторов/произведений, новые поступления
- Озвучка текста (TTS) с подсветкой читаемого слова и автопрокруткой
- Публикация и редактирование собственных произведений/разделов на samlib
- Перевод текста (ML Kit) на лету
- Импорт данных из старой версии приложения

## Стек

- Kotlin, Jetpack Compose (Material 3), Navigation Compose (type-safe routes)
- Koin — dependency injection
- Orbit MVI — `ContainerHost`/`State`/`SideEffect` во всех экранах
- Ktor (OkHttp engine) — сеть, Jsoup — разбор HTML-страниц сайта
- Room — офлайн-кеш и история, DataStore — настройки
- WorkManager — фоновая проверка подписок
- media3 — фоновое воспроизведение TTS, ML Kit — перевод

## Архитектура

```
domain/        доменные модели, интерфейсы репозиториев, usecase — чистый Kotlin/JVM
data/          реализации репозиториев, Room, сетевой клиент, HTML-парсеры
ui/            Compose-экраны + ViewModel (Orbit MVI), ресурсы
app/           composition root — Application/MainActivity/NavHost, Koin-модули, media3 TtsService
testfixtures/  общие тестовые фейки для :data/:ui/:app
```

Каждый экран — `ViewModel : ContainerHost<State, SideEffect>` с единым `State` и
`sealed interface Action` для событий из UI.

## Сборка и запуск

```bash
./gradlew assembleDebug      # debug-сборка
./gradlew testDebugUnitTest  # юнит-тесты
./gradlew ktlintCheck detekt # статический анализ
```

Для release-сборки нужен локальный `app/keystore.properties` (не хранится в репозитории,
см. `app/build.gradle.kts`).

## Статический анализ

ktlint + detekt (`config/detekt/detekt.yml`), проверка встроена в CI
(`.github/workflows/ci.yml`).

## Тесты

225+ unit-тестов: `:data` — парсеры, Room, сетевой клиент (`MockEngine`); `:ui` — ViewModel
через `orbit-test`. Общие фейки и фикстуры — в `:testfixtures`.

## Импорт данных из старой версии

`LegacyImporter` переносит данные пользователя (подписки, «Сохранённые» книги) из предыдущей
версии приложения при первом запуске новой.
