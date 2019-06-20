#!/bin/bash

# move to project root
cd $(dirname "$0")
cd ../

echo '>> 書き込み禁止ファイルをread-onlyに変更中...'
chmod -w src/main/java/enshud/Main.java
chmod -w src/main/java/enshud/casl/CaslSimulator.java
chmod -w src/test/java/enshud/s0/trial/TrialTest.java
chmod -w src/test/java/enshud/s1/lexer/LexerTest.java
chmod -w src/test/java/enshud/s2/parser/ParserTest.java
chmod -w src/test/java/enshud/s3/checker/CheckerTest.java
chmod -w src/test/java/enshud/s4/compiler/CompilerTest.java
chmod -w build.gradle
chmod -w settings.gradle
chmod -w data/pas/*
chmod -w data/ts/*
chmod -w data/ans/*
chmod -w data/cas/*

echo '>> gitのpre-commit hookを追加中...'
cp misc/pre-commit .git/hooks/
chmod +x .git/hooks/pre-commit

echo '>> gitのデフォルト設定を更新中...'
uid=$(git config -l | grep '09B[0-9]*' -o | head -n 1)
git config --local user.name $uid
git config --local user.email $uid@localhost
git config --local core.ignorecase false

echo '>> gradleの初期化中...'
if [[ "${http_proxy}" ]]; then
  # exp環境固有の設定
  echo 'systemProp.https.proxyHost=cacheserv.ics.es.osaka-u.ac.jp' > gradle.properties
  echo 'systemProp.https.proxyPort=3128' >> gradle.properties
  echo 'org.gradle.java.home=/usr/lib/jvm/java-8-openjdk-amd64' >> gradle.properties
  gradle eclipse
else
  ./gradlew eclipse
fi

echo '   演習Dプロジェクトは正しく初期化されました'
