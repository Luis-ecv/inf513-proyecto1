package com.curriculo.email;

import com.curriculo.config.Config;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cliente SMTP de bajo nivel mediante Sockets TCP puros (puerto 25).
 * Permite enviar respuestas en texto plano y tablas HTML estilizadas con cabeceras MIME.
 * Valida los códigos de respuesta del servidor (220, 250, 354) y codifica el Subject según RFC 2047.
 */
public class EmailSender {

    public static boolean enviar(String destinatario, String asunto, String cuerpo) {
        Socket socket = null;
        BufferedReader input = null;
        DataOutputStream output = null;

        try {
            socket = new Socket(Config.MAIL_HOST, Config.SMTP_PORT);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            output = new DataOutputStream(socket.getOutputStream());

            String res = input.readLine(); // Saludo del servidor 220
            if (!validarRespuesta(res, "220", "Conexión SMTP")) return false;

            enviarComando(output, "HELO " + Config.MAIL_HOST + "\r\n");
            res = input.readLine();
            if (!validarRespuesta(res, "250", "HELO")) return false;

            enviarComando(output, "MAIL FROM: <" + Config.MAIL_EMAIL + ">\r\n");
            res = input.readLine();
            if (!validarRespuesta(res, "250", "MAIL FROM")) return false;

            enviarComando(output, "RCPT TO: <" + destinatario + ">\r\n");
            res = input.readLine();
            if (!validarRespuesta(res, "250", "RCPT TO")) return false;

            enviarComando(output, "DATA\r\n");
            res = input.readLine();
            if (!validarRespuesta(res, "354", "DATA")) return false;

            String subjectCodificado = codificarAsunto(asunto);
            String base64Body = Base64.getMimeEncoder().encodeToString(cuerpo.getBytes(StandardCharsets.UTF_8));
            String contenido = "From: " + Config.MAIL_EMAIL + "\r\n" +
                    "To: " + destinatario + "\r\n" +
                    "Subject: " + subjectCodificado + "\r\n" +
                    "MIME-Version: 1.0\r\n" +
                    "Content-Type: text/plain; charset=UTF-8\r\n" +
                    "Content-Transfer-Encoding: base64\r\n" +
                    "\r\n" +
                    base64Body + "\r\n" +
                    ".\r\n";

            output.write(contenido.getBytes(StandardCharsets.UTF_8));
            output.flush();
            res = input.readLine();
            if (!validarRespuesta(res, "250", "Envío de Cuerpo")) return false;

            enviarComando(output, "QUIT\r\n");
            input.readLine();

            System.out.println("Email (Texto) enviado exitosamente a: " + destinatario);
            return true;
        } catch (IOException e) {
            System.err.println("Error de red al enviar email (Texto): " + e.getMessage());
            return false;
        } finally {
            cerrarRecursos(socket, input, output);
        }
    }

