# jenkins.ci - Пайплайн для сборки дострибутивов

## Запуск окружения
1. Запустить окружение: `docker-compose up`
2. Создать credentials:
   1. `SSH_KEY_GITHUB`
   2. `NEXUS_CREDENTIALS`
3. Настроить доверие для ssh хоста: <br>
   Manage Jenkins -> Security -> Git Host Key Verification Configuration. <br> 
   Выставить на 'Accept first connection'