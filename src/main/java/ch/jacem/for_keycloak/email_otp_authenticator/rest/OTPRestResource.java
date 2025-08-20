package ch.jacem.for_keycloak.email_otp_authenticator.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.ClientModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;

import ch.jacem.for_keycloak.email_otp_authenticator.service.TwilioEmailService;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;

@Path("")
public class OTPRestResource {

    private static final Logger logger = Logger.getLogger(OTPRestResource.class);
    private static final String OTP_ALPHABET = "0123456789";
    private static final int OTP_LENGTH = getOTPLengthFromEnv();
    private static final String AUTH_NOTE_OTP_KEY = "for-kc-email-otp-key";
    private static final String AUTH_NOTE_OTP_CREATED_AT = "for-kc-email-otp-created-at";
    private static final int OTP_EXPIRY_SECONDS = 600; // 10 minutes

    private KeycloakSession session;
    private TwilioEmailService emailService;

    public OTPRestResource(KeycloakSession session) {
        this.session = session;
        this.emailService = new TwilioEmailService();
    }

    @POST
    @Path("/send")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendOTP(OTPSendRequest request) {
        try {
            RealmModel realm = session.getContext().getRealm();
            
            if (request.email == null || request.email.isEmpty()) {
                return createErrorResponse("Email is required");
            }

            if (request.method == null || (!request.method.equals("login") && !request.method.equals("signup"))) {
                return createErrorResponse("Method must be 'login' or 'signup'");
            }

            UserModel user = session.users().getUserByEmail(realm, request.email);

            if (request.method.equals("login")) {
                if (user == null) {
                    return createErrorResponse("User not found");
                }
            } else if (request.method.equals("signup")) {
                if (user != null) {
                    return createErrorResponse("User already exists");
                }
                // For signup, create user
                user = session.users().addUser(realm, request.email);
                user.setEmail(request.email);
                user.setEmailVerified(false);
                user.setEnabled(true);
            }

            // Generate OTP
            String otpCode = generateOTP();
            
            // Create or get a default client for OTP API operations
            ClientModel client = getOrCreateOTPApiClient(realm);
            
            // Store OTP in authentication session
            RootAuthenticationSessionModel rootSession = session.authenticationSessions().createRootAuthenticationSession(realm);
            AuthenticationSessionModel authSession = rootSession.createAuthenticationSession(client);
            authSession.setAuthNote(AUTH_NOTE_OTP_KEY, otpCode);
            authSession.setAuthNote(AUTH_NOTE_OTP_CREATED_AT, String.valueOf(System.currentTimeMillis() / 1000));
            authSession.setAuthenticatedUser(user);

            // Send OTP via Twilio
            emailService.sendOTPEmail(request.email, otpCode, OTP_EXPIRY_SECONDS / 60);

            logger.infof("OTP sent successfully to %s for %s", request.email, request.method);
            return createSuccessResponse("OTP sent successfully", Map.of("sessionId", authSession.getParentSession().getId()));

        } catch (Exception e) {
            logger.error("Failed to send OTP", e);
            return createErrorResponse("Failed to send OTP: " + e.getMessage());
        }
    }

    @POST
    @Path("/verify")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response verifyOTP(OTPVerifyRequest request) {
        try {
            RealmModel realm = session.getContext().getRealm();

            if (request.email == null || request.email.isEmpty()) {
                return createErrorResponse("Email is required");
            }

            if (request.code == null || request.code.isEmpty()) {
                return createErrorResponse("OTP code is required");
            }

            if (request.sessionId == null || request.sessionId.isEmpty()) {
                return createErrorResponse("Session ID is required");
            }

            // Find the authentication session
            RootAuthenticationSessionModel rootSession = session.authenticationSessions().getRootAuthenticationSession(realm, request.sessionId);
            if (rootSession == null) {
                return createErrorResponse("Invalid session ID");
            }

            AuthenticationSessionModel authSession = rootSession.getAuthenticationSessions().values().iterator().next();
            if (authSession == null) {
                return createErrorResponse("No authentication session found");
            }

            String storedOTP = authSession.getAuthNote(AUTH_NOTE_OTP_KEY);
            String createdAtStr = authSession.getAuthNote(AUTH_NOTE_OTP_CREATED_AT);

            if (storedOTP == null || createdAtStr == null) {
                return createErrorResponse("No OTP found for this session");
            }

            // Check expiration
            long createdAt = Long.parseLong(createdAtStr);
            long now = System.currentTimeMillis() / 1000;
            if ((now - OTP_EXPIRY_SECONDS) > createdAt) {
                // Clean up expired OTP
                authSession.removeAuthNote(AUTH_NOTE_OTP_KEY);
                authSession.removeAuthNote(AUTH_NOTE_OTP_CREATED_AT);
                return createErrorResponse("OTP has expired");
            }

            // Verify OTP
            if (!storedOTP.equals(request.code)) {
                return createErrorResponse("Invalid OTP code");
            }

            // Update user email verification status
            UserModel user = authSession.getAuthenticatedUser();
            if (user != null) {
                user.setEmailVerified(true);
                logger.infof("Email verified for user: %s", user.getEmail());
            }

            // Clean up OTP
            authSession.removeAuthNote(AUTH_NOTE_OTP_KEY);
            authSession.removeAuthNote(AUTH_NOTE_OTP_CREATED_AT);

            logger.infof("OTP verified successfully for %s", request.email);
            return createSuccessResponse("OTP verified successfully", null);

        } catch (Exception e) {
            logger.error("Failed to verify OTP", e);
            return createErrorResponse("Failed to verify OTP: " + e.getMessage());
        }
    }

    private String generateOTP() {
        SecureRandom secureRandom = new SecureRandom();
        StringBuilder otpBuilder = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            otpBuilder.append(OTP_ALPHABET.charAt(secureRandom.nextInt(OTP_ALPHABET.length())));
        }
        return otpBuilder.toString();
    }

    private Response createSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);
        return Response.ok(response).build();
    }

    private Response createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("data", null);
        return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
    }

    private static int getOTPLengthFromEnv() {
        String otpLength = System.getenv("OTP_LENGTH");
        if (otpLength != null && !otpLength.isEmpty()) {
            try {
                int length = Integer.parseInt(otpLength);
                if (length >= 4 && length <= 10) {
                    return length;
                } else {
                    Logger.getLogger(OTPRestResource.class).warnf("OTP_LENGTH environment variable must be between 4 and 10. Using default: 6");
                }
            } catch (NumberFormatException e) {
                Logger.getLogger(OTPRestResource.class).warnf("Invalid OTP_LENGTH environment variable: %s. Using default: 6", otpLength);
            }
        }
        return 6; // Default length
    }

    private ClientModel getOrCreateOTPApiClient(RealmModel realm) {
        String clientId = "email-otp-api-client";
        ClientModel client = realm.getClientByClientId(clientId);
        
        if (client == null) {
            logger.infof("Creating OTP API client: %s", clientId);
            client = realm.addClient(clientId);
            client.setName("Email OTP API Client");
            client.setDescription("Internal client for Email OTP API operations");
            client.setEnabled(true);
            client.setPublicClient(true);
            client.setDirectAccessGrantsEnabled(false);
            client.setServiceAccountsEnabled(false);
            client.setImplicitFlowEnabled(false);
            client.setStandardFlowEnabled(false);
        }
        
        return client;
    }

    public static class OTPSendRequest {
        public String email;
        public String method;
    }

    public static class OTPVerifyRequest {
        public String email;
        public String code;
        public String sessionId;
    }
}