# Скрипт для исправления ошибки Flyway
# Выполните эти команды в терминале:

# 1. Перейдите в директорию проекта
cd "E:\1_MyProjects\AltaProject (AltaTrack)\noteapp-ai-integration"

# 2. Выполните repair команду Flyway
.\gradlew flywayRepair

# 3. Если repair не работает, попробуйте альтернативные команды:
# .\gradlew flywayClean flywayMigrate

# 4. Или запустите приложение с отключенной валидацией:
# .\gradlew bootRun -Dspring.flyway.validate-on-migrate=false

echo "Выберите один из вариантов выше"
