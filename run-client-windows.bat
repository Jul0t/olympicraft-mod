@echo off
title Test local d'Olympicraft

echo ============================================
echo      Lancement du client de test Fabric
echo ============================================
echo.

if not exist "gradlew.bat" (
    echo ERREUR : gradlew.bat est introuvable.
    pause
    exit /b 1
)

call gradlew.bat runClient

if errorlevel 1 (
    echo.
    echo Le lancement a echoue.
    pause
    exit /b 1
)

pause
