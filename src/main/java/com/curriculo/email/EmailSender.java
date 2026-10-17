package com.curriculo.email;

import com.curriculo.config.Config;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

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
            String contenido = "From: " + Config.MAIL_EMAIL + "\r\n" +
                    "To: " + destinatario + "\r\n" +
                    "Subject: " + subjectCodificado + "\r\n" +
                    "Content-Type: text/plain; charset=UTF-8\r\n" +
                    "\r\n" +
                    cuerpo + "\r\n" +
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
            String contenido = "From: " + Config.MAIL_EMAIL + "\r\n" +
                    "To: " + destinatario + "\r\n" +
                    "Subject: " + subjectCodificado + "\r\n" +
                    "MIME-Version: 1.0\r\n" +
                    "Content-Type: text/html; charset=UTF-8\r\n" +
                    "Content-Transfer-Encoding: 8bit\r\n" +
                    "\r\n" +
                    cuerpoHTML + "\r\n" +
                    ".\r\n";

            output.write(contenido.getBytes(StandardCharsets.UTF_8));
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
