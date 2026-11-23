# Effect Timer v1.1.1 — *by Denvdrite*

Corrección de verificación sobre v1.1.0 (sin rehacer lo que ya funcionaba):

- **Anclajes de hotbar restaurados**: el catálogo correcto de la spec son **9
  anclajes predefinidos + PERSONALIZADO**. `HOTBAR_LEFT`/`HOTBAR_RIGHT` vuelven a
  existir, posicionados junto a la barra rápida vanilla (izquierda/derecha de los
  182 px centrados), sin ninguna relación con armadura/durabilidad. Offset X/Y
  disponibles también en ellos; al salir de PERSONALIZADO los offsets siguen
  reseteándose a 0. Traducciones EN/ES añadidas (`effecttimer.anchor.hotbar_*`).
- **Búsqueda de la pantalla de colores** extraída a `dev.effecttimer.format.EffectSearchFilter`
  (pura y testeable): coincide con nombre o id, case-insensitive, query vacía = todo.
- **Branding completo**: icono `assets/effecttimer/icon.png` redrawnizado como
  botella de pociones arrojadizable 128×128 RGBA (dibujo original, sin texturas
  de Mojang), crédito **«by Denvdrite»** en la pantalla principal (misma clave
  `effecttimer.screen.main.credit` en EN/ES), en el README y en esta descripción
  de release. `fabric.mod.json`: `authors: ["Denvdrite"]`, sin `contributors`,
  `contact` solo con el repo propio. El archivo LICENSE no se toca.
- **Tests**: nueva batería `BrandingAndAnchorsTest` (10 tests JUnit: catálogo y
  geometría de anclajes, offsets, filtro de búsqueda, pico por `id#amplifier`,
  validación binaria del icono PNG, `fabric.mod.json`, paridad de idiomas del
  crédito, ausencia total de referencias a armadura) y la batería existente
  actualizada (catálogo = 10 valores). `docs/VERIFICATION.md` reescrito con una
  matriz honesta: qué está cubierto por JUnit real en CI, qué solo por el arnés
  local, y qué queda pendiente de smoke test en juego.

Requiere Minecraft 1.21.11 + Fabric Loader 0.20.1 (Java 21). Colocar el `.jar`
en `mods/` junto a Fabric API.
