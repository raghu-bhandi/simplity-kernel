mvn javadoc:aggregate
mkdir -p javadocs
cd javadocs
git clone -b gh-pages https://github.com/simplity/simplity.git
cd ..
cp -r target/site/apidocs javadocs/simplity 
cd javadocs/simplity

git config user.name "Travis CI"
git config user.email "archetana@gmail.com"


git add .
git commit -m "gh pages javadocs"

ENCRYPTED_KEY_VAR="encrypted_${ENCRYPTION_LABEL}_key"
ENCRYPTED_IV_VAR="encrypted_${ENCRYPTION_LABEL}_iv"
ENCRYPTED_KEY=${!ENCRYPTED_KEY_VAR}
ENCRYPTED_IV=${!ENCRYPTED_IV_VAR}
openssl aes-256-cbc -K $ENCRYPTED_KEY -iv $ENCRYPTED_IV -in deploy_key.enc -out deploy_key -d
chmod 600 deploy_key
eval `ssh-agent -s`
ssh-add deploy_key

SOURCE_BRANCH="master"
TARGET_BRANCH="gh-pages"
REPO=`git config remote.origin.url`
SSH_REPO=${REPO/https:\/\/github.com\//git@github.com:}

# Now that we're all set up, we can push.
git push $SSH_REPO $TARGET_BRANCH
