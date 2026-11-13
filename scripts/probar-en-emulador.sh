#!/usr/bin/env bash
# Instala el APK en el emulador, abre la app, recorre los ajustes y comprueba que no se cierra.
set -u
PKG=com.abretesesamo.app
mkdir -p prueba
adb logcat -c
adb install -r app/build/outputs/apk/debug/app-debug.apk | tee prueba/instalacion.txt
grep -q Success prueba/instalacion.txt || { echo "No se pudo instalar"; exit 1; }

adb shell am start -W -n $PKG/.ui.settings.MainActivity
sleep 8
adb exec-out screencap -p > prueba/1-abierta.png

# Desliza por los ajustes (puertas y manos) para cargar todas las imágenes
for i in 1 2 3; do adb shell input swipe 540 1800 540 600 400; sleep 2; done
adb exec-out screencap -p > prueba/2-ajustes.png
for i in 1 2 3 4; do adb shell input swipe 900 1200 150 1200 300; sleep 1; done
sleep 2
adb exec-out screencap -p > prueba/3-puertas.png

adb logcat -d > prueba/registro.txt
if ! adb shell pidof $PKG > /dev/null; then
  echo "La app se cerró:"; grep -A40 "FATAL EXCEPTION" prueba/registro.txt; exit 1
fi
if grep -q "FATAL EXCEPTION" prueba/registro.txt; then
  echo "Hubo un error grave:"; grep -A40 "FATAL EXCEPTION" prueba/registro.txt; exit 1
fi
echo "La app se instaló, se abrió y sigue funcionando."
