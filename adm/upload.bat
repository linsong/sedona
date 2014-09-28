@echo off
set uphome=%~p0%\..
set uplib=%uphome%\lib
set upcp=%uplib%\sedona.jar;
java -classpath %upcp% sedona.util.sedonadev.Upload %1 %2 %3 %4 %5 %6 %7 %8 %9

