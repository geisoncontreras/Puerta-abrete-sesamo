# Arquitectura de "Puerta Ábrete Sésamo"

## 1. Decisión de plataforma: Android nativo (Kotlin + Jetpack Compose)

Mostrar algo encima de la pantalla de bloqueo necesita APIs del sistema (`setShowWhenLocked`,
`KeyguardManager`, servicios en primer plano, el evento de "pantalla apagada"). En Flutter o
React Native habría que escribir todo eso en código nativo igualmente, así que usamos
Kotlin directo y **Jetpack Compose** para dibujar la puerta y animarla a 60 fps.

iPhone (iOS) no permite a ninguna app ponerse sobre la pantalla de bloqueo, así que la app es
solo para Android.

## 2. Estrategia para la pantalla de bloqueo (segura y fluida)

**Lo honesto primero:** Android no deja que una app normal *sustituya* el bloqueo seguro del
sistema (PIN, patrón, huella). Las apps de "lock screen" que lo intentan desactivando el PIN
dejan el teléfono menos protegido. Por eso la app funciona como **una capa encima** del bloqueo:

```
Pantalla se apaga
      │  (LockService escucha ACTION_SCREEN_OFF)
      ▼
LockActivity se abre "por debajo" de la pantalla apagada
  · setShowWhenLocked(true)  → se dibuja ENCIMA del bloqueo del sistema
  · pantalla completa, sin barras, "atrás" desactivado
      ▼
Usuario enciende la pantalla → ve la PUERTA
      │  toca el ritmo
      ▼
Ritmo correcto → animación de apertura + mano
      ▼
KeyguardManager.requestDismissKeyguard()
  · Si hay PIN/huella → Android lo pide (seguridad real intacta)
  · Si no hay bloqueo seguro → el teléfono queda abierto
```

Piezas y por qué:

| Pieza | Para qué |
|---|---|
| `LockService` (servicio en primer plano, tipo `specialUse`) | `ACTION_SCREEN_OFF` solo se recibe con un receptor registrado en código, así que hace falta un proceso vivo. La notificación fija es obligatoria en Android. |
| Permiso "Mostrar sobre otras apps" (`SYSTEM_ALERT_WINDOW`) | Desde Android 10 no se pueden abrir pantallas desde segundo plano; este permiso es la excepción oficial. |
| `LockActivity` con `showWhenLocked`, `singleTask`, `excludeFromRecents` | Se muestra sobre el bloqueo, solo existe una instancia y no aparece en "recientes". |
| `requestDismissKeyguard` | Pasa el control al bloqueo del sistema tras abrir la puerta. Nunca se desactiva el PIN. |
| `BootReceiver` | Reactiva la puerta al reiniciar el teléfono o actualizar la app. |
| Botón "Usar PIN del teléfono" (doble toque) | Salida de emergencia si olvidas el ritmo; solo aparece si hay PIN, porque así saltarse la puerta sigue exigiendo el PIN. También da acceso a la llamada de emergencia del sistema. |
| `allowBackup=false` + almacenamiento privado | El ritmo no sale del teléfono ni viaja en copias de seguridad. |

Límites conocidos (explicados en el README):
- El botón **Inicio** sale de la puerta: Android no permite bloquearlo a una app normal. Con PIN puesto no importa, porque debajo está el bloqueo del sistema.
- Algunas marcas (Xiaomi, Huawei, Oppo…) piden un permiso extra "Mostrar en pantalla de bloqueo" y matan servicios para ahorrar batería.
- Para fluidez, todo se dibuja con `Canvas` de Compose (sin imágenes pesadas) y la animación usa `graphicsLayer` (rotación 3D por GPU).

## 3. Estructura de carpetas

