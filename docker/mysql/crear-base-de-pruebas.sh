#!/usr/bin/env bash
# Crea la base sigit_testing que usan las pruebas automatizadas (SIGIT-DMS-A3.2-AD, 2.1.6).

mysql --user=root --password="$MYSQL_ROOT_PASSWORD" <<-EOSQL
    CREATE DATABASE IF NOT EXISTS sigit_testing;
EOSQL

if [ -n "$MYSQL_USER" ]; then
mysql --user=root --password="$MYSQL_ROOT_PASSWORD" <<-EOSQL
    GRANT ALL PRIVILEGES ON \`sigit_testing\`.* TO '$MYSQL_USER'@'%';
EOSQL
fi
