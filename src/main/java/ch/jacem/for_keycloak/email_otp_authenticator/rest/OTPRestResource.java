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
import java.util.List;

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

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response secureLogin(SecureLoginRequest request) {
        try {
            RealmModel realm = session.getContext().getRealm();
            
            if (request.email == null || request.email.isEmpty()) {
                return createErrorResponse("Email is required");
            }
            
            // Find user by email (OTP-only authentication, no password required)
            UserModel user = session.users().getUserByEmail(realm, request.email);
            if (user == null) {
                return createErrorResponse("User not found with this email");
            }

            // Check if user is enabled
            if (!user.isEnabled()) {
                return createErrorResponse("Account is disabled");
            }

            // For OTP-only authentication, we just need to verify the user exists and is enabled

            // Password is valid, now check OTP status
            ClientModel client = getOrCreateOTPApiClient(realm);
            
            // Check if we have an existing session for this user
            String existingSessionId = findExistingLoginSession(realm, user);
            AuthenticationSessionModel authSession;
            
            if (existingSessionId != null) {
                // Use existing session
                RootAuthenticationSessionModel rootSession = session.authenticationSessions()
                    .getRootAuthenticationSession(realm, existingSessionId);
                authSession = rootSession.getAuthenticationSessions().values().iterator().next();
            } else {
                // Create new session
                RootAuthenticationSessionModel rootSession = session.authenticationSessions().createRootAuthenticationSession(realm);
                authSession = rootSession.createAuthenticationSession(client);
                authSession.setAuthenticatedUser(user);
                authSession.setAuthNote("LOGIN_USER_VERIFIED", "true");
                authSession.setAuthNote("LOGIN_USER_VERIFIED_AT", String.valueOf(System.currentTimeMillis() / 1000));
            }

            // Check if OTP code was provided
            if (request.otpCode == null || request.otpCode.isEmpty()) {
                // No OTP provided - send OTP and request it
                String loginOTP = generateOTP();
                authSession.setAuthNote("LOGIN_OTP", loginOTP);
                authSession.setAuthNote("LOGIN_OTP_CREATED_AT", String.valueOf(System.currentTimeMillis() / 1000));

                // Send OTP email
                emailService.sendLoginOTPEmail(request.email, loginOTP, OTP_EXPIRY_SECONDS / 60);

                logger.infof("Login initiated for %s, OTP sent", request.email);
                return Response.ok(Map.of(
                    "success", false,
                    "otpRequired", true,
                    "message", "Please check your email for the verification code.",
                    "sessionId", authSession.getParentSession().getId()
                )).build();
            } else {
                // OTP provided - validate it
                String storedOTP = authSession.getAuthNote("LOGIN_OTP");
                String otpCreatedAtStr = authSession.getAuthNote("LOGIN_OTP_CREATED_AT");
                String userVerified = authSession.getAuthNote("LOGIN_USER_VERIFIED");

                if (userVerified == null || !userVerified.equals("true")) {
                    return createErrorResponse("Invalid login session state");
                }

                if (storedOTP == null || otpCreatedAtStr == null) {
                    return createErrorResponse("No OTP found. Please restart login process.");
                }

                // Check OTP expiration
                long otpCreatedAt = Long.parseLong(otpCreatedAtStr);
                long now = System.currentTimeMillis() / 1000;
                if ((now - OTP_EXPIRY_SECONDS) > otpCreatedAt) {
                    authSession.removeAuthNote("LOGIN_OTP");
                    authSession.removeAuthNote("LOGIN_OTP_CREATED_AT");
                    return createErrorResponse("OTP has expired. Please restart login process.");
                }

                // Verify OTP
                if (!storedOTP.equals(request.otpCode)) {
                    return createErrorResponse("Invalid OTP code");
                }

                // Login successful - clean up session and return success
                authSession.removeAuthNote("LOGIN_OTP");
                authSession.removeAuthNote("LOGIN_OTP_CREATED_AT");
                authSession.removeAuthNote("LOGIN_USER_VERIFIED");
                authSession.removeAuthNote("LOGIN_USER_VERIFIED_AT");

                // Update user last login
                user.setAttribute("last_login", List.of(String.valueOf(System.currentTimeMillis())));

                logger.infof("Secure login completed successfully for %s", request.email);
                return createSuccessResponse("Login successful!", Map.of(
                    "userId", user.getId(),
                    "email", user.getEmail(),
                    "emailVerified", user.isEmailVerified(),
                    "loginTimestamp", System.currentTimeMillis()
                ));
            }

        } catch (Exception e) {
            logger.error("Failed to process secure login", e);
            return createErrorResponse("Failed to process login: " + e.getMessage());
        }
    }

    private String findExistingLoginSession(RealmModel realm, UserModel user) {
        // Look for existing sessions with password verified but pending OTP
        // This is a simplified approach - in production you might want more sophisticated session management
        try {
            // For now, we'll create a new session each time for security
            // You could implement session reuse logic here if needed
            return null;
        } catch (Exception e) {
            return null;
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

    public static class SecureLoginRequest {
        public String email;
        public String otpCode; // Optional - only provide after receiving OTP email
    }
}