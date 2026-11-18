@echo off
rem Bootstrap Gradle wrapper script for ROCKET VISUALS (Windows).
set APP_HOME=%~dp0
set JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar
if not exist "%JAR%" (
  echo gradle-wrapper.jar not found - downloading...
  powershell -NoProfile -Command "[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -UseBasicParsing 'https://raw.githubusercontent.com/gradle/gradle/v9.5.1/gradle/wrapper/gradle-wrapper.jar' -OutFile '%JAR%'"
  if not exist "%JAR%" ( echo Download failed. Run: gradle wrapper --gradle-version 9.5.1 & exit /b 1 )
)
if defined JAVA_HOME ( set JAVACMD=%JAVA_HOME%\bin\java.exe ) else ( set JAVACMD=java.exe )
"%JAVACMD%" -Dorg.gradle.appname=gradlew -classpath "%JAR%" org.gradle.wrapper.GradleWrapperMain %*
