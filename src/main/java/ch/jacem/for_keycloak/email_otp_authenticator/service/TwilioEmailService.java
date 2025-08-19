package ch.jacem.for_keycloak.email_otp_authenticator.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import org.jboss.logging.Logger;

import java.io.IOException;

public class TwilioEmailService {

    private static final Logger logger = Logger.getLogger(TwilioEmailService.class);
    
    private static final String SENDGRID_API_KEY = System.getenv("SENDGRID_API_KEY");
    private static final String FROM_EMAIL = System.getenv("SENDGRID_FROM_EMAIL");
    private static final String FROM_NAME = System.getenv("SENDGRID_FROM_NAME");

    private SendGrid sendGrid;

    public TwilioEmailService() {
        if (SENDGRID_API_KEY != null) {
            sendGrid = new SendGrid(SENDGRID_API_KEY);
            logger.info("Twilio SendGrid initialized successfully");
        } else {
            logger.warn("SendGrid API key not found. Set SENDGRID_API_KEY environment variable.");
        }
    }

    public void sendOTPEmail(String toEmail, String otpCode, int expiryMinutes) {
        try {
            if (SENDGRID_API_KEY == null || FROM_EMAIL == null) {
                throw new RuntimeException("SendGrid credentials not configured. Please set SENDGRID_API_KEY and SENDGRID_FROM_EMAIL environment variables.");
            }

            Email from = new Email(FROM_EMAIL, FROM_NAME != null ? FROM_NAME : "OTP Service");
            Email to = new Email(toEmail);
            String subject = "Your Email Verification Code";
            Content content = new Content("text/plain", buildOTPEmailContent(otpCode, expiryMinutes));

            Mail mail = new Mail(from, subject, to, content);

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                logger.infof("Email sent successfully to %s via SendGrid", toEmail);
            } else {
                logger.errorf("Failed to send email via SendGrid. Status: %d, Body: %s", 
                             response.getStatusCode(), response.getBody());
                throw new RuntimeException("Failed to send email via SendGrid. Status: " + response.getStatusCode());
            }
            
        } catch (IOException e) {
            logger.error("Failed to send email via SendGrid", e);
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    private String buildOTPEmailContent(String otpCode, int expiryMinutes) {
        return String.format(
            "Hello,\n\n" +
            "Your email verification code is: %s\n\n" +
            "This code will expire in %d minutes.\n\n" +
            "If you did not request this code, please ignore this email.\n\n" +
            "Best regards,\n" +
            "Your Application Team",
            otpCode, expiryMinutes
        );
    }
}