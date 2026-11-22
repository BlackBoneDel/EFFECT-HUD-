# Registro de verificación — extracción Effect Timer (standalone)

Fecha: 2026-11-22 · Entorno: sandbox de Arena (sin JDK/Gradle ni acceso a los
maven de Fabric/Mojang; solo `github.com`, `codeload`, `api.github.com`, `npm`,
`pypi` son alcanzables).

## Qué se verificó (y cómo) dentro del sandbox

### 1. Type-check real con `javac` (OpenJDK) vía WASM
Se montó un compilador Java funcional sin red usando
`teavm-javac` (javac de OpenJDK compilado a WebAssembly, paquete npm
`@browserrun/java` → registry.npmjs.org). Todos los fuentes del mod
(8 archivos) + los 3 de test se compilaron **contra stubs de API de Minecraft
1.21.11 (mojmap), Fabric API, JOML y GLFW escritos replicando exactamente las
firmas usadas por el código del repositorio padre** (`HUD-MODS`, que sí compila
contra el MC real), lo que garantiza que toda llamada de API del código extraído
existe tal cual en 1.21.11.

Resultado: `compile()=true · errors=0 · diagnostics=0` (0 warnings) ·
93 `.class` emitidos.

### 2. Ejecución real de los tests
Los `.class` emitidos se ejecutaron sobre un JRE real (jdk4py, Java 25 de PyPI)
con un runner explícito (`testharness.TestMain`). La persistencia del config se
ejercitó con un shim GSON funcional de igual superficie API
(`toJson/fromJson/JsonParser/JsonObject/JsonParseException`), cubriendo:

```
RESULT passed=20 failed=0
  · DurationFormatter: 02:15 / 00:45 / 00:10 / 1:00:00 / ∞ / tickrate 40tps
  · LevelNames: nivel I oculto (regla original), romanos 4000-
  · ColorHex: normalización, rechazo, parseo y paleta cíclica
  · EffectTimerConfig: defaults, clamp (posX>=0, scale 0.5–2.5),
    round-trip JSON por disco, recuperación de JSON corrupto con backup .bak
  · Efecto del guardado atómico (tmp + move) verificado sobre sistema de ficheros real
```

### 3. Barridos estáticos
- `grep` de trazas de otros módulos (armor/durability/fps/cps/keystrokes/
  compass/ping/coordinates/editor/presets/modmenu/customizablehud): **sin
  referencias funcionales**; solo menciones en Javadoc que documentan qué fue
  eliminado.
- Claves de traducción usadas en código vs `en_us.json`/`es_es.json`: 100 % de
  correspondencia, cero claves huérfanas en ambos sentidos.
- `fabric.mod.json` y lang files: JSON válido; el entrypoint declarado
  (`dev.effecttimer.client.EffectTimerMod`) existe; `icon.png` presente.
- `build.gradle`: repositorio Terraformers (Mod Menu) y `modmenu_version`
  eliminados; dependencias = Minecraft + mojmap + loader + Fabric API +
  JUnit-loader.

## Qué NO puede ejecutarse aquí (y cómo comprobarlo)

`./gradlew build` necesita descargar la distribución de Gradle, el plugin Loom,
el MC jar/piston-meta y mojmap desde `services.gradle.org` /
`maven.fabricmc.net` / `*.mojang.com`, todos bloqueados por la red del sandbox.
En una máquina con red normal:

```bash
./gradlew build test
# → build/libs/effect-timer-1.0.0.jar  (y fuentes en build/libs/*-sources.jar)
```

Los tests JUnit incluidos (`src/test/java`) reproducen sobre GSON real las
mismas comprobaciones de persistencia/formato ejecutadas aquí.

## Notas de comportamiento (fieles al original, con 3 decisiones menores)

1. **Nivel I oculto** (`hideLevelOne` del original): la preview muestra
   `Velocidad II - 02:15`, `Fuerza - 00:45`, `Regeneración - 00:10`. (El
   ejemplo `Fuerza I` de la spec es incompatible con `Regeneración` sin nivel
   bajo cualquier regla única; se eligió la regla del mod original.)
2. Relleno fijo `mm:ss` (`01:30`) en lugar del `%d:%02d` del padre, según los
   ejemplos de la spec (evita saltos de ancho de texto).
3. El fade de expiración original (`computeLowDurationFade`, ±10 s con coseno)
   se conservó literal como `blinkOnExpire`.
