package com.curriculo.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Servicio centralizado y thread-safe de logging para el sistema.
 * Registra eventos operativos en access.log y excepciones/advertencias en error.log,
 * cumpliendo con los estándares de auditoría y monitoreo exigidos por INF513.
 */
public class LoggerService {

    private static final String ACCESS_LOG_FILE = "access.log";
    private static final String ERROR_LOG_FILE = "error.log";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private LoggerService() {
        // Evitar instanciación directa
    }

    /**
     * Registra un acceso exitoso o procesamiento de comando en access.log.
     *
     * @param remitente Correo electrónico del usuario remitente
     * @param comando   Comando recibido en el Asunto (Subject)
     * @param estado    Estado de la operación (OK, RECHAZADO, etc.)
     * @param duracionMs Tiempo en milisegundos que tomó procesar el comando
     */
    public static synchronized void access(String remitente, String comando, String estado, long duracionMs) {
        String timestamp = DATE_FORMAT.format(new Date());
        String logLine = String.format("[%s] [ACCESS] Remitente: %s | Comando: %s | Estado: %s | Duración: %dms%n",
                timestamp,
                (remitente != null && !remitente.isEmpty()) ? remitente : "DESCONOCIDO",
                (comando != null) ? comando : "VACIO",
                (estado != null) ? estado : "PROCESADO",
                duracionMs);

        escribirLog(ACCESS_LOG_FILE, logLine);
    }

    /**
     * Registra un error, excepción o fallo de validación en error.log.
     *
     * @param remitente Correo electrónico del usuario remitente
     * @param comando   Comando en el que ocurrió el error
     * @param mensaje   Descripción del error
     * @param ex        Excepción capturada (puede ser null)
     */
    public static synchronized void error(String remitente, String comando, String mensaje, Throwable ex) {
        String timestamp = DATE_FORMAT.format(new Date());
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[%s] [ERROR] Remitente: %s | Comando: %s | Mensaje: %s%n",
                timestamp,
                (remitente != null && !remitente.isEmpty()) ? remitente : "SISTEMA",
                (comando != null) ? comando : "N/A",
                (mensaje != null) ? mensaje : "Error no especificado"));

        if (ex != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            sb.append(String.format("--- Traza de Excepción ---%n%s%n", sw.toString()));
        }

        escribirLog(ERROR_LOG_FILE, sb.toString());
    }

    private static void escribirLog(String archivoNombre, String contenido) {
        try {
            File file = new File(archivoNombre);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8, true))) {
                writer.write(contenido);
                writer.flush();
            }
        } catch (IOException e) {
            System.err.println("[LoggerService] Error al escribir en " + archivoNombre + ": " + e.getMessage());
        }
    }
}
