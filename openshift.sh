cd app-root/repo/
mvn install
echo "Completed mvn install"
cd ~
rm -r jbossews/webapps/*
cp app-root/repo/example/target/example.war jbossews/webapps/
cd jbossews/webapps/
mv example.war ROOT.war