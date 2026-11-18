#!/bin/sh
# Bootstrap Gradle wrapper script for ROCKET VISUALS.
# Downloads gradle-wrapper.jar on first run, then behaves like the standard ./gradlew.
APP_HOME=$(cd "$(dirname "$0")" && pwd -P)
JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$JAR" ]; then
  echo "gradle-wrapper.jar not found - downloading..."
  URL="https://raw.githubusercontent.com/gradle/gradle/v9.5.1/gradle/wrapper/gradle-wrapper.jar"
  if command -v curl >/dev/null 2>&1; then curl -fL "$URL" -o "$JAR"
  elif command -v wget >/dev/null 2>&1; then wget -O "$JAR" "$URL"
  else echo "Need curl or wget, or run: gradle wrapper --gradle-version 9.5.1"; exit 1; fi
  [ -s "$JAR" ] || { rm -f "$JAR"; echo "Download failed. Run: gradle wrapper --gradle-version 9.5.1"; exit 1; }
fi
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then JAVACMD="$JAVA_HOME/bin/java"; else JAVACMD=java; fi
exec "$JAVACMD" -Dorg.gradle.appname=gradlew -classpath "$JAR" org.gradle.wrapper.GradleWrapperMain "$@"
