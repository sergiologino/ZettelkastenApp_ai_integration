# История изменений (AI-память)

Краткие записи (1–3 строки) по итогам задач; без логов и отладочного шума.

- **Инициализация памяти проекта**: созданы `docs/ai/` и файлы PROJECT_OVERVIEW.md, ARCHITECTURE.md, CURRENT_STATE.md, DECISIONS.md, CONVENTIONS.md, CHANGELOG_AI.md. Описаны цель проекта, текущая архитектура backend, фактическое состояние и контракт работы с памятью.
- **Пополнение каталога нейросетей (V016)**: добавлено 19 моделей — OpenAI GPT-5.4/5.4 Pro, GPT-4.1/Mini/Nano, o3/o3-pro/o4-mini; Anthropic Claude Opus 4.6/Sonnet 4.6/Opus 4.5/Sonnet 4.5/Haiku 4.5; Google Gemini 2.5 Pro/Flash; xAI Grok 3/3 Mini; DeepSeek R1; Mistral Large 3. Провайдеры google/xai роутятся через OpenAiClient (ADR-3).
- **Dockerfile: замена базового образа**: `openjdk:17-jdk-slim` → `eclipse-temurin:17-jdk-jammy` (Oracle-образ удалён из Docker Hub).
- **2026-04-09**: Исчерпывающее руководство для внешних интеграций **`EXTERNAL_SERVICES_INTEGRATION.md`** (контракт JWT + `X-API-Key`, эндпоинты, примеры).
- **2026-04-09**: Добавлен тип **`speech_synthesis`**: OpenAI TTS (`OpenAiClient`, `/v1/audio/speech`, голос в `payload.voice`) и Yandex **SpeechKit TTS** (`YandexGptClient`, REST `tts:synthesize`, голос/язык в `payload`). Обновлены `AiRequestDTO`, `EXTERNAL_SERVICES_INTEGRATION.md` (в т.ч. пояснение по Yandex: сервис SpeechKit TTS, не отдельное имя «модели» как у YandexGPT).
- **2026-04-12**: Миграция **V017**: сиды **`openai-tts`** и **`yandex-speechkit-tts`** (`network_type = speech_synthesis`). В админке (`NetworksManager`) в списке типов сети добавлен пункт **Speech synthesis (TTS)** — раньше тип был только в API, без строк в UI и без записей в каталоге.
- **2026-04-26**: Добавлен **Social posting API**: `POST /api/social/posts` под `X-API-Key` для Telegram/Facebook/X, транзитные credentials передаются в запросе и не сохраняются. Результаты пишутся в `request_logs` как `social_post:<platform>`, добавлены `GET /api/admin/social/stats`, блок статистики на фронте и автотесты сервиса.
- **2026-05-28**: Grok Imagine (virtual try-on): временные URL `imgen.x.ai` скачиваются в noteapp и отдаются клиенту как `imageBase64` (+ `data[].base64`); исходный URL сохраняется в `sourceImageUrl`. Остальные image-провайдеры без изменений.
- **2026-05-28**: Season hit video: `SeasonHitVideoClient` (xAI `/v1/videos/generations` + poll), `RemoteVideoDownloader`, провайдер `season_hit_video`, миграция V021 `wibestyle-season-video`.
