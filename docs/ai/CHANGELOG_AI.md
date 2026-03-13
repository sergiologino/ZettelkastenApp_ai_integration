# Changelog (AI memory)

Краткая история изменений (1–3 строки на запись). Логи и stacktrace не включать.

---

- **Инициализация памяти проекта**: созданы `docs/ai/` и файлы PROJECT_OVERVIEW.md, ARCHITECTURE.md, CURRENT_STATE.md, DECISIONS.md, CONVENTIONS.md, CHANGELOG_AI.md. Описаны цель проекта, текущая архитектура backend, фактическое состояние и контракт работы с памятью.
- **Пополнение каталога нейросетей (V016)**: добавлено 19 моделей — OpenAI GPT-5.4/5.4 Pro, GPT-4.1/Mini/Nano, o3/o3-pro/o4-mini; Anthropic Claude Opus 4.6/Sonnet 4.6/Opus 4.5/Sonnet 4.5/Haiku 4.5; Google Gemini 2.5 Pro/Flash; xAI Grok 3/3 Mini; DeepSeek R1; Mistral Large 3. Провайдеры google/xai роутятся через OpenAiClient (ADR-3).