    public static boolean enviarHTML(String destinatario, String asunto, String cuerpoHTML) {
        Socket socket = null;
        BufferedReader input = null;
        DataOutputStream output = null;

        try {
            socket = new Socket(Config.MAIL_HOST, Config.SMTP_PORT);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            output = new DataOutputStream(socket.getOutputStream());

            String res = input.readLine(); // Saludo del servidor 220
            if (!validarRespuesta(res, "220", "Conexión SMTP")) return false;

            enviarComando(output, "HELO " + Config.MAIL_HOST + "\r\n");
            res = input.readLine();
            if (!validarRespuesta(res, "250", "HELO")) return false;

            enviarComando(output, "MAIL FROM: <" + Config.MAIL_EMAIL + ">\r\n");
            res = input.readLine();
            if (!validarRespuesta(res, "250", "MAIL FROM")) return false;

            enviarComando(output, "RCPT TO: <" + destinatario + ">\r\n");
            res = input.readLine();
            if (!validarRespuesta(res, "250", "RCPT TO")) return false;

            enviarComando(output, "DATA\r\n");
            res = input.readLine();
            if (!validarRespuesta(res, "354", "DATA")) return false;

            String subjectCodificado = codificarAsunto(asunto);
            StringBuilder sbContenido = new StringBuilder();

            // Verificar si el cuerpo contiene imágenes inline para incrustar mediante MIME con CID
            if (cuerpoHTML != null && cuerpoHTML.contains("<!--CID_ATTACH:")) {
                Map<String, String> imagenesCid = new LinkedHashMap<>();
                Pattern p = Pattern.compile("<!--CID_ATTACH:([a-zA-Z0-9_-]+):([A-Za-z0-9+/=\r\n]+)-->");
                Matcher m = p.matcher(cuerpoHTML);
                while (m.find()) {
                    imagenesCid.put(m.group(1), m.group(2).replaceAll("\\s+", ""));
                }
                String cuerpoLimpio = m.replaceAll("");

                String boundary = "----=_Part_Curriculo_" + System.currentTimeMillis();
                String base64Body = Base64.getMimeEncoder().encodeToString(cuerpoLimpio.getBytes(StandardCharsets.UTF_8));

                sbContenido.append("From: ").append(Config.MAIL_EMAIL).append("\r\n");
                sbContenido.append("To: ").append(destinatario).append("\r\n");
                sbContenido.append("Subject: ").append(subjectCodificado).append("\r\n");
                sbContenido.append("MIME-Version: 1.0\r\n");
                sbContenido.append("X-Mailer: SeguimientoCurricularBot/1.0\r\n");
                sbContenido.append("Auto-Submitted: auto-generated\r\n");
                sbContenido.append("Content-Type: multipart/related; boundary=\"").append(boundary).append("\"\r\n");
                sbContenido.append("\r\n");

                // Parte 1: Cuerpo HTML
                sbContenido.append("--").append(boundary).append("\r\n");
                sbContenido.append("Content-Type: text/html; charset=UTF-8\r\n");
                sbContenido.append("Content-Transfer-Encoding: base64\r\n");
                sbContenido.append("\r\n");
                sbContenido.append(base64Body).append("\r\n");

                // Partes 2..N: Imágenes inline con Content-ID (CID)
                for (Map.Entry<String, String> entry : imagenesCid.entrySet()) {
                    String cid = entry.getKey();
                    String b64Img = entry.getValue();

                    sbContenido.append("--").append(boundary).append("\r\n");
                    sbContenido.append("Content-Type: image/png\r\n");
                    sbContenido.append("Content-Transfer-Encoding: base64\r\n");
                    sbContenido.append("Content-ID: <").append(cid).append(">\r\n");
                    sbContenido.append("Content-Disposition: inline; filename=\"").append(cid).append(".png\"\r\n");
                    sbContenido.append("\r\n");

                    // Dividir Base64 en líneas de hasta 76 caracteres (estándar RFC 2045)
                    for (int i = 0; i < b64Img.length(); i += 76) {
                        sbContenido.append(b64Img.substring(i, Math.min(i + 76, b64Img.length()))).append("\r\n");
                    }
                }

                sbContenido.append("--").append(boundary).append("--\r\n");
                sbContenido.append(".\r\n");
            } else {
                // Flujo estándar para correos HTML sin imágenes locales inline
                String base64Body = Base64.getMimeEncoder().encodeToString(cuerpoHTML.getBytes(StandardCharsets.UTF_8));
                sbContenido.append("From: ").append(Config.MAIL_EMAIL).append("\r\n");
                sbContenido.append("To: ").append(destinatario).append("\r\n");
                sbContenido.append("Subject: ").append(subjectCodificado).append("\r\n");
                sbContenido.append("MIME-Version: 1.0\r\n");
                sbContenido.append("X-Mailer: SeguimientoCurricularBot/1.0\r\n");
                sbContenido.append("Auto-Submitted: auto-generated\r\n");
                sbContenido.append("Content-Type: text/html; charset=UTF-8\r\n");
                sbContenido.append("Content-Transfer-Encoding: base64\r\n");
                sbContenido.append("\r\n");
                sbContenido.append(base64Body).append("\r\n");
                sbContenido.append(".\r\n");
            }

            output.write(sbContenido.toString().getBytes(StandardCharsets.UTF_8));
            output.flush();
            res = input.readLine();
            if (!validarRespuesta(res, "250", "Envío de Cuerpo HTML")) return false;

            enviarComando(output, "QUIT\r\n");
            input.readLine();

            System.out.println("Email (HTML) enviado exitosamente a: " + destinatario);
            return true;
        } catch (IOException e) {
            System.err.println("Error de red al enviar email (HTML): " + e.getMessage());
            return false;
        } finally {
            cerrarRecursos(socket, input, output);
        }
    }

    public static String normalizarCuerpoSMTP(String cuerpo) {
        if (cuerpo == null) return "";
        String texto = cuerpo.replace("\r\n", "\n").replace("\r", "\n");
        String[] lineas = texto.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lineas.length; i++) {
            String linea = lineas[i];
            if (linea.startsWith(".")) {
                sb.append("."); // Dot-stuffing RFC 5321 sección 4.5.2
            }
            sb.append(linea);
            if (i < lineas.length - 1) {
                sb.append("\r\n");
            }
        }
        return sb.toString();
    }

    private static boolean validarRespuesta(String respuesta, String codigoEsperado, String etapa) {
        if (respuesta == null || !respuesta.startsWith(codigoEsperado)) {
            System.err.println("Fallo SMTP en etapa [" + etapa + "]. Respuesta recibida: " + respuesta + " (Se esperaba: " + codigoEsperado + ")");
            return false;
        }
        return true;
    }

    public static String codificarAsunto(String asunto) {
        if (asunto == null) return "";
        boolean requiereCodificacion = false;
        for (char c : asunto.toCharArray()) {
            if (c > 127) {
                requiereCodificacion = true;
                break;
            }
        }
        if (requiereCodificacion) {
            return "=?UTF-8?B?" + Base64.getEncoder().encodeToString(asunto.getBytes(StandardCharsets.UTF_8)) + "?=";
        }
        return asunto;
    }

    private static void enviarComando(DataOutputStream output, String comando) throws IOException {
        output.write(comando.getBytes(StandardCharsets.UTF_8));
        output.flush();
    }

    private static void cerrarRecursos(Socket socket, BufferedReader input, DataOutputStream output) {
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error al cerrar recursos de red SMTP: " + e.getMessage());
        }
    }
}
