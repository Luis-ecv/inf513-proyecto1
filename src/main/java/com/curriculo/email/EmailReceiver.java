package com.curriculo.email;

import com.curriculo.comandos.CommandProcessor;
import com.curriculo.config.Config;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import javax.mail.internet.MimeUtility;

/**
 * Receptor de correos electrónicos vía Socket TCP puro sobre protocolo POP3
 * (puerto 110).
 * Sondea periódicamente el buzón del grupo, analiza los comandos y delega la
 * ejecución.
 */
public class EmailReceiver implements Runnable {

    private final CommandProcessor processor = new CommandProcessor();
    private boolean activo = true;

    @Override
    public void run() {
        System.out.println("EmailReceiver iniciado. Escuchando en: " + Config.MAIL_EMAIL);

        while (activo) {
            try {
                procesarBuzon();
            } catch (Exception e) {
                System.err.println("Error durante el ciclo de lectura POP3: " + e.getMessage());
            }

            try {
                Thread.sleep(Config.POP3_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                activo = false;
            }
        }
    }

    private void procesarBuzon() {
        Socket socket = null;
        BufferedReader input = null;
        DataOutputStream output = null;

        try {
            socket = new Socket(Config.MAIL_HOST, Config.POP3_PORT);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            output = new DataOutputStream(socket.getOutputStream());

            input.readLine(); // Saludo inicial +OK

            enviarComando(output, "USER " + Config.MAIL_USER + "\r\n");
            input.readLine();

            enviarComando(output, "PASS " + Config.MAIL_PASSWORD + "\r\n");
            String authResponse = input.readLine();

            if (authResponse == null || authResponse.startsWith("-ERR")) {
                System.err.println("Fallo de autenticación POP3: " + authResponse);
                return;
            }

            enviarComando(output, "STAT\r\n");
            String statLine = input.readLine();

            if (statLine == null || !statLine.startsWith("+OK")) {
                return;
            }

            String[] statParts = statLine.split(" ");
            int totalEmails = Integer.parseInt(statParts[1]);

            if (totalEmails > 0) {
                System.out.println("Nuevos correos detectados en el buzón: " + totalEmails);

                for (int i = 1; i <= totalEmails; i++) {
                    enviarComando(output, "RETR " + i + "\r\n");
                    StringBuilder emailBuilder = new StringBuilder();
                    String line;

                    while ((line = input.readLine()) != null) {
                        if (".".equals(line)) {
                            break;
                        }
                        emailBuilder.append(line).append("\n");
                    }

                    procesarMensajeIndividual(emailBuilder.toString());

                    // Marcar correo como eliminado para no procesarlo reiteradamente
                    enviarComando(output, "DELE " + i + "\r\n");
                    input.readLine();
                }
            }

            enviarComando(output, "QUIT\r\n");
            input.readLine();

        } catch (IOException e) {
            System.err.println("Error de conexión POP3: " + e.getMessage());
        } finally {
            cerrarRecursos(socket, input, output);
        }
    }

    private void procesarMensajeIndividual(String emailRaw) {
        String remitente = "";
        String subject = "";
        boolean inSubject = false;

        for (String linea : emailRaw.split("\n")) {
            if (linea.startsWith("From: ")) {
                inSubject = false;
                String fromRaw = linea.substring(6).trim();
                if (fromRaw.contains("<") && fromRaw.contains(">")) {
                    remitente = fromRaw.substring(fromRaw.indexOf('<') + 1, fromRaw.indexOf('>'));
                } else {
                    remitente = fromRaw;
                }
            } else if (linea.startsWith("Subject: ")) {
                subject = linea.substring(9).trim();
                inSubject = true;
            } else if (inSubject && (linea.startsWith(" ") || linea.startsWith("\t"))) {
                // Soporte para Subject folding (RFC 5322)
                subject += " " + linea.trim();
            } else if (!linea.trim().isEmpty()) {
                inSubject = false;
            }
        }

        // Decodificación de asunto según RFC 2047 (MIME encoded-words UTF-8 / Base64 /
        // Quoted-Printable)
        try {
            subject = MimeUtility.decodeText(subject);
        } catch (Exception e) {
            // Si no se puede decodificar, se conserva el texto original
        }

        System.out.println("Procesando de: [" + remitente + "] | Asunto: [" + subject + "]");

        // Ignorar respuestas automáticas para evitar bucles infinitos
        if (subject.toUpperCase().startsWith("RE:")) {
            System.out.println("Mensaje ignorado (es una respuesta automática RE:).");
            return;
        }

        if (remitente.isEmpty()) {
            System.err.println("No se pudo extraer el remitente del correo. Omitiendo.");
            return;
        }

        // Ejecutar comando
        String respuesta = processor.procesar(subject, remitente);

        // Enviar respuesta al remitente
        String subjectRespuesta = "RE: " + subject;
        if (respuesta != null && respuesta.trim().toLowerCase().startsWith("<html>")) {
            EmailSender.enviarHTML(remitente, subjectRespuesta, respuesta);
        } else {
            EmailSender.enviar(remitente, subjectRespuesta, respuesta != null ? respuesta : "Operación procesada.");
        }
    }

    private void enviarComando(DataOutputStream output, String comando) throws IOException {
        output.write(comando.getBytes(StandardCharsets.UTF_8));
        output.flush();
    }

    private void cerrarRecursos(Socket socket, BufferedReader input, DataOutputStream output) {
        try {
            if (input != null)
                input.close();
            if (output != null)
                output.close();
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            System.err.println("Error al cerrar recursos de red POP3: " + e.getMessage());
        }
    }

    public void detener() {
        this.activo = false;
    }
}
