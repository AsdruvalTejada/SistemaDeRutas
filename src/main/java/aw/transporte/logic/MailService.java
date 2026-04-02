package aw.transporte.logic;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import java.util.Random;

public class MailService {

    // RECUERDA PONER TU CORREO REAL Y TU CONTRASEÑA DE APLICACIÓN AQUÍ
    private static final String REMITENTE = "wilmaryhdez2006@gmail.com";
    private static final String PASSWORD = "rnrq hama gvel tnoh";

    // Cambiamos el método para que devuelva un código generado
    public static String enviarCodigoVerificacion(String destinatario, String nombreUsuario) {

        // 1. Generamos un código de 6 dígitos aleatorio
        String codigoSeguridad = String.format("%06d", new Random().nextInt(999999));

        // 2. Enviamos el correo en segundo plano
        new Thread(() -> {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");

            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(REMITENTE, PASSWORD);
                }
            });

            try {
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(REMITENTE));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
                message.setSubject(" Codigo de Verificacion - Sistema de Rutas");

                String htmlContent = "<h2 style='color: #0033A0;'>Código de Acceso</h2>"
                        + "<p>Hola <b>" + nombreUsuario + "</b>,</p>"
                        + "<p>Para completar tu inicio de sesión, ingresa el siguiente código en la aplicación:</p>"
                        + "<h1 style='color: #e74c3c; letter-spacing: 5px;'>" + codigoSeguridad + "</h1>"
                        + "<hr><p style='font-size: 10px; color: #7f8fa6;'>Si no solicitaste este acceso, ignora este mensaje.</p>";

                message.setContent(htmlContent, "text/html; charset=utf-8");

                Transport.send(message);
                System.out.println("Código enviado a: " + destinatario);

            } catch (MessagingException e) {
                System.out.println("Error al enviar el correo: " + e.getMessage());
            }
        }).start();

        // 3. Devolvemos el código a la pantalla de Login para que sepa qué número esperar
        return codigoSeguridad;
    }
}