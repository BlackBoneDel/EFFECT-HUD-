# Effect Timer — Effects HUD 100 % standalone (Fabric)

*by Denvdrite*

Port **completo del módulo "Effects HUD"** de [Hud Utils (HUD-MODS)](https://github.com/BlackBoneDel/HUD-MODS)
a un mod **independiente**: la lógica del repo padre se portó tal cual (clases de
config, formato, orden, colores, layout y pantallas) y se extirpó todo rastro de
los demás módulos (no queda una sola referencia a Armor/Durabilidad). La spec de
v1.0.0 (10 parámetros) queda **derogada**: esta versión expone la superficie
completa del módulo original.

## Versiones (idénticas a las del repositorio fuente)

| Componente        | Versión                |
|-------------------|------------------------|
| Minecraft         | `1.21.11` (mojmap)     |
| Mod Loader        | Fabric (`fabric-loader >= 0.20.1`) |
| Fabric API        | `0.141.6+1.21.11`      |
| Java              | `21`                   |
| Loom              | `1.17.21` (plugin `net.fabricmc.fabric-loom-remap`) |
| Gradle            | `9.8.2` (wrapper incluido) |

## Controles

- Tecla **O** → pantalla principal (toggle del HUD, *Configure Effects…*, idioma,
  preajustes, reglas de visibilidad, guías del editor, reset y guardar).
- Tecla **H** → **editor visual**: arrastra el HUD para moverlo (el anclaje pasa a
  `CUSTOM` automáticamente), arrastra una esquina para escalar (0.50–2.50),
  imán a rejilla + guías de centrado; `Escape`/Cancelar restaura lo que había al entrar.
- **Tercera ligación sin asignar** → toggle on/off que guarda al instante y avisa
  en la action bar.
- Las tres teclas viven en su propia categoría **"Effect Timer"** del juego.
- Integración opcional con **Mod Menu** (abre la misma pantalla principal).

## Funcionalidad portada del módulo original

- **Estilos**: `MINECRAFT_COMPACT`, `CLEAN_CARD`, `MINIMAL_TEXT` y `POTION_HUD`
  (con submodo `NORMAL`/`MINI` y mini-indicador `TIMER_TEXT`/`DURATION_BAR`).
- **Apariencia**: fondo activable con opacidad 0–100 % (def. 65 %), borde
  independiente, `rowSpacing` 0–16, escala con cuantización 1/100.
- **Elementos**: icono, nombre, duración, nivel (romano/árabe, `hideLevelOne`).
- **Tiempo**: prefijo `~` cuando el reloj del mundo está congelado, barra/duración
  relativa al **máximo observado** por efecto (purge al desaparecer), fade del
  icono cerca del final (umbral 200 ticks ≈ 10 s), `colorDurationByRemaining`
  (rojo ≤10 s, amarillo ≤30 s, cian ∞; el color personalizado por efecto gana).
- **Filtros/orden**: ocultar efectos sin partículas, agrupar por categoría,
  orden por nombre/duración ascendente-descendente, `CATEGORY_THEN_NAME`, `NONE`.
- **Colores por efecto**: paleta exacta de 10 colores, validación `#RRGGBB`,
  override on/off y reset por ID *namespaced* (`minecraft:speed`), IDs huérfanos
  listados, selección inicial `minecraft:regeneration` sin crear override.
- **Preajustes**: `VANILLA_COMPACT`, `COMPACT_TEXT`, `EXPANDED_READOUT` (conservan
  los colores personalizados; los definidos en la spec coinciden con los del código).
- **Idioma de la UI** (EN/ES) independiente del idioma del juego, con paridad de
  claves entre `en_us.json` y `es_es.json`.
- **Modo de Prueba** (extra v1.0.0 conservado): en *Configure Effects…* fuerza
  pociones ficticias con cuenta regresiva en bucle en la vista previa, aunque
  tengas efectos reales; se apaga solo al cerrar la pantalla.
- **Render**: enganchado con la API oficial `HudElementRegistry` (0 mixins),
  early-return cuando no hay jugador/efectos/HUD oculto/regla de visibilidad,
  y sustitución del HUD vanilla según `vanillaEffectsMode`
  (`SHOW_BOTH` / `CUSTOM_ONLY` / `VANILLA_ONLY`).
- **Persistencia** (`config/effecttimer.json`): `configVersion`, guardado
  atómico (tmp + move), recuperación ante JSON corrupto con copia
  `.corrupt-<ts>.bak`, migración automática desde el formato plano de v1.0.0
  (`blinkOnExpire → fadeOnLowDuration`, `posX/posY → CUSTOM + offsets`,
  `showIcons → showIcon`, `textColor` descartado).

## Construir

```bash
./gradlew test build     # → build/libs/effect-timer-<versión>.jar
```

## Release / descargas

El workflow [Release](../../actions/workflows/release.yml) (etiquetas `v*`) corre
`./gradlew test build --no-daemon`, sube los reportes de tests como artefacto y
publica el `.jar` en [Releases](../../releases). Instalación: coloca el `.jar` en
`.minecraft/mods` junto a Fabric Loader ≥ 0.20.1 y Fabric API para 1.21.11.

Última release: **[v1.0.0](../../releases/tag/v1.0.0)** (standalone de 10
parámetros); la v1.1.0 corresponde al port 100 % del módulo Effects HUD.

## Verificación

Matriz función → prueba → resultado en [`docs/VERIFICATION.md`](docs/VERIFICATION.md).
