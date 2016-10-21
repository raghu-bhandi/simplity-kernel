if not exist "D:\Projects\simplity" mkdir "D:\Projects\simplity"
cd /D D:\Projects\simplity
git pull origin master
cd AFE2016
call mvn clean install
cd ..\example
call mvn clean install