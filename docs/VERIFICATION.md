# Verificación

Potion Effect se verifica con dos cosas: los tests JUnit que corren en CI y
pruebas manuales en el juego. Esta página indica qué cubre cada una.

## Tests automáticos

`./gradlew test` ejecuta 81 tests en 12 clases (`src/test/java/dev/effecttimer`).
El workflow [CI](../.github/workflows/ci.yml) los ejecuta en cada pull request
hacia `main`, y el workflow de release los ejecuta antes de publicar el jar.

| Clase | Tests | Qué cubre |
|-------|-------|-----------|
| `EffectTimerBatteryTest` | 20 | Valores por defecto, clamps del validador, IDs y hex, migración desde el formato 1.0.0, persistencia atómica con copia `.corrupt-*.bak`, formato de duración, umbrales de color, layout, orden y filtros, prioridad de colores, paridad de idiomas. |
| `BrandingAndAnchorsTest` | 10 | Catálogo de anclajes, anclajes junto a la hotbar, offsets de CUSTOM, búsqueda, tracker de duración, icono, metadatos del mod y ausencia de referencias a armadura. |
| `SideAnchorsTest` | 6 | Anclajes laterales (25 / 50 / 75 %), margen horizontal, clamp de origen y migración de los anclajes antiguos. |
| `BorderRequiresBackgroundTest` | 7 | Regla «sin fondo = sin marco», persistencia del valor de borde y estado del control. |
| `CustomColorsToggleTest` | 7 | Interruptor global de colores, pausa por efecto, cadena de prioridad y migración a v3. |
| `VividColorsTest` | 7 | Color automático vívido: tono, estabilidad, legibilidad y paleta fija. |
| `TextShadowTest` | 5 | Sombra de texto por defecto, aplicación a todas las llamadas de texto y round-trip. |
| `OptionsOrderTest` | 3 | Orden de los controles en «Configurar efectos» y cuadrícula compartida con el editor. |
| `ConfigMigrationV3Test` | 4 | Cadena v1 → v2 → v3 y guardado con copia de seguridad. |
| `IconPixelArtTest` | 4 | Icono de 128×128 con bloques uniformes y documentación de la vista previa. |
| `PotionEffectRenameTest` | 4 | Nombre visible «Potion Effect» en metadatos, idiomas, README y jar. |
| `ModInstallationTest` | 4 | Fabric Loader instala el mod: id, nombre, entorno, versión expandida, entrypoint cliente e icono. |

Los tests de la capa de cliente (pantallas, HUD, render) no pueden ejecutarse
sin Minecraft; se cubren solo con la lógica pura que usan (formato, config,
layout, colores).

## Arranque con Fabric Loader

Los tests se ejecutan dentro de una JVM con Fabric Loader arrancado
(`fabric-loader-junit`), en modo de desarrollo. Al arrancar, el loader lee el
`fabric.mod.json` del mod y lo instala: si el manifiesto, las dependencias
declaradas o el icono estuvieran mal, la suite no llegaría a ejecutarse. La
instalación del mod queda así comprobada en cada ejecución de la CI.

El minion de PIT no arranca el loader: PIT abre una sesión de JUnit por
clase de test y el listener del loader solo puede arrancar una vez por JVM
(`Duplicate setLauncher call!`). Por eso `fabric-loader-junit` se excluye del
classpath de PIT; los tests de este mod son de lógica pura y no lo necesitan.

## Tests de mutación

`./gradlew pitest -I tools/mutation/pitest.init.gradle` ejecuta PIT sobre la
lógica pura (`dev.effecttimer.config`, `.format` y `.layout`, 39 clases) con
los grupos de tests como oráculo (el test de instalación queda fuera: necesita
el loader, que no está en el classpath de PIT). El informe queda en
`build/reports/pitest/mutations.xml` y el resumen por paquete se publica como
anotación en la CI.

El objetivo no es una puntuación alta sino comprobar que los tests detectan
los errores que dicen detectar: las mutaciones que sobreviven son puntos
donde la cobertura de los tests es floja y sirven de lista de trabajo. La
puntuación actual es del 42,9 % sobre 924 mutaciones (396 detectadas).

## Pruebas manuales en el juego

No verificado en esta revisión: las comprobaciones siguientes requieren lanzar
Minecraft 26.2 con Fabric.

- Teclas O, H y la tercera tecla sin asignar.
- Render de los estilos `MINECRAFT_COMPACT`, `CLEAN_CARD`, `MINIMAL_TEXT` y
  `POTION_HUD`, incluido el fade cerca del final y el indicador de congelación.
- Vista previa, Modo de Prueba y casilla de cada efecto en «Colores por efecto».
- Editor visual: arrastrar, escalar, imán a rejilla y `Escape`.
- Ocultar el HUD vanilla con `vanillaEffectsMode`.
- Integración con Mod Menu.
- Posición de los anclajes junto a la hotbar con distintas resoluciones.
