# Скрипт для исправления ошибки Flyway checksum mismatch

# Способ 1: Использовать repair команду Flyway
# Выполните одну из команд ниже:

# Для Gradle:
./gradlew flywayRepair

# Для Maven:
# mvn flyway:repair

# Для командной строки Flyway:
# flyway repair

# Способ 2: Ручное исправление в базе данных
# Выполните SQL команду в базе данных:

# UPDATE flyway_schema_history 
# SET checksum = -1622964891 
# WHERE version = '004';

# Способ 3: Удалить запись о миграции (если она не была применена)
# DELETE FROM flyway_schema_history WHERE version = '004';

echo "Выберите один из способов выше для исправления ошибки Flyway"
