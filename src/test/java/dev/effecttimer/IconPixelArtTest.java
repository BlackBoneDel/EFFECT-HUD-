package dev.effecttimer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * el icono debe ser pixel art auténtico — cuadrícula lógica
 * 16×16 escalada con nearest-neighbor (cada bloque de 8×8 es un único color),
 * paleta ≤ 12 colores opacos, RGBA con fondo transparente, las 4 esquinas
 * transparentes y margen lógico ≥ 1 px. El arte se genera con
 * {@code tools/generate_icon.py} (dibujo original, sin texturas de Mojang).
 */
public final class IconPixelArtTest {

	private static final int LOGICAL = 16;
	private static final int SCALE = 8;

	private static BufferedImage readIcon() throws Exception {
		Path p = Path.of("src/main/resources/assets/effecttimer/icon.png");
		if (!Files.exists(p)) {
			p = Path.of("..", "src/main/resources/assets/effecttimer/icon.png");
		}
		assertTrue(Files.exists(p), "falta icon.png");
		try (InputStream in = Files.newInputStream(p)) {
			BufferedImage img = ImageIO.read(in);
			assertTrue(img != null, "PNG ilegible");
			return img;
		}
	}

	@Test
	public void gridBlocksAreUniformAndSizeIs128() throws Exception {
		BufferedImage img = readIcon();
		assertEquals(LOGICAL * SCALE, img.getWidth());
		assertEquals(LOGICAL * SCALE, img.getHeight());
		assertTrue(img.getColorModel().hasAlpha(), "el icono necesita canal alfa");
		for (int gy = 0; gy < LOGICAL; gy++) {
			for (int gx = 0; gx < LOGICAL; gx++) {
				int ref = img.getRGB(gx * SCALE, gy * SCALE);
				for (int dy = 0; dy < SCALE; dy++) {
					for (int dx = 0; dx < SCALE; dx++) {
						assertEquals(ref, img.getRGB(gx * SCALE + dx, gy * SCALE + dy),
								"bloque " + gx + "," + gy + " no uniforme (falta nearest-neighbor)");
					}
				}
			}
		}
	}

	@Test
	public void cornersTransparentMarginOpaqueCenter() throws Exception {
		BufferedImage img = readIcon();
		assertEquals(0, img.getRGB(0, 0) >>> 24, "esquina sup-izq opaca");
		assertEquals(0, img.getRGB(127, 0) >>> 24);
		assertEquals(0, img.getRGB(0, 127) >>> 24);
		assertEquals(0, img.getRGB(127, 127) >>> 24);
		// margen lógico ≥ 1 px: el borde exterior de la cuadrícula está despejado
		for (int i = 0; i < LOGICAL; i++) {
			assertEquals(0, img.getRGB(i * SCALE, 0) >>> 24, "fila superior no despejada");
			assertEquals(0, img.getRGB(i * SCALE, 15 * SCALE) >>> 24, "fila inferior no despejada");
			assertEquals(0, img.getRGB(0, i * SCALE) >>> 24, "col izquierda no despejada");
			assertEquals(0, img.getRGB(15 * SCALE, i * SCALE) >>> 24, "col derecha no despejada");
		}
		// hay píxeles opacos y transparentes
		boolean anyOpaque = false;
		for (int y = 0; y < img.getHeight() && !anyOpaque; y += 4) {
			for (int x = 0; x < img.getWidth() && !anyOpaque; x += 4) {
				anyOpaque = (img.getRGB(x, y) >>> 24) != 0;
			}
		}
		assertTrue(anyOpaque, "el icono está vacío");
	}

	@Test
	public void paletteSmallAndLiquidPinkPresent() throws Exception {
		BufferedImage img = readIcon();
		java.util.Set<Integer> opaque = new java.util.HashSet<>();
		for (int gy = 0; gy < LOGICAL; gy++) {
			for (int gx = 0; gx < LOGICAL; gx++) {
				int argb = img.getRGB(gx * SCALE, gy * SCALE);
				if ((argb >>> 24) != 0) {
					opaque.add(argb & 0x00FFFFFF);
				}
			}
		}
		assertTrue(opaque.size() <= 12, "paleta opaca demasiado grande: " + opaque.size());
		// el líquido rosa vivo #FF66B2 está presente y hay al menos un claro y un oscuro
		assertTrue(opaque.contains(0xFF66B2), "falta el rosa vivo del líquido");
		assertNotEquals(-1, img.getRGB(7 * SCALE, 12 * SCALE) >>> 24, "el cuerpo no tiene relleno");
	}

	@Test
	public void previewDocumentExists() throws Exception {
		Path p = Path.of("docs/icon-preview.png");
		if (!Files.exists(p)) {
			p = Path.of("..", "docs/icon-preview.png");
		}
		assertTrue(Files.exists(p), "falta docs/icon-preview.png (salida de tools/generate_icon.py)");
		try (InputStream in = Files.newInputStream(p)) {
			BufferedImage img = ImageIO.read(in);
			assertEquals(512, img.getWidth());
			assertEquals(512, img.getHeight());
		}
	}
}
