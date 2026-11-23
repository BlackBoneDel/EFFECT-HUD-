# Potion Effect v1.2.0 — by Denvdrite

> **Aviso:** si tienes `effect-timer-*.jar` instalado, ** bórralo ** antes de
> usar este (mismo id de mod: `effecttimer`). Tu `config/effecttimer.json` se
> **migra automáticamente** (configVersion 2 → 3: solo añade el interruptor
> global de colores y la sombra de texto; nada más se toca).

Renombrado «Effect Timer» → **Potion Effect** (el id, paquete y archivo de
config se mantienen a propósito para no romper nada).

- **Bloque 1 — Renombre**: nombre visible, categoría de teclas, mensajes,
  README, icono y release; el jar pasa a llamarse `potion-effect-1.2.0.jar`.
- **Bloque 2 — Icono**: nuevo sprite original pixel-art 16×16 → 128 px
  (poción arrojadizable, ≤12 colores, nearest-neighbor puro), generado con el
  script documentado `tools/generate_icon.py`; vista previa en
  `docs/icon-preview.png`.
- **Bloque 3 — Posiciones**: 4 anclajes nuevos (alta/media/baja × izquierda/derecha,
  centro vertical al 25 %/50 %/75 %), selector rápido de 6 botones en
  «Configurar efectos» y en el editor (H); dropdown completo con 13+PERSONALIZADO;
  default sigue siendo centro-derecha; al salir de PERSONALIZADO se resetean los
  desplazamientos.
- **Bloque 4 — Sin fondo = sin marco**: el borde/tarjeta solo se dibuja con el
  fondo activado; el valor de borde se conserva guardado; el control queda en
  gris con tooltip («Requiere activar el fondo») sin fondo; el acento de
  «Tarjeta limpia» sigue siempre.
- **Bloque 5 — Colores personalizados ON/OFF**: interruptor global (por defecto
  ON) + casilla por fila en «Colores por efecto»; en OFF se pausa todo sin
  borrar ningún hex; prioridad: override activo > color por tiempo > automático
  vívido (documentada en tooltip).
- **Bloque 6 — Automáticos más vívidos**: saturación/valor reforzados conservando
  el tono, casi-grises aclarados sin inventarles color, suelo de legibilidad
  (idempotencia exacta garantizada); tabla antes→después en `docs/vivid-colors.md`.
  Los tiempos usan `#FF3B3B / #FFE600 / #FFFFFF / #00E5FF`. La paleta de 10
  colores no cambia. El sprite de efecto nunca se tiñe.
- **Bloque 7 — Sombra de texto**: opción nueva (por defecto ON) para nombre,
  nivel y temporizador en HUD, vista previa y Modo de Prueba.
- **Bloque 8 — «HUD vanilla» arriba**: primer control de «Configurar efectos»,
  bajo la nueva sección «Visibilidad».
- **Bloque 9 — Calidad**: configVersion 3 con migración en cadena probada;
  76 tests JUnit en CI (9 clases nuevas por bloque + batería histórica);
  matriz honesta en `docs/VERIFICATION.md`.

Requiere Minecraft 1.21.11 + Fabric Loader 0.20.1 (Java 21) y Fabric API.
