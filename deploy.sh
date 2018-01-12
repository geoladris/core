#!/usr/bin/env bash
if [ "$TRAVIS_BRANCH" = 'master' ] && [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
    openssl aes-256-cbc -K $encrypted_f06ce5567527_key -iv $encrypted_f06ce5567527_iv -in ci/signingkey.asc.enc -out ci/signingkey.asc -d
    gpg --import signingkey.asc
    mvn deploy --settings deploy-settings.xml
fi

