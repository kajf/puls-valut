SET SQLITE_HOME=c:\opt
SET APP_HOME=D:\_ws_a\puls-valut
SET DB_NAME=androidovich.db

%SQLITE_HOME%\sqlite3.exe -bail %APP_HOME%\auxiliary\%DB_NAME% < %APP_HOME%\app\sql\meta.sql
%SQLITE_HOME%\sqlite3.exe -bail %APP_HOME%\auxiliary\%DB_NAME% < %APP_HOME%\app\sql\places.sql
rem %SQLITE_HOME%\sqlite3.exe -bail %APP_HOME%\auxiliary\%DB_NAME% < %APP_HOME%\app\sql\places_ua.sql
