@echo off
set home=%~p0%\..
set lib=%home%\lib
set cp=%lib%\sedona.jar;
java -classpath %cp% sedona.util.sedonadev.Upload %1 %2 %3 %4 %5 %6 %7 %8 %9