```
puerta-abrete-sesamo/
├── core/                                   ← lógica pura en Kotlin (sin Android, con tests)
│   └── src/main/kotlin/com/abretesesamo/core/
│       ├── rhythm/
│       │   ├── Rhythm.kt                   ← el ritmo = lista de deltas (ms); mitad para la pista
│       │   ├── RhythmMatcher.kt            ← comparación con tolerancia ±X %
│       │   └── RhythmEnroller.kt           ← grabación en 2 tomas y promedio
│       └── unlock/
│           └── UnlockSession.kt            ← intentos, 3 fallos → pista, completar tras la pista
│   └── src/test/…/RhythmTest.kt            ← 12 pruebas automáticas
├── app/                                    ← la app Android
│   └── src/main/java/com/abretesesamo/app/
│       ├── audio/KnockPlayer.kt            ← golpes sintetizados, reproducción exacta de la pista
│       ├── data/SettingsRepository.kt      ← ritmo, tolerancia, puerta, mano, activado
│       ├── lock/
│       │   ├── LockService.kt              ← vigila "pantalla apagada"
│       │   ├── LockActivity.kt             ← la puerta encima del bloqueo
│       │   ├── LockViewModel.kt            ← conecta la lógica con la pantalla
│       │   └── BootReceiver.kt
│       ├── model/                          ← DoorStyle, HandStyle
│       └── ui/
│           ├── door/
│           │   ├── DoorPainter.kt          ← las 4 puertas dibujadas por código
│           │   ├── DoorScene.kt            ← detector de toques + animación 3D + mano
│           │   └── DoorLockScreen.kt       ← mensajes y salida de emergencia
│           ├── hand/
│           │   ├── HandPainter.kt          ← las 5 manos (antebrazo → dedos)
│           │   └── HandMotion.kt           ← la animación propia de cada mano
│           ├── settings/                   ← ajustes y grabación del ritmo
│           └── theme/
├── .github/workflows/build-apk.yml         ← compila el APK en la nube
└── docs/ARQUITECTURA.md
```

`core` no depende de Android a propósito: la parte delicada (tiempos, tolerancia, fallos) se
puede probar en segundos sin teléfono.

## 4. Componentes clave

### 4.1 Detector de toques y validación del ritmo
- `DoorScene` usa `pointerInput { awaitEachGesture { awaitFirstDown() } }`: cada dedo que baja
  es un toque y se usa la **marca de tiempo del propio evento táctil** (`uptimeMillis`), que es
  más precisa que leer el reloj después.
- `Rhythm.fromTimestamps` convierte marcas de tiempo en **deltas** (ms entre toques).
- `RhythmMatcher` acepta cada delta si `|tocado − original| ≤ max(original × tolerancia, 60 ms)`.
  El mínimo de 60 ms evita que los intervalos muy cortos sean imposibles (20 % de 150 ms son solo 30 ms).
- El intento se evalúa al llegar al número de toques del ritmo, o al dejar de tocar
  `2 × intervalo más largo` (entre 1,2 y 4 s). Un toque suelto no cuenta como fallo.

### 4.2 Fallos y pista (50 % del ritmo)
- `UnlockSession` cuenta fallos seguidos. En el 3.º (y 6.º, 9.º…) el fallo trae `secret.firstHalf()`:
  los primeros ⌈N/2⌉ toques.
- `KnockPlayer` genera **una sola pista de audio** con todos los golpes colocados en su
  milisegundo exacto (más preciso que lanzar un sonido por golpe) y mide con `AudioTimestamp`
  cuándo sonó de verdad, descontando la latencia del altavoz.
- Tras la pista, esos golpes cuentan como el inicio del ritmo: el usuario **solo completa el resto**.
  También vale tocarlo entero.

### 4.3 Animación de la puerta y la mano
- La hoja gira sobre su bisagra izquierda con `graphicsLayer { transformOrigin = (0, 0.5); rotationY; cameraDistance }`.
- Guion común: la puerta se entreabre → la mano aparece desde el fondo (crece y se acerca) →
  empuja → la puerta se abre del todo → el interior se ilumina.
- Cada mano tiene su `HandMotion`: el esqueleto tiembla, la amarilla rebota (resorte), la del
  brazalete azul llega rápida con aura de energía, la del monstruo forcejea y abre de golpe con
  sacudida de pantalla, y el guante blanco empuja suave y saluda.
- Las manos son diseños originales inspirados en estilos (cartoon, anime, monstruo, guante clásico),
  no personajes con derechos de autor, para poder publicar la app sin problemas.
