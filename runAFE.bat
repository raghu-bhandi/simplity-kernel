if not exist "D:\Projects\simplity" mkdir "D:\Projects\simplity"
cd /D D:\Projects\simplity
git init
git remote add -f origin http://infygit.ad.infosys.com/encore/Simplity.git
git config core.sparseCheckout true
echo example/*  1>>.git/info/sparse-checkout
echo AFE2016/*  1>>.git/info/sparse-checkout
git pull origin master
cd AFE2016
call mvn clean install
cd ..\example
call mvn clean install