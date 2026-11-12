# 🚪 Puerta Ábrete Sésamo

App para Android que pone una **puerta** en tu pantalla de bloqueo. Para abrirla tocas
sobre la puerta tu **ritmo secreto** (como cuando llamas a la puerta de un amigo).

- Eliges la puerta: madera rústica, metal industrial, medieval o moderna.
- Eliges la mano que la abre desde dentro: esqueleto, cartoon amarilla, pulsera azul anime,
  monstruo verde o guante blanco. Cada una abre con su propia animación.
- Si fallas 3 veces seguidas, la puerta toca desde dentro **la mitad de tu ritmo** como pista,
  y tú solo tienes que completar el resto.
- Margen de error ajustable (por defecto ±20 %).

## Cómo instalarla desde el teléfono

1. Entra en la sección **Releases** de este repositorio (o en la pestaña **Actions** → la última
   ejecución correcta → `puerta-abrete-sesamo-apk`).
2. Descarga `puerta-abrete-sesamo.apk`.
3. Ábrelo. Android te pedirá permitir "instalar apps de origen desconocido" para tu navegador: acéptalo.
4. Si ya tenías una versión anterior y da error al instalar, desinstala la vieja primero
   (las versiones de prueba se firman con una llave distinta cada vez).

## Primeros pasos en la app

1. **Graba tu ritmo**: tócalo sobre la puerta, espera 2 segundos y repítelo igual.
2. Elige puerta, mano y margen de error.
3. Da el permiso **"Mostrar sobre otras apps"** (y notificaciones si lo pide).
4. Activa el interruptor **"Poner la puerta al bloquear el teléfono"**.
5. Pulsa **"Probar la puerta ahora"** para practicar.

## Importante

- **Deja puesto tu PIN o huella.** Android no permite a ninguna app reemplazar el bloqueo
  seguro, así que la puerta aparece por encima de él. Después de abrir la puerta, Android te
  pedirá tu PIN como siempre. Así tu teléfono sigue igual de protegido.
- Si olvidas el ritmo, toca dos veces **"Usar PIN del teléfono"** en la parte de abajo de la puerta.
- En Xiaomi, Huawei, Oppo y similares puede que tengas que activar "Mostrar en pantalla de
  bloqueo" en los permisos de la app y quitarle el ahorro de batería.

## Para desarrolladores

- Kotlin + Jetpack Compose, Android 8.0 (API 26) o superior.
- `./gradlew :core:test` ejecuta las pruebas de la lógica del ritmo.
- `./gradlew :app:assembleDebug` compila el APK.
- Detalles técnicos y estructura de carpetas: [docs/ARQUITECTURA.md](docs/ARQUITECTURA.md).
