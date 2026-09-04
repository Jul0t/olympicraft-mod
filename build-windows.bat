@echo off
title Compilation d'Olympicraft

echo ============================================
echo        Compilation d'Olympicraft
echo ============================================
echo.

if not exist "gradlew.bat" (
    echo ERREUR : gradlew.bat est introuvable.
    echo Verifiez que ce script est place a la racine du projet.
    pause
    exit /b 1
)

call gradlew.bat clean build

if errorlevel 1 (
    echo.
    echo La compilation a echoue.
    echo Consultez les erreurs affichees ci-dessus.
    pause
    exit /b 1
)

echo.
echo ============================================
echo Compilation terminee avec succes.
echo Le mod se trouve dans build\libs\
echo ============================================
echo.

explorer "build\libs"
pause
