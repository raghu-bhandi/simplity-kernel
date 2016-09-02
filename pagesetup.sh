call mvn javadoc:aggregate
mkdir -p javadocs
cd javadocs
git clone -b gh-pages https://github.com/simplity/simplity.git
cd ..
xcopy target\site\apidocs javadocs\simplity /Y /i /s
cd javadocs\simplity
git add .
git commit -m "gh pages javadocs"
git push origin gh-pages
