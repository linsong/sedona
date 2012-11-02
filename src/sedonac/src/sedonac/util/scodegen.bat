@echo off
rem
rem scodegen.bat
rem
rem   This batch compiles "scode const.txt" into SCode.java
rem
rem   Revision
rem     4 Mar 07   Brian      Original
rem

set rt="%java_home%\jre\lib\rt.jar"
jikes +E -classpath "%rt%" %sedona_home%adm\SCodeGen.java
java -cp %sedona_home%adm SCodeGen "java" "%sedona_home%adm\scode.txt" "%sedona_home%adm\scode.java" "%sedona_home%src\sedonac\src\sedonac\scode\SCode.java"
java -cp %sedona_home%adm SCodeGen "h" "%sedona_home%adm\scode.txt" "%sedona_home%adm\scode.h" "%sedona_home%src\vm\scode.h"
del %sedona_home%adm\SCodeGen*.class