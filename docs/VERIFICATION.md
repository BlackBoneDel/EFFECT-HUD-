# VERIFICATION.md — Potion Effect v1.2.0 (port 100 % del módulo Effects HUD)

## Cómo se verificó (transparencia)

En el sandbox **no se puede ejecutar Gradle** (sin acceso a Maven Central /
maven.fabricmc.net / services.gradle.org) ni lanzar Minecraft. Por ello la
verificación local se hizo con dos canales y hay que distinguirlos:

- **[J] JUnit real en CI** — `./gradlew test` ejecuta la batería histórica
  (`EffectTimerBatteryTest` 20 + `BrandingAndAnchorsTest` 10) y las 9 clases
  nuevas de v1.2 (`PotionEffectRenameTest` 4, `IconPixelArtTest` 4,
  `SideAnchorsTest` 6, `BorderRequiresBackgroundTest` 6, `CustomColorsToggleTest` 6,
  `VividColorsTest` 7, `TextShadowTest` 5, `OptionsOrderTest` 3,
  `ConfigMigrationV3Test` 4): **76 tests**; Gradle aborta el build si falla
  alguno. Estos son los únicos tests que viajan con el repo y corren contra el
  entorno oficial.
- **[H] arnés local (harness-only)** — las clases puras (`config/`, `format/`,
  `layout/`) compiladas con el `javac` de OpenJDK 21 portado a WebAssembly
  (0 errores, 56 classes) y ejecutadas en JVM (jdk4py Java 21):
  **195 aserciones → `RESULT passed=195 failed=0`** (incluye los bloques v1.2: frame, laterales, vívido con punto fijo, globales y round-trips). Estas aserciones *no*
  viven en el repo ni corren en CI; son verificación local de esta sesión.
  Cuando una fila dice [H] a secas, esa lógica **no** tiene test JUnit.
- **[—] sin prueba automática** — solo compila en CI (Loom, contra las APIs de
  Minecraft 1.21.11) y se revisó línea a línea contra el repo padre; pendiente
  de smoke test en juego.

Las pantallas (vista previa, editor visual, colores, Modo de Prueba) se
portaron **línea a línea del repo padre** donde existían y se reescribieron
mínimamente al extirpar armadura; no hay batería headless posible aquí para
ellas (necesitan cliente de Minecraft).

## Matriz función → prueba → resultado

Leyenda: **[J]** test JUnit en CI (se nombra) · **[H]** solo arnés local (se
nombra el bloque) · **[—]** sin prueba automática · **smoke** pendiente en juego.

