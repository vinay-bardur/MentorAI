@echo off
echo ========================================
echo FORCE REBUILD - CLEARING ALL CACHES
echo ========================================

echo Stopping Gradle daemon...
call gradlew --stop

echo Cleaning project...
call gradlew clean

echo Removing build directories...
if exist "app\build" rmdir /s /q "app\build"
if exist "build" rmdir /s /q "build"
if exist ".gradle" rmdir /s /q ".gradle"

echo Removing Android Studio caches...
if exist "%USERPROFILE%\.gradle\caches" rmdir /s /q "%USERPROFILE%\.gradle\caches"

echo Force rebuilding...
call gradlew build --refresh-dependencies

echo ========================================
echo REBUILD COMPLETE!
echo Now open Android Studio and:
echo 1. File -> Invalidate Caches / Restart
echo 2. Uninstall old app from device
echo 3. Run -> Run 'app'
echo ========================================
pause