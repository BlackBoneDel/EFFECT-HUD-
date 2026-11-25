# Potion Effect 1.2.0

Renombrado de «Effect Timer» a **Potion Effect**. El id del mod, el paquete y el
archivo de configuración no cambian, así que la configuración existente sigue
funcionando.

## Aviso al actualizar

- Si tienes instalado el jar `effect-timer-*.jar`, bórralo antes de usar este.
  Los dos tienen el mismo id de mod (`effecttimer`).
- Tu `config/effecttimer.json` se migra solo, de `configVersion` 2 a 3. La
  migración añade el interruptor global de colores y la sombra de texto, y no
  toca nada más.

## Novedades

- **Icono nuevo**: sprite pixel art de 16×16 escalado a 128 px, con 12 colores
  como máximo. Se genera con `tools/generate_icon.py`.
- **Posiciones rápidas**: cuatro anclajes laterales nuevos (alto, medio y bajo, a
  izquierda y derecha) y un selector 3×2 en «Configurar efectos» y en el editor.
  Al salir de «Personalizado» se restablecen los desplazamientos.
- **Sin fondo, sin marco**: el borde y la tarjeta solo se dibujan con el fondo
  activado. El valor del borde se conserva, y el control aparece en gris con un
  aviso mientras el fondo esté apagado. El acento de «Tarjeta limpia» se mantiene.
- **Colores personalizados con interruptor**: interruptor global (activado por
  defecto) y casilla por efecto en «Colores por efecto». Al apagarlo se pausan
  todos los colores personalizados sin borrar sus valores. Prioridad: color
  personalizado, luego color por tiempo restante, y por último el automático.
- **Colores automáticos más vívidos**: más saturación y brillo, con el mismo tono
  y mejor legibilidad. Ver [`docs/vivid-colors.md`](docs/vivid-colors.md).
- **Sombra de texto**: opción nueva, activada por defecto, para nombre, nivel y
  temporizador.
- **«HUD vanilla» arriba**: es el primer control de «Configurar efectos», dentro
  de la sección Visibilidad.

## Calidad

- 77 tests JUnit, que se ejecutan en CI.
- Migración de configuración probada de la versión 1 a la 3.
- Lo que se verifica y lo que falta comprobar en el juego está en
  [`docs/VERIFICATION.md`](docs/VERIFICATION.md).

## Requisitos

Minecraft 1.21.11, Fabric Loader 0.20.1 o superior, Fabric API y Java 21.
