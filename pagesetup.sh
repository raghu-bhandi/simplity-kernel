mvn javadoc:aggregate
mkdir -p javadocs
cd javadocs
git clone -b gh-pages https://github.com/simplity/simplity.git
cd ..
cp -r target/site/apidocs javadocs/simplity 
cd javadocs\simplity
git add .
git commit -m "gh pages javadocs"
git push origin gh-pages
