package com.curriculo;

import com.curriculo.config.Config;
import com.curriculo.email.EmailReceiver;

/**
 * Punto de entrada del sistema de Seguimiento al Diseño Curricular por Correo Electrónico.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("  SISTEMA DE SEGUIMIENTO AL DISEÑO CURRICULAR (VIA EMAIL)");
        System.out.println("  Grupo 02 SA - TecnoWeb UAGRM");
        System.out.println("=================================================================");
        System.out.println("Servidor de Correo : " + Config.MAIL_HOST);
        System.out.println("Buzón de Escucha   : " + Config.MAIL_EMAIL);
        System.out.println("Base de Datos      : " + Config.DB_HOST + ":" + Config.DB_PORT + "/" + Config.DB_NAME);
        System.out.println("Intervalo Sondeo   : " + (Config.POP3_POLL_INTERVAL_MS / 1000) + " segundos");
        System.out.println("=================================================================");
        System.out.println("Iniciando servicio de escucha de correos...");

        EmailReceiver receiver = new EmailReceiver();
        Thread hiloEscucha = new Thread(receiver, "Hilo-EmailReceiver");
        hiloEscucha.setDaemon(false);
        hiloEscucha.start();

        System.out.println("Servicio activo y a la espera de nuevos comandos.");
    }
}
