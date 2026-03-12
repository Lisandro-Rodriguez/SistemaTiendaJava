@echo off
echo =============================================
echo  Empaquetando Sistema Tienda v1.0.0
echo =============================================

cd /d "%~dp0"

echo.
echo [1/3] Compilando y generando JAR...
call mvn clean package -q
if errorlevel 1 (
    echo ERROR: Fallo la compilacion.
    pause
    exit /b 1
)

echo [2/3] Generando instalador .exe con jpackage...
if exist "instalador" rmdir /s /q "instalador"

jpackage ^
  --input target ^
  --name "Sistema Tienda" ^
  --main-jar SistemaTienda-jar-with-dependencies.jar ^
  --main-class com.tienda.Main ^
  --type exe ^
  --dest instalador ^
  --app-version 1.0.0 ^
  --vendor "Lisandro Rodriguez" ^
  --description "Sistema POS para kioscos y almacenes" ^
  --win-shortcut ^
  --win-menu ^
  --win-dir-chooser ^
  --win-per-user-install ^
  --icon src\main\resources\icono.ico

if errorlevel 1 (
    echo.
    echo NOTA: jpackage requiere WiX Toolset instalado para generar .exe
    echo Descargalo desde: https://wixtoolset.org/
    echo.
    echo Alternativa: usa el JAR directamente:
    echo   java -jar target\SistemaTienda-jar-with-dependencies.jar
    pause
    exit /b 1
)

echo [3/3] Listo!
echo.
echo Instalador generado en: instalador\
echo JAR ejecutable en:      target\SistemaTienda-jar-with-dependencies.jar
echo.
pause
