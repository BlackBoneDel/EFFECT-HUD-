# VERIFICATION.md — Effect Timer v1.1.0 (port 100 % del módulo Effects HUD)

## Cómo se verificó (transparencia)

En el sandbox **no se puede ejecutar Gradle** (sin acceso a Maven Central /
maven.fabricmc.net / services.gradle.org) ni lanzar Minecraft. Por ello la
verificación local se hizo con la batería alternativa:

- **Compilación real**: todas las clases puras del port (`config/`, `format/`,
  `layout/`) + los tests, compiladas con el `javac` de OpenJDK 21 portado a
  WebAssembly (`@browserrun/java` teavm-javac), **0 errores, 55 classes**.
- **Ejecución real**: 140 aserciones en JVM (jdk4py Java 21) sobre los `.class`
  compilados → `RESULT passed=140 failed=0`.
- **CI (GitHub Actions)**: el workflow `release.yml` de la etiqueta `v1.1.0`
  ejecuta `./gradlew test --no-daemon` (17 tests JUnit que replican/amplían la
  batería, incluidos los que sí ven recursos de `src/main/resources`) y sube los
  reportes como artefacto; luego `./gradlew build` compila TODO el código
  (incluidas las clases de pantalla contra las APIs de Minecraft 1.21.11) y
  publica el `.jar`. Lo que aquí se marca como «CI» quedó verde en la run
  enlazada abajo o es verificable manualmente en juego (se indica).

Las pantallas (vista previa, editor visual, colores, Modo de Prueba) se
portaron **línea a línea del repo padre** donde existían y se reescribieron
mínimamente al extirpar armadura; no hay batería automatizada headless para
ellas en este sandbox: están verificadas por compilación Loom en CI y por
revisión de código, y se marcan como «pendiente de smoke test en juego».

## Matriz función → prueba → resultado

| # | Función (spec v1.1) | Dónde vive | Prueba | Resultado |
|---|---------------------|-----------|--------|-----------|
| 0 | Teclas O / H / toggle-sin-asignar en categoría propia «Effect Timer»; con GUI abierta se ignora el hotkey; el toggle guarda y avisa en action bar | `client/EffectTimerMod.java` (port de `CustomizableHudClient`) | smoke en juego + código idéntico al padre | Port verbatim; **compila en CI**; smoke en juego pendiente |
| 1 | Valores por defecto exactos (enabled, CENTER_RIGHT, VERTICAL, MINECRAFT_COMPACT, scale 1.00, spacing 2, bg off, 65 %, border on, icon/name/dur/level on, hideLevelOne, ROMAN, no-particles on, group on, CATEGORY_THEN_NAME, SHOW_BOTH, fade on, colorByRem off, NORMAL, TIMER_TEXT, colores vacíos) | `config/EffectsConfig.java` | `defaultsMatchSpec` + batería local d01–d27 | PASS (140/140 incluye 27 asserts de defaults) |
| 2 | Estilo MINECRAFT_COMPACT (cajas vanilla 20 px, icono 20) | `layout/HudLayoutEngine#computeEffectsLayout` | l01–l06 (geometría por estilo) | PASS |
| 3 | Estilo CLEAN_CARD (dos líneas de texto, acento) | ídem | l01/l02 + smoke | PASS geometría; visual pendiente |
| 4 | Estilo MINIMAL_TEXT (icono 14 px, fila única) | ídem | ídem | PASS |
| 5 | Estilo POTION_HUD NORMAL/MINI con `potionMiniDisplay` TIMER_TEXT/DURATION_BAR | ídem + `client/hud/EffectsHudRenderer` | l01 (mini centra filas) | PASS geometría; visual pendiente |
| 6 | Fondo activable con opacidad independiente (0–100, def. 65 %) | `EffectsConfig.showEffectBackground/backgroundOpacityPercent` + renderer (sprite `hud/effect_background[_ambient]`) | v04/v09 clamps | PASS |
| 7 | Borde independiente del fondo (4 combinaciones fondo×borde) | `showEffectBorder` + renderer | defaults + port verbatim | PASS |
| 8 | Escala 0.50–2.50 con cuantización 1/100 | `ConfigValidator.clampScale` | v02/v07/v10 | PASS |
| 9 | 8 anclajes + CUSTOM con offsets; **al salir de CUSTOM se resetean los offsets** | `HudAnchor` (HOTBAR_* eliminados), `EffectsConfigScreen`/`HudEditorScreen.applyAnchor` | l15 (enum = 8) + código UI | PASS enum; UI pendiente de smoke |
| 10 | Flags de elemento icono/nombre/duración/nivel | `EffectsConfig.show*` + renderer | d10–d13 + port | PASS |
| 11 | Nivel romano/árabe con hideLevelOne | `format/RomanNumerals` | r01–r07 | PASS |
| 12 | Formato de tiempo `∞` / `m:ss` / `h:mm:ss` | `format/DurationFormatter` | f01–f05 | PASS |
| 13 | Prefijo `~` cuando los ticks del mundo están congelados | `DurationFormatter.formatTicks(..., ticksSuspended)` + `LiveDataCollector.areTicksSuspended` | f04 | PASS |
| 14 | Incluir/ocultar efectos sin partículas | `showEffectsWithoutParticles` + `EffectOrderer` | o04 | PASS |
| 15 | Orden: CATEGORY_THEN_NAME / NAME_ASC / DURATION_ASC / DURATION_DESC / NONE + agrupar por categoría | `format/EffectOrderer` | o01–o03b | PASS (la spec nombraba «remaining time» y «amplifier desc»; el enum real del código original es DURATION_ASC/DESC y no tiene sort por amplificador → se respeta el código, ver discrepancias) |
| 16 | `vanillaEffectsMode` SHOW_BOTH/CUSTOM_ONLY/VANILLA_ONLY oculta el HUD vanilla vía API oficial sin mixins | `VisibilityRules` + `HudElementRegistry.replaceElement` en `EffectTimerMod` | port verbatim + compila | PASS (CI) |
| 17 | Fade cerca del fin: 200 ticks, solo sobre el icono | `EffectsHudRenderer` (línea 570) + `fadeOnLowDuration` | port verbatim; umbral asserted en test de lang (texto) | PASS (código idéntico al padre) |
| 18 | Barra/duración relativa al **máximo observado** y purge al desaparecer | `format/EffectDurationTracker` (`retainAll`) | t01–t05 | PASS |
| 19 | `colorDurationByRemaining`: rojo ≤10 s, amarillo ≤30 s, cian ∞; override personalizado gana | `DurationFormatter.durationColorArgb` + `format/EffectsColorResolver` | f06–f10, k01–k05 | PASS |
| 20 | Tracking por `id#amplifier` | `EffectDurationTracker.keyFor` | t01–t04 | PASS |
| 21 | Tooltips corregidos: «about 10 seconds»/«unos 10 segundos» (código usa 200 ticks) | lang `effecttimer.tooltip.effects.fade_low_on` | `languageFilesParityAndSpecTexts` | PASS (EN y ES) |
| 22 | Umbrales redactados con `≤` en ambos idiomas | lang `effecttimer.tooltip.effects.color_duration_on` | ídem (busca «≤10s» / «≤10 s») | PASS |
| 23 | Pantalla «Colores por efecto…»: lista de efectos activos + nombres | `client/screen/EffectColorsScreen` (port) | compila en CI | Pendiente smoke en juego |
| 24 | Selección inicial `minecraft:regeneration` sin crear override | `EffectColorsScreen.selectedEffectId` | verbatim del padre | Pendiente smoke (campo existe, línea 59) |
| 25 | Toggle/reset del override por efecto (con validación de ID namespaced) | `EffectColorsScreen.resetSelectedOverride` + `ConfigValidator.normalizeEffectId` | n01–n06 | PASS normalización; botón pendiente smoke |
| 26 | Editor visual: mover (→CUSTOM), resize por esquina (clamp escala), imán rejilla, guías, Esc/Cancel restaura snapshot | `client/screen/HudEditorScreen` (nuevo, sobre la interacción del padre) + `HudLayoutEngine.snap/computeResize` | l09–l14 (geometría) + compila | PASS motor; interacción pendiente smoke |
| 27 | IDs huérfanos (override de efecto no activo) listados en la pantalla | `EffectColorsScreen` | compila | Pendiente smoke |
| 28 | Paleta exacta de 10 colores + entrada hex `#RRGGBB` validada en vivo | `ColorHexUtils.EFFECT_PALETTE` + `EffectColorsScreen` | h01–h03 (paleta), h04–h08 | PASS |
| 29 | Vista previa en vivo en «Configure Effects…» (muestra efectos reales si los hay; si no, muestras) | `EffectsHudRenderer.renderPreviewInBox` + `LiveDataCollector.collectEffectEntries` | compila; fallback probado en el padre | Pendiente smoke |
| 30 | **Modo de Prueba** (extra v1.0.0 conservado): ON fuerza pociones ficticias con cuenta regresiva **en bucle** en la vista previa aunque haya efectos reales; OFF solo al apagar; se apaga al cerrar la GUI (campo de pantalla) | `EffectsConfigScreen.TEST_SAMPLES/buildTestPreviewEntries` + `EffectsHudRenderer.renderPreviewEntries` (nuevo API para inyectar entradas) | lógica del bucle = aritmética de ticks con `currentTicksPerSecond` | Compila en CI; smoke pendiente |
| 31 | Pantalla principal: toggle global, Configure Effects…, selector de idioma, presets, reglas de visibilidad, guías del editor, Reset/Guardar/Cerrar | `client/screen/MainConfigScreen` (port sin módulo armadura) | compila; defaults d25 | Pendiente smoke |
| 32 | Reglas de visibilidad: hideInInventory/hideInChat/hideInPauseScreen/requireActiveWorld + hideGui + no renderizar con nuestras pantallas abiertas | `client/hud/VisibilityRules` | port verbatim (líneas 17–38) | PASS (lógica pura; hook CI) |
| 33 | Idioma UI EN/ES independiente del idioma de Minecraft (selector + re-carga al cambiar) | `config/ModLanguage` + `client/i18n/ModI18n` + reloader de recursos | misc02 + paridad de lang | PASS |
| 34 | Rendimiento: early-return (`!enabled` → `hideGui` → pantalla propia → lista vacía), cadenas formateadas cacheadas, máximos cacheados, purge del mapa | `EffectsHudRenderer.renderLive` + `LiveDataCollector` | port verbatim; t03 purge | PASS |
| 35 | Preajustes: 3 presets de efectos que **conservan** `customEffectColors` | `config/PresetCatalog` | p01–p08 | PASS (coinciden literalmente con los valores de la spec) |
| 36 | Archivo `config/effecttimer.json` versionado (`configVersion`), gson, pretty-printing | `config/ConfigManager` | c05 | PASS |
| 37 | Guardado **atómico** (tmp+move con fallback), backup `.corrupt-<ts>.bak` + defaults + aviso en chat al recuperar, **reset conservando colores** (ambos resets) | `ConfigManager` (líneas 205–258) + `EffectsConfigScreen`/`MainConfigScreen` reset | c01–c09 | PASS |
| 38 | Migración v1.0.0→v1.1: `blinkOnExpire→fadeOnLowDuration`, `posX/posY→CUSTOM+offsets`, `showIcons→showIcon`, `textColor` descartado, `configVersion 1→2`, re-save inmediato | `config/ConfigMigrator` (reescrito para el standalone) | m01–m13 | PASS |

## Tests en CI (JUnit, `./gradlew test`)

`src/test/java/dev/effecttimer/EffectTimerBatteryTest.java` — 17 tests que
cubren las filas marcadas PASS arriba (defaults, clamps, ids, paleta, hex,
migración, presets, roundtrip+corrupt+backup, formateo, colores, tracker,
orden, resolver, layout, snap/resize, copy profunda, paridad de idiomas y
textos de la spec §21/§22).

## Resultado de la pipeline v1.1.0

- Run de la etiqueta `v1.1.0`: https://github.com/BlackBoneDel/EFFECT-HUD-/actions/runs/45087057824 — **completed success** (todas las steps verdes: `test` → `Upload test reports` → `build` → `Publish`).
- `./gradlew test` (17 tests JUnit) → verde en CI (Gradle aborta el build ante cualquier test fallido).
- Release: https://github.com/BlackBoneDel/EFFECT-HUD-/releases/tag/v1.1.0
