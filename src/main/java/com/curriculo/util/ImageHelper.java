package com.curriculo.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Base64;
import javax.imageio.ImageIO;

/**
 * Utilitario para el procesamiento, resolución y optimización de imágenes locales
 * para su incrustación eficiente en correos electrónicos mediante estándar MIME con CID.
 */
public class ImageHelper {

    private ImageHelper() {
        // Evitar instanciación directa
    }

    /**
     * Resuelve un archivo físico a partir de diferentes formatos de ruta relativa.
     * Busca en la ruta directa, en 'storage/' y en 'storage/fotos/'.
     *
     * @param rutaFoto Ruta proporcionada en base de datos o comando.
     * @return El objeto File si existe y es legible, o null si no se encuentra.
     */
    public static File resolverArchivoFoto(String rutaFoto) {
        if (rutaFoto == null || rutaFoto.trim().isEmpty()) {
            return null;
        }

        String limpia = rutaFoto.trim().replace("\\", "/");
        String nombreArchivo = new File(limpia).getName();

        // Candidatos en orden de prioridad
        File[] candidatos = new File[] {
                new File(limpia),
                new File("storage/fotos", nombreArchivo),
                new File("storage", limpia),
                new File("storage/fotos", limpia)
        };

        for (File f : candidatos) {
            if (f.exists() && f.isFile() && f.canRead()) {
                return f;
            }
        }

        return null;
    }

    /**
     * Lee un archivo de imagen, lo redimensiona en memoria a las dimensiones especificadas
     * (manteniendo calidad y transparencia) y lo exporta como cadena Base64 en formato PNG.
     *
     * @param archivo Archivo físico de la imagen.
     * @param ancho Ancho deseado en píxeles.
     * @param alto Alto deseado en píxeles.
     * @return Cadena Base64 de la miniatura PNG, o null si ocurre algún error.
     */
    public static String generarMiniaturaBase64(File archivo, int ancho, int alto) {
        if (archivo == null || !archivo.exists()) {
            return null;
        }

        try {
            BufferedImage original = ImageIO.read(archivo);
            if (original == null) {
                return null;
            }

            BufferedImage miniatura = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = miniatura.createGraphics();

            // Configuración de renderizado de alta calidad para thumbnails suaves
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g.drawImage(original, 0, 0, ancho, alto, null);
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(miniatura, "png", baos);

            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            System.err.println("Aviso [ImageHelper]: No se pudo generar la miniatura para " + archivo.getName() + ": " + e.getMessage());
            return null;
        }
    }
}