| # | Función (spec v1.1 + correcciones v1.1.1) | Prueba real | Resultado |
|---|--------------------------------------------|-------------|-----------|
| 0 | Teclas O / H / toggle en categoría «Effect Timer», ignora hotkey con GUI abierta, guarda y avisa en action bar | [—] (compila en CI; código idéntico al padre) | smoke pendiente |
| 1 | Valores por defecto exactos (enabled, CENTER_RIGHT, VERTICAL, MINECRAFT_COMPACT, scale 1.00, spacing 2, bg off 65 %, border on, flags on, hideLevelOne, ROMAN, no-particles on, group on, CATEGORY_THEN_NAME, SHOW_BOTH, fade on, colorByRem off, NORMAL, TIMER_TEXT, colores vacíos, configVersion 2) | [J] `defaultsMatchSpec` + [H] d01–d27 | PASS |
| 2 | Estilo MINECRAFT_COMPACT (cajas vanilla 20 px, icono 20) | [H] l01–l06; [J] `layoutEngineGeometry` solo cubre el estilo por defecto | PASS [H]; parcial [J] |
| 3 | Estilo CLEAN_CARD (dos líneas, acento) | [H] l01–l02 | PASS [H]; visual smoke |
| 4 | Estilo MINIMAL_TEXT (icono 14 px, fila única) | [H] l01–l02 | PASS [H]; visual smoke |
| 5 | POTION_HUD NORMAL/MINI con `potionMiniDisplay` TIMER_TEXT/DURATION_BAR | [H] l01 (mini centra filas) | PASS [H]; visual smoke |
| 6 | Fondo activable con opacidad independiente 0–100 (def. 65 %) | [J] `validatorClamps` (110→100, −10→0) + `defaultsMatchSpec`; [H] v04/v09; render del sprite [—] | PASS clamps |
| 7 | Borde independiente del fondo (4 combinaciones) | [J] `defaultsMatchSpec` (flags); render [—] | PASS flags; visual smoke |
| 8 | Escala 0.50–2.50 cuantizada a 1/100 | [J] `validatorClamps` (0.2→0.5, 3.4→2.5, 1.234→1.23) | PASS |
| 9 | **9 anclajes predefinidos + CUSTOM** incl. HOTBAR_LEFT/RIGHT junto a la barra rápida vanilla; offsets en todos; **al salir de CUSTOM se resetean offsets** | [J] `anchorCatalogHasNinePresetsPlusCustom`, `hotbarLeftAnchorsBesideVanillaHotbar`, `hotbarRightAnchorsBesideVanillaHotbar`, `customAnchorUsesRawOffsets`, `snapResizeAndBounds` (catálogo=10), `nullAnchorFallsBackToCenterRight` + [H] anch01–07b, l15. El reset de offsets es código de UI (`EffectsConfigScreen` CycleButton, `if (val != CUSTOM) offsetX=offsetY=0`) → [—] | PASS geometría/enum; reset por revisión de código, smoke pendiente |
| 10 | Flags icono/nombre/duración/nivel | [J] `defaultsMatchSpec`; render [—] | PASS flags; visual smoke |
| 11 | Nivel romano/árabe + hideLevelOne | [J] `romanNumeralsAndLevel` + [H] r01–r07 | PASS |
| 12 | Tiempo `∞` / `m:ss` / `h:mm:ss` | [J] `durationFormatting` + [H] f01–f05 | PASS |
| 13 | Prefijo `~` con ticks del mundo congelados | [J] `durationFormatting` («~0:42») | PASS |
| 14 | Incluir/ocultar efectos sin partículas | [J] `sortModesAndFilters` + [H] o04 | PASS |
| 15 | Orden CATEGORY_THEN_NAME / NAME_ASC / DURATION_ASC / DURATION_DESC / NONE + agrupación | [J] `sortModesAndFilters` + [H] o01–o03b. Nota: la spec citaba «remaining time»/«amplifier desc»; el enum real del padre es DURATION_ASC/DESC sin sort por amplificador → se respeta el código | PASS |
| 16 | `vanillaEffectsMode` SHOW_BOTH/CUSTOM_ONLY/VANILLA_ONLY (oculta el HUD vanilla por API oficial, sin mixins) | [—] compila en CI; hook `HudElementRegistry.replaceElement` no testeable headless | smoke pendiente |
| 17 | Fade cerca del fin: 200 ticks, solo sobre el icono | texto: [J] `languageFilesParityAndSpecTexts`; código renderer [—] | smoke visual pendiente |
| 18 | Barra/duración relativa al **máximo observado** + purge al desaparecer | [J] `observedMaxAndPurge` + [H] t01–t05, t07a/b | PASS |
| 19 | `colorDurationByRemaining`: rojo ≤10 s, amarillo ≤30 s, cian ∞; override gana | [J] `durationColorThresholds` + `colorOverridePriority` + [H] f06–f10, k01–k05 | PASS |
| 20 | Tracking por `id#amplifier` | [J] `trackerKeepsPeakPerIdAndAmplifier` + [H] t06, t07a/b | PASS |
| 21 | Tooltips «about 10 seconds»/«unos 10 segundos» (200 ticks) | [J] `languageFilesParityAndSpecTexts` | PASS EN+ES |
| 22 | Umbrales redactados con `≤` en ambos idiomas | [J] `languageFilesParityAndSpecTexts` | PASS EN+ES |
| 23 | Pantalla «Colores por efecto…»: lista de efectos activos + nombres + búsqueda | búsqueda: [J] `searchFilterSemantics` + [H] srch01–srch08; lista/render [—] | PASS filtro; UI smoke |
| 24 | Selección inicial `minecraft:regeneration` sin crear override | [—] (verbatim del padre) | smoke pendiente |
| 25 | Toggle/reset del override por efecto (validación de ID namespaced) | [J] `effectIdNormalization` + [H] n01–n06; botones [—] | PASS normalización |
| 26 | Editor visual: mover (→CUSTOM), resize por esquina, imán/rejilla, guías, Esc restaura snapshot | motor: [J] `snapResizeAndBounds`, `hudRectHelpers` + [H] l09–l14; interacción [—] | PASS motor; smoke pendiente |
| 27 | IDs huérfanos listados en la pantalla | [—] | smoke pendiente |
| 28 | Paleta exacta de 10 colores + hex `#RRGGBB` validado en vivo | [J] `paletteHasExactlyTenSpecColors`, `hexValidation` + [H] h01–h08 | PASS |
| 29 | Vista previa en vivo en «Configure Effects…» (efectos reales o muestras) | [—] (fallback probado en el padre) | smoke pendiente |
| 30 | Modo de Prueba (extra v1.0.0): pociones ficticias en bucle, OFF al cerrar la GUI | [—] (aritmética de ticks en `EffectsConfigScreen`; no portada a JUnit) | smoke pendiente |
| 31 | Pantalla principal: toggle, Configure Effects…, idioma, presets, visibilidad, guías, Reset/Guardar/Cerrar + **crédito «by Denvdrite»** | crédito: clave y texto [J] `creditPresentInBothLanguagesAndReadme`; render [—] | PASS datos; smoke pendiente |
| 32 | hideInInventory/hideInChat/hideInPauseScreen/requireActiveWorld + hideGui + no render con pantallas propias | flags por [J] `defaultsMatchSpec`; `VisibilityRules` [—] (port verbatim) | smoke pendiente |
| 33 | Idioma UI EN/ES independiente del de Minecraft | paridad: [J] `languageFilesParityAndSpecTexts`; `ModLanguage.fromCode` [H] misc02; reloader [—] | PASS datos |
| 34 | Rendimiento: early-returns, cacheo de cadenas/máximos, purge | purge: [J] `observedMaxAndPurge`; resto [—] | smoke pendiente |
| 35 | 3 presets que **conservan** `customEffectColors` | [J] `presetsApplyAndPreserveColors` + [H] p01–p08 | PASS |
| 36 | `config/effecttimer.json` versionado, gson, pretty-printing | [J] `configRoundTripAtomicAndCorruptBackup` + [H] c05 | PASS |
| 37 | Guardado atómico, backup `.corrupt-<ts>.bak` + defaults + aviso, reset conservando colores | roundtrip/backup: [J] `configRoundTripAtomicAndCorruptBackup` + [H] c01–c09; botones de reset UI [—] (preservación de colores sí: `presetsApplyAndPreserveColors`) | PASS persistencia |
| 38 | Migración v1.0.0→v1.1 (blink→fade, posX/Y→CUSTOM, showIcons→showIcon, textColor fuera, v1→v2, re-save) | [J] `migratesLegacyStandaloneKeys` + [H] m01–m13 | PASS |
| 39 | **Branding v1.1.1**: icono PNG 128×128 RGBA dibujo original referenciado; `authors=["Denvdrite"]`, sin `contributors`, sin nombres/enlaces del repo original en `contact`/homepage/issues; LICENSE intacto; «by Denvdrite» en pantalla, README y release | [J] `iconIsOriginal128x128PngWithAlpha`, `modMetadataIsBrandedClean`, `creditPresentInBothLanguagesAndReadme`, `noArmorReferencesInSources` | PASS (en cuanto CI corra la batería nueva) |

