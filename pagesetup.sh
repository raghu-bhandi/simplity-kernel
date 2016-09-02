mvn javadoc:aggregate
mkdir -p javadocs
cd javadocs
git clone -b gh-pages https://github.com/simplity/simplity.git
cd ..
cp -r target/site/apidocs javadocs/simplity 
cd javadocs/simplity
git config --global user.email "archetana@gmail.com"
git config --global user.name "Chetana"
git add .
git commit -m "gh pages javadocs"
git push origin gh-pages
