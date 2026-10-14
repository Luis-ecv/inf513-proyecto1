package com.curriculo.email;

import com.curriculo.config.Config;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Cliente SMTP de bajo nivel mediante Sockets TCP puros (puerto 25).
 * Permite enviar respuestas en texto plano y tablas HTML estilizadas con cabeceras MIME.
 */
public class EmailSender {

    public static void enviar(String destinatario, String asunto, String cuerpo) {
        Socket socket = null;
        BufferedReader input = null;
        DataOutputStream output = null;

        try {
            socket = new Socket(Config.MAIL_HOST, Config.SMTP_PORT);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            output = new DataOutputStream(socket.getOutputStream());

            input.readLine(); // Saludo del servidor 220

            enviarComando(output, "HELO " + Config.MAIL_HOST + "\r\n");
            input.readLine();

            enviarComando(output, "MAIL FROM: <" + Config.MAIL_EMAIL + ">\r\n");
            input.readLine();

            enviarComando(output, "RCPT TO: <" + destinatario + ">\r\n");
            input.readLine();

            enviarComando(output, "DATA\r\n");
            input.readLine();

            String contenido = "From: " + Config.MAIL_EMAIL + "\r\n" +
                    "To: " + destinatario + "\r\n" +
                    "Subject: " + asunto + "\r\n" +
                    "Content-Type: text/plain; charset=UTF-8\r\n" +
                    "\r\n" +
                    cuerpo + "\r\n" +
                    ".\r\n";

            output.write(contenido.getBytes(StandardCharsets.UTF_8));
            output.flush();
            input.readLine();

            enviarComando(output, "QUIT\r\n");
            input.readLine();

            System.out.println("Email (Texto) enviado exitosamente a: " + destinatario);
        } catch (IOException e) {
            System.err.println("Error al enviar email (Texto): " + e.getMessage());
        } finally {
            cerrarRecursos(socket, input, output);
        }
    }

    public static void enviarHTML(String destinatario, String asunto, String cuerpoHTML) {
        Socket socket = null;
        BufferedReader input = null;
        DataOutputStream output = null;

        try {
            socket = new Socket(Config.MAIL_HOST, Config.SMTP_PORT);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            output = new DataOutputStream(socket.getOutputStream());

            input.readLine(); // Saludo del servidor 220

            enviarComando(output, "HELO " + Config.MAIL_HOST + "\r\n");
            input.readLine();

            enviarComando(output, "MAIL FROM: <" + Config.MAIL_EMAIL + ">\r\n");
            input.readLine();

            enviarComando(output, "RCPT TO: <" + destinatario + ">\r\n");
            input.readLine();

            enviarComando(output, "DATA\r\n");
            input.readLine();

            String contenido = "From: " + Config.MAIL_EMAIL + "\r\n" +
                    "To: " + destinatario + "\r\n" +
                    "Subject: " + asunto + "\r\n" +
                    "MIME-Version: 1.0\r\n" +
                    "Content-Type: text/html; charset=UTF-8\r\n" +
                    "Content-Transfer-Encoding: 8bit\r\n" +
                    "\r\n" +
                    cuerpoHTML + "\r\n" +
                    ".\r\n";

            output.write(contenido.getBytes(StandardCharsets.UTF_8));
            output.flush();
            input.readLine();

            enviarComando(output, "QUIT\r\n");
            input.readLine();

            System.out.println("Email (HTML) enviado exitosamente a: " + destinatario);
        } catch (IOException e) {
            System.err.println("Error al enviar email (HTML): " + e.getMessage());
        } finally {
            cerrarRecursos(socket, input, output);
        }
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