## Bloques de la v1.2 (nuevos, matriz propia)

| # | Función v1.2 | Prueba real | Resultado |
|---|--------------|-------------|-----------|
| B1 | Renombre «Potion Effect» (id/paquete/config intactos), jar `potion-effect-*.jar` | [J] `PotionEffectRenameTest` (fabric.mod, escaneo de TODOS los valores lang, README, gradle.properties + workflow) | PASS en JUnit; texto visible de otras pantallas revisado a mano |
| B2 | Icono pixel art 16×16 → 128 nearest, ≤12 colores, gotas splash, `tools/generate_icon.py` + `docs/icon-preview.png` | [J] `IconPixelArtTest` (ImageIO: bloques 8×8 uniformes, esquinas/margen transparentes, paleta, preview 512) | PASS |
| B3 | 4 anclajes laterales (25/50/75 %) + selector 3×2 en pantalla y editor, offsets reset al salir de CUSTOM, 13+CUSTOM en dropdown, EN/ES | [J] `SideAnchorsTest` (6 tests: catálogo, 3 resoluciones × 3 escalas, margen = CENTER_*, clamp vertical/horizontal, grid, round-trip + migración) + [H] la01–la05 | PASS motor y datos; render en pantalla smoke pendiente |
| B4 | Sin fondo = sin marco (5 estilos + potion mini), valor de borde conservado, control deshabilitado con tooltip, acento CLEAN_CARD siempre | [J] `BorderRequiresBackgroundTest` (6 tests: matriz, conservación, control, round-trip, scan del renderer, coherencia de preajustes) + [H] fr01–fr03 | PASS; aspecto final de presets reportado en la entrega |
| B5 | Global `customColorsEnabled` (default true) + toggle por fila con casilla, cadena de prioridad, preview/Modo de Prueba al instante, presets no lo tocan, reset lo reactiva | [J] `CustomColorsToggleTest` (6 tests) + [H] gc01–gc04; la casilla dibujada y el hit-test [—] | PASS lógica; UI smoke pendiente |
| B6 | `vividize` vívido con suelo de luminancia y punto fijo idempotente; hexes por tiempo `#FF3B3B/#FFE600/#FFFFFF/#00E5FF`; paleta intacta | [J] `VividColorsTest` (7 tests, espectro amplio) + [H] vi02–vi05 + k03 | PASS; tabla docs/vivid-colors.md (ver DESVIACIÓN de colores base en ese doc) |
| B7 | `textShadow` default true, aplicado a nombre/nivel/temporizador (incl. mini) en HUD, preview y test mode | [J] `TextShadowTest` (5 tests: defaults, regla, scan de los drawString del renderer, round-trip, preset/no-preset) + [H] sh01–sh02 | PASS regla; sombreado visual smoke pendiente |
| B8 | «HUD vanilla» como primer control bajo «Visibilidad» en Configurar efectos | [J] `OptionsOrderTest` (orden real de init + grid compartido con el editor) — el orden de widgets en runtime deriva de init() | PASS estructural; visibilidad sin scroll a 854×480 y 1080@scale3 smoke pendiente |
| B9 | configVersion 3 con cadena v1→v2→v3 y v2→v3 puntual; release por CI | [J] `ConfigMigrationV3Test` (4 tests, incluye backup `.corrupt-*.bak` intacto) + CI | PASS |

