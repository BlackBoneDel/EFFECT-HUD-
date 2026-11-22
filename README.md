# Effect Timer — Standalone Potion/Status Effects HUD

Extracción quirúrgica del módulo **"Effect Timer" (Effects HUD)** de
[Hud Utils (HUD-MODS)](https://github.com/BlackBoneDel/HUD-MODS) convertida en un
mod **independiente y ultra ligero** para Fabric. Sin rastro alguno de los demás
módulos del proyecto padre: no existe una sola referencia a Armor Status, FPS,
CPS, Keystrokes, Coordenadas, Ping, Compass, editor visual, presets o estilos
PotionHUD.

## Versiones (idénticas a las del repositorio fuente)

| Componente        | Versión                |
|-------------------|------------------------|
| Minecraft         | `1.21.11` (mojmap)     |
| Mod Loader        | Fabric (`fabric-loader >= 0.20.1`) |
| Fabric API        | `0.141.6+1.21.11`      |
| Java              | `21`                   |
| Loom              | `1.17.21` (plugin `net.fabricmc.fabric-loom-remap`) |
| Gradle            | `9.8.2` (wrapper incluido) |

## Estructura del proyecto

```
src/client/java/dev/effecttimer/client/
├── EffectTimerMod.java              # ClientModInitializer: config, keybind (O), hook HUD
├── config/
│   ├── EffectTimerConfig.java       # JSON/GSON con SOLO los 10 parámetros del Effect Timer
│   ├── Orientation.java             # VERTICAL | HORIZONTAL
│   └── ColorHex.java                # Validación #RRGGBB + paleta (reducción de ColorHexUtils)
├── format/
│   ├── DurationFormatter.java       # mm:ss con relleno y "∞" (heredado del original)
│   └── LevelNames.java              # Números romanos, nivel I oculto (RomanNumerals + hideLevelOne)
├── hud/
│   └── EffectTimerHudRenderer.java  # Dibujo del HUD (EffectsHudRenderer + LiveDataCollector aislados)
└── screen/
    └── EffectTimerConfigScreen.java # GUI nativa + MODO DE PRUEBA en vivo
src/main/resources/
├── fabric.mod.json
└── assets/effecttimer/ (icon.png, lang/en_us.json, lang/es_es.json)
src/test/java/dev/effecttimer/       # Tests de formato, colores y persistencia
```

## Configuración (`config/effecttimer.json`)

Única superficie de guardado/lectura, según la spec:

| Campo            | Tipo                     | Default    | Descripción                                   |
|------------------|--------------------------|------------|-----------------------------------------------|
| `enabled`        | boolean                  | `true`     | Activar/desactivar el HUD de efectos          |
| `posX` / `posY`  | int (px)                 | `4` / `4`  | Posición en pantalla (recortada a los bordes) |
| `scale`          | float 0.50–2.50          | `1.0`      | Escala del HUD (paso 0.05)                    |
| `orientation`    | enum                     | `VERTICAL` | Renderizado vertical u horizontal             |
| `showIcons`      | boolean                  | `true`     | Ícono real del efecto (atlas `mob_effect`)    |
| `showDuration`   | boolean                  | `true`     | Tiempo restante `01:30`                       |
| `showName`       | boolean                  | `true`     | Nombre + nivel en romanos (`Velocidad II`)    |
| `textColor`      | hex `#RRGGBB`            | `#FFFFFF`  | Color personalizado del texto                 |
| `blinkOnExpire`  | boolean                  | `true`     | Parpadeo con < 10 s (fórmula de fade original)|

Un JSON corrupto se copia a `effecttimer.json.corrupt-<ts>.bak` y se restauran
los valores por defecto (mecanismo probado del `ConfigManager` padre, con
escritura atómica).

## Uso

- **Abrir ajustes**: tecla `O` (recategorizable en Controles → Effect Timer) o,
  sin ella, editando `config/effecttimer.json`.
- **Modo de Prueba (Fase 4, MUST-HAVE)**: dentro de la pantalla de ajustes, el
  botón **"Modo de Prueba: ON/OFF"** dibuja en vivo las pociones ficticias
  `Velocidad II - 02:15`, `Fuerza - 00:45` y `Regeneración - 00:10` (contando
  hacia atrás y en bucle, para calibrar también el parpadeo). Al **arrastrar los
  sliders de Pos X/Y o Escala** la vista previa se fuerza igualmente. Al cerrar
  la GUI o apagar el toggle, las pociones ficticias desaparecen y el HUD vuelve
  a leer `client.player.getActiveEffects()`.
- **Sin duplicados**: el HUD propio se engancha con
  `HudElementRegistry.attachElementAfter(VanillaHudElements.STATUS_EFFECTS, ...)`
  (pipeline oficial de Fabric, **sin Mixins**). El indicador vanilla queda
  intacto, igual que en el mod padre cuando `vanillaEffectsMode = SHOW_BOTH`
  (el modo de ocultado no pertenece a la spec de este standalone y fue
  eliminado).
- **Coste ~0% con la lista vacía** (Fase 5): sin efectos activos, con el HUD
  apagado, con `F3+H`-style `hideGui` o sin jugador, `renderLive` devuelve tras
  3–4 comprobaciones de campo, sin asignaciones ni recorrido de lista.

## Decisos de la extracción

1. **Nivel I oculto** (`hideLevelOne = true`, regla por defecto del original):
   `Velocidad II` se muestra con sufijo y `Regeneración`/`Fuerza` sin él.
2. El formato fija `mm:ss` con relleno (`01:30`), ancho de texto estable, según
   los ejemplos de la spec.
3. Se conservaron del original: íconos vía sprite atlas sin teñir + cuadrado de
   color nativo como fallback, orden benéficos → neutros → perjudiciales por
   nombre, ajuste al tickrate real del mundo (`tickRateManager().tickrate()`) y
   el fade de expiración exacto.
4. Sin dependencia de Mod Menu (se eliminó el repositorio Terraformers y el
   entrypoint `modmenu`): el mod solo requiere **Fabric API**.

## Compilar

```bash
./gradlew build        # → build/libs/effect-timer-1.0.0.jar (+ -sources.jar)
./gradlew test         # tests de formato/colores/persistencia
```

Requisitos: JDK 21+ y acceso a los repositorios de Maven de Fabric/Mojang.

## Release automatizada (GitHub Actions)

Pipeline: [`.github/workflows/release.yml`](.github/workflows/release.yml).

- **Trigger**: creación de un tag `v*` (p. ej. `v1.0.0`) o ejecución manual
  (`workflow_dispatch`).
- Runner `ubuntu-latest`, Temurin JDK 21, `chmod +x ./gradlew`,
  `./gradlew build --no-daemon` (incluye los tests).
- `softprops/action-gh-release@v2` crea/actualiza la Release del tag y adjunta
  el `effect-timer-*.jar` de `build/libs/` (excluye `-sources`/`-dev`).

Descargas: <https://github.com/BlackBoneDel/EFFECT-HUD-/releases>


## Licencia

MIT, basado en `HUD-MODS` (© Customizable HUD Contributors / Denvdrite).
