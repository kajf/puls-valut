SET SQLITE_HOME=d:\programs
SET APP_HOME=d:\ws_adt\prokopovi-ch-mobile\project
SET DB_NAME=androidovich.db

%SQLITE_HOME%\sqlite3.exe -bail %APP_HOME%\auxiliary\%DB_NAME% < %APP_HOME%\sql\meta.sql
%SQLITE_HOME%\sqlite3.exe -bail %APP_HOME%\auxiliary\%DB_NAME% < %APP_HOME%\sql\places.sql
%SQLITE_HOME%\sqlite3.exe -bail %APP_HOME%\auxiliary\%DB_NAME% < %APP_HOME%\sql\places_ua.sql