## Lo que sigue **sin verificar en juego** (honesto)

Todo lo marcado [—] o «smoke» (incluidos los humos de v1.2: casilla de toggle por fila, atenuación global OFF, selector 3×2 pintado, botón de borde en gris con tooltip, sombra de texto real, «HUD vanilla» visible sin scroll en 854×480 y 1080/scale 2-3, colores vívidos y el icono renderizados por Minecraft): pulsación de teclas, render real (estilos, fade,
fondo/borde, preview, editor con ratón, tooltips, guardado con chat visible),
integración Mod Menu y Fabric API, y la posición visual exacta de los anclajes
de hotbar en pantalla. Nada de esto es testeable sin lanzar Minecraft.

## Resultado de la pipeline

- v1.1.0 (base): run https://github.com/BlackBoneDel/EFFECT-HUD-/actions/runs/45087544670 — completed success; release https://github.com/BlackBoneDel/EFFECT-HUD-/releases/tag/v1.1.0
- v1.1.1: run https://github.com/BlackBoneDel/EFFECT-HUD-/actions/runs/45089986473 — **completed success** (13 steps verdes, incluida «Run the test battery» con los 30 tests JUnit) y release https://github.com/BlackBoneDel/EFFECT-HUD-/releases/tag/v1.1.1 con el jar `effect-timer-1.1.1.jar` (174 733 B).
