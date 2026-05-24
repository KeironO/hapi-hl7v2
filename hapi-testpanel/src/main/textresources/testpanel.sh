#!/bin/sh

exec java \
  -Dsun.java2d.uiScale.enabled=true \
  -Dswing.aatext=true \
  -Dawt.useSystemAAFontSettings=on \
  -jar hapi-testpanel-jar-with-dependencies.jar "$@"
