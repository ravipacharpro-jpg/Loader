@rem Gradle startup script for Windows
@rem
@if "%DEBUG%"=="" @echo off
setlocal
set JAVA_HOME=C:\Users\sandi\Downloads\android-build-env\jdk\jdk-17.0.11+9
set ANDROID_HOME=C:\Users\sandi\Downloads\android-build-env\sdk
set ANDROID_SDK_ROOT=C:\Users\sandi\Downloads\android-build-env\sdk
set APP_BASE_NAME=gradlew
set APP_HOME=%~dp0
set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar
"%JAVA_HOME%\bin\java.exe" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
