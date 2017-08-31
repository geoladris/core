#!/bin/bash

pomVersion=`grep -oPm1 "(?<=<version>)[^<]+" pom.xml | grep -oE '[0-9]+\.[0-9]+\.[0-9]+'`
packageVersion=`jq -r .version package.json`

if [[ $packageVersion =~ $pomVersion-alpha\.[0-9]+$ ]]; then
	v="${packageVersion%.*}.$((${packageVersion##*.}+1))"
else
	v="`echo $pomVersion | grep -oE '[0-9]+\.[0-9]+\.[0-9]+'`-alpha.0"
fi

yarn publish --new-version $v --no-git-tag-version --tag dev

if [ "`git status -s package.json | wc -l`" -gt 0 ]; then
	git add package.json
	git commit -m "Bump package.json version to $v"
fi
