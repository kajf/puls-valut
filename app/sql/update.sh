#!/bin/bash

APP_HOME=/home/public/git/prokopovi-ch-mobile/project
DB_NAME=androidovich.db

echo APP_HOME=$APP_HOME
echo DB_NAME=$DB_NAME

echo "Updating metadata..."
sqlite3 -bail -echo $APP_HOME/auxiliary/$DB_NAME < $APP_HOME/sql/meta.sql

echo "Updating places BY..."
sqlite3 -bail -echo $APP_HOME/auxiliary/$DB_NAME < $APP_HOME/sql/places.sql

#echo "Updating places UA..."
#sqlite3 -bail -echo $APP_HOME/auxiliary/$DB_NAME < $APP_HOME/sql/places_ua.sql
