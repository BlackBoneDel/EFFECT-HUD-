# Potion Effect — by Denvdrite

HUD independiente para los efectos de poción de Minecraft, para Fabric
(1.21.11). Muestra los efectos activos con su duración, nivel e icono, y deja
ajustar el aspecto, la posición y los colores por efecto desde el juego.

El id del mod (`effecttimer`), el paquete (`dev.effecttimer`) y el archivo de
configuración (`config/effecttimer.json`) se mantienen del nombre anterior
«Effect Timer» para que las configuraciones existentes sigan funcionando.

## Instalación

Requisitos:

- Minecraft 1.21.11
- Fabric Loader 0.20.1 o superior
- Fabric API 0.141.6+1.21.11
- Java 21
- Mod Menu (opcional, para abrir la configuración desde la lista de mods)

Copia `potion-effect-<versión>.jar` en la carpeta `mods` de tu instancia.

## Controles

| Tecla | Acción |
|-------|--------|
| **O** | Pantalla principal: mostrar u ocultar el HUD, *Configure Effects…*, idioma, preajustes, visibilidad, guías y guardar. |
| **H** | Editor visual: arrastra el HUD para moverlo, arrastra una esquina para escalarlo. `Escape` cancela los cambios. |

Las dos teclas aparecen en la categoría **Potion Effect** de los controles del
juego. Una tercera tecla sin asignar activa o desactiva el HUD de efectos y lo
anuncia en la barra de acción.

## Configuración

La configuración se guarda en `config/effecttimer.json` y se escribe de forma
atómica. Si el archivo está corrupto, se restauran los valores por defecto y se
conserva una copia `.corrupt-<fecha>.bak`. Los archivos de la versión 1.0.0 se
migran automáticamente.

## Icono

El icono está en `src/main/resources/assets/effecttimer/icon.png` y se genera
con `python3 tools/generate_icon.py` (requiere Pillow). La vista previa ampliada
está en `docs/icon-preview.png`.

## Compilar

```bash
./gradlew test build
```

El jar queda en `build/libs/potion-effect-<versión>.jar`. Los tests están en
`src/test/java`.

## Créditos y licencia

Potion Effect es obra de Denvdrite. El módulo de efectos está basado en el
módulo *Effects HUD* de [HUD-MODS](https://github.com/BlackBoneDel/HUD-MODS).

Licencia MIT, ver [LICENSE](LICENSE).

Cómo se verifica el mod: [`docs/VERIFICATION.md`](docs/VERIFICATION.md).
Cambios por versión: [`RELEASE_NOTES.md`](RELEASE_NOTES.md).
