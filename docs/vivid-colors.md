# Colores automáticos vívidos — tabla «antes → después» (v1.2, bloque 6)

Algoritmo: `dev.effecttimer.format.EffectsColorResolver.vividize` (puro, tests
JUnit en `VividColorsTest`). **La columna «después» está calculada ejecutando la
misma clase compilada que usa el mod** (driver `testharness.VividTable` sobre el
harness local, JVM 21), no una reimplementación aproximada.

> **Nota honesta (DESVIACIÓN):** en este sandbox no hay acceso al `.jar` de
> Minecraft 1.21.11 ni a stubs con los `MobEffect.getColor()` nativos, así que
> la columna «antes» usa la paleta pública estándar de los efectos vanilla
> (constantes de `MobEffect`, estables desde 1.9). Si algún «antes» difiriera en
> 1.21.11, el «después» sigue siendo la aplicación exacta de la regla sobre ese
> valor — volver a ejecutar el driver con el color corregido lo actualiza.
> Verificar lista completa en juego.

## Efectos vanilla (antes → después)

| Efecto (MobEffect) | Antes | Después | Luminancia |
|---|---|---|---|
| Speed | `#7CDFCF` | `#20FFDB` | 0.79 → 0.80 |
| Slowness | `#5A6C81` | `#4D9FFF` | 0.41 → 0.58 |
| Haste | `#EDB41D` | `#F2B81E` | 0.71 → 0.73 |
| Mining Fatigue | `#4A4343` | `#EBD4D4` | 0.27 → 0.85 |
| Strength | `#932423` | `#F23B3A` | 0.23 → 0.38 |
| Instant Health | `#F82423` | `#FF3433` | 0.32 → 0.37 |
| Instant Damage | `#CC0000` | `#FF3333` | 0.17 → 0.37 |
| Jump Boost | `#243AB1` | `#3454FF` | 0.24 → 0.35 |
| Regeneration | `#98D98C` | `#48FF26` | 0.77 → 0.79 |
| Resistance | `#6E6E2E` | `#F2F242` | 0.41 → 0.90 |
| Fire Resistance | `#E49237` | `#F29B3A` | 0.62 → 0.65 |
| Water Breathing | `#2081CA` | `#269BF2` | 0.45 → 0.53 |
| Invisibility | `#7F7F7F` | `#EBEBEB` | 0.50 → 0.92 |
| Blindness | `#1F1F1F` | `#EBEBEB` | 0.12 → 0.92 |
| Night Vision | `#4A6D98` | `#4D9DFF` | 0.41 → 0.58 |
| Hunger | `#468230` | `#66F233` | 0.44 → 0.78 |
| Weakness | `#484F4F` | `#D6EBEB` | 0.30 → 0.90 |
| Poison | `#4E9331` | `#64F228` | 0.49 → 0.77 |
| Wither | `#676767` | `#EBEBEB` | 0.40 → 0.92 |
| Health Boost | `#F87D23` | `#F87D23` | 0.57 → 0.57 |
| Absorption | `#3E5709` | `#ADF219` | 0.30 → 0.83 |
| Saturation | `#388D46` | `#3CF25A` | 0.46 → 0.75 |
| Glowing | `#94A0B5` | `#73A6FF` | 0.62 → 0.63 |
| Levitation | `#CEFFFF` | `#A6FFFF` | 0.96 → 0.93 |
| Luck | `#339900` | `#51F200` | 0.47 → 0.75 |
| Bad Luck | `#0D712E` | `#1CF263` | 0.34 → 0.73 |
| Slow Falling | `#67E4E9` | `#4DF8FF` | 0.79 → 0.83 |

Los casi-grises (Mining Fatigue, Invisibility, Blindness, Weakness, Wither)
suben a gris muy claro sin inventarse tono, como exige la spec. Levitation es
el único con luminancia ~0.03 menor a cambio de un salto de saturación muy
visible (guarda de 0.92 documentada); Health Boost ya cumplía todos los suelos
y queda intacto.

## Espectro de prueba del algoritmo (entradas JUnit `VividColorsTest`)

| Entrada | Salida | Luminancia | Idempotente |
|---|---|---|---|
| `#FF0000` puro | `#FF3333` | 0.21 → 0.37 | sí |
| `#00FF00` puro | `#00FF00` | 0.72 → 0.72 | sí |
| `#0000FF` puro | `#4D4DFF` | 0.07 → 0.35 | sí |
| `#FFE600` amarillo | `#FFE600` | 0.86 → 0.86 | sí |
| `#8A2F2F` rojo apagado | `#FF2D2D` | 0.26 → 0.35 | sí |
| `#3E7C31` verde sucio | `#5BF23B` | 0.41 → 0.77 | sí |
| `#112233` azul oscuro | `#288DF2` | 0.12 → 0.50 | sí |
| `#6A0DAD` violeta | `#AA33FF` | 0.17 → 0.36 | sí |
| `#B05050` ladrillo | `#FF4D4D` | 0.39 → 0.45 | sí |
| `#99AABB` gris azulado | `#66B3FF` | 0.66 → 0.66 | sí |
| `#7CDFCF` menta | `#20FFDB` | 0.79 → 0.80 | sí |
| `#777777` gris | `#EBEBEB` | 0.47 → 0.92 | sí |
| `#000000` negro | `#EBEBEB` | 0.00 → 0.92 | sí |
| `#FFFFFF` blanco | `#FFFFFF` | 1.00 → 1.00 | sí |

## Reglas y prioridades documentadas

1. S cromática se lleva a ≥ 0.70 con el impulso `max(S×1.25, 0.70)` del spec
   cuando aún no llega; V ≥ 0.95. Casi-grises (S < 0.12): solo V ≥ 0.92, sin
   tocar el tono.
2. La luminancia objetivo es `max(0.35, min(lum(entrada), 0.92))`: el resultado
   nunca es menos legible que la entrada para todos los colores oscuros/tenues
   reales (superconjunto de la antigua «aclarar oscuros»).
3. **Prioridad en hues imposibles** (azul puro, violeta): S ≥ 0.70 y suelo 0.35
   no son simultáneamente alcanzables; gana la legibilidad y S puede quedar
   cerca del suelo. La convergencia a punto fijo del `vividize` (≤12 pasadas)
   hace la operación **exactamente idempotente** incluso con redondeo a canales.
4. Los **overrides** del usuario nunca pasan por `vividize` (test dedicado).
5. La paleta de 10 colores del selector y sus hex **no cambian** (test dedicado).
