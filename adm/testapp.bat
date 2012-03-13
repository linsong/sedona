@echo off
echo ##
echo ## make sys
echo ##
call sedonac %sedona_home%src\sys
if  errorlevel 1 goto failure

echo ##
echo ## make x86-test.scode
echo ##
call sedonac %sedona_home%scode\x86-test.xml
if  errorlevel 1 goto failure

echo ##
echo ## test vm
echo ##
call svm %sedona_home%scode\x86-test.scode -test
if  errorlevel 1 goto failure

echo ##
echo ## MAKEALL SUCCESS
echo ##
goto end

:failure
echo ##
echo **
echo ## MAKEALL FAILED
echo ##

:end