mkdir javadocs
echo /javadocs/ >> .gitignore
git add .
git commit -m ".gitignore javadocs"
git push origin master
cd javadocs
git clone https://github.com/simplity/simplity.git
git checkout origin/gh-pages -b gh-pages
echo "h1. Sample Readme" > README.md
git add .
git commit -m "gh pages README added"
git push origin gh-pages
