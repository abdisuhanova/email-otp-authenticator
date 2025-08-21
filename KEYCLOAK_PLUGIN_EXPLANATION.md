# Keycloak Email OTP Authenticator Plugin - Complete Technical Explanation

## Table of Contents
1. [Project Structure & Keycloak Plugin Basics](#1-project-structure--keycloak-plugin-basics)
2. [pom.xml (Maven Configuration)](#2-pomxml-maven-configuration)
3. [Main Authenticator Classes](#3-main-authenticator-classes)
4. [Conditional Authenticator Classes](#4-conditional-authenticator-classes)
5. [Helper Classes (ConfigHelper)](#5-helper-classes-confighelper)
6. [REST API Classes](#6-rest-api-classes)
7. [Service Classes (TwilioEmailService)](#7-service-classes-twilioemailservice)
8. [META-INF Service Registration Files](#8-meta-inf-service-registration-files)
9. [Theme Templates (FreeMarker Templates)](#9-theme-templates-freemarker-templates)
10. [Internationalization (Message Properties)](#10-internationalization-message-properties)
11. [Complete Plugin Architecture Overview](#complete-plugin-architecture-overview)

---

## 1. Project Structure & Keycloak Plugin Basics

This is a **Keycloak SPI (Service Provider Interface) plugin** that adds email-based OTP (One-Time Password) authentication to Keycloak.

### What is a Keycloak Plugin?
- Keycloak allows you to extend its functionality through SPIs
- Your plugin implements specific interfaces that Keycloak looks for
- When Keycloak starts, it scans for these implementations and registers them
- This plugin specifically adds a new authentication method (email OTP)

### Project Structure:
```
src/main/java/ch/jacem/for_keycloak/email_otp_authenticator/
├── EmailOTPFormAuthenticator.java          # Main authenticator logic
├── EmailOTPFormAuthenticatorFactory.java   # Creates authenticator instances
├── authentication/authenticators/conditional/  # Conditional authentication
├── helpers/                                # Utility classes
├── rest/                                   # REST API endpoints
└── service/                                # Email service integration
```

### Key Concepts:
- **Authenticator**: The main class that handles authentication logic
- **Factory**: Creates and configures authenticator instances
- **SPI Registration**: META-INF files tell Keycloak what services you provide
- **Themes**: Custom UI templates for the login flow

---

## 2. pom.xml (Maven Configuration)

This is the **Maven Project Object Model** file that defines how to build your Keycloak plugin. Think of it as a blueprint for your project.

### Key Sections Explained:

**Project Identity (lines 5-8):**
```xml
<groupId>ch.jacem.for_keycloak</groupId>
<artifactId>email_otp_authenticator</artifactId>
<version>1.1.4</version>
```
- `groupId`: Your organization/namespace identifier 
- `artifactId`: The plugin name
- `version`: Current plugin version

**Java Version (lines 29-31):**
```xml
<java.version>11</java.version>
```
- Specifies Java 11 (required for modern Keycloak versions)

**Keycloak Version Profiles (lines 56-78):**
```xml
<profile>
    <id>keycloak-26.2.0</id>
    <properties><keycloak.version>26.2.0</keycloak.version></properties>
</profile>
```
- Allows building for different Keycloak versions
- Default is 26.2.0 (line 60: `activeByDefault>true`)

**Key Dependencies:**

1. **Keycloak SPIs (lines 81-98):**
   - `keycloak-server-spi`: Core SPI interfaces
   - `keycloak-server-spi-private`: Internal Keycloak APIs
   - `keycloak-services`: Keycloak service classes
   - `scope>provided`: These are provided by Keycloak at runtime

2. **SendGrid Email (lines 108-112):**
   - `sendgrid-java`: Library for sending emails via SendGrid
   - This is bundled with your plugin (no `provided` scope)

**Build Configuration:**
- **maven-shade-plugin (lines 140-166):** Packages your plugin with all dependencies into one JAR
- **Final name (line 128):** Creates JAR like `email-otp-authenticator-dev-kc-26.2.0.jar`

---

## 3. Main Authenticator Classes

### EmailOTPFormAuthenticatorFactory.java

This is the **Factory class** that tells Keycloak about your authenticator. Think of it as the "recipe card" that describes how to create and configure your authenticator.

**Key Methods Explained:**

**create() (line 30):** 
- Returns a singleton instance of your authenticator
- Keycloak calls this when it needs to use your authenticator

**getId() (line 44):**
- Returns `"email-otp-form"` - this is your authenticator's unique identifier
- You'll see this ID in Keycloak's admin UI

**Configuration Properties (lines 79-119):**
```java
getConfigProperties() {
    return Arrays.asList(
        new ProviderConfigProperty(
            SETTINGS_KEY_USER_ROLE,           // Internal key
            "User Role",                      // Display name in admin UI
            "The OTP will only be required...", // Help text
            ProviderConfigProperty.ROLE_TYPE,  // Input type (dropdown, text, etc)
            SETTINGS_DEFAULT_VALUE_USER_ROLE   // Default value
        )
    );
}
```

**Available Settings:**
1. **User Role** (line 81): Restrict OTP to users with specific role
2. **Negate User Role** (line 88): Inverse logic - exclude role from OTP
3. **Code Alphabet** (line 95): Characters used in OTP (default excludes confusing 0,1,I,O)
4. **Code Length** (line 102): OTP length (default 6 characters) 
5. **Code Lifetime** (line 110): How long OTP is valid (default 600 seconds = 10 minutes)

### EmailOTPFormAuthenticator.java

This is the **main business logic** class that handles the actual authentication process.

**Key Methods:**

**authenticate() (line 125):**
- Called when user first reaches this authentication step
- Generates OTP and shows the form
- `generateOtp(context, false)` - creates new OTP
- `context.challenge()` - displays OTP entry form

**action() (line 46):**
- Called when user submits the OTP form
- Handles three scenarios:
  1. **Resend button** (line 67): Generates new OTP
  2. **Empty OTP** (line 83): Shows form again  
  3. **OTP validation** (line 91): Checks if OTP matches

**OTP Generation Process (line 191):**
```java
private String generateOtp(AuthenticationFlowContext context, boolean forceRegenerate) {
    String alphabet = ConfigHelper.getOtpCodeAlphabet(context);  // Get allowed characters
    int length = ConfigHelper.getOtpCodeLength(context);         // Get code length
    
    SecureRandom secureRandom = new SecureRandom();             // Cryptographically secure random
    StringBuilder otpBuilder = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
        otpBuilder.append(alphabet.charAt(secureRandom.nextInt(alphabet.length())));
    }
}
```

**OTP Validation (line 91):**
- Compares user input with stored OTP from authentication session
- Checks if OTP has expired (line 102)
- On success: marks email as verified and proceeds (line 117-121)

**Email Sending (line 217):**
- Uses Keycloak's built-in `EmailTemplateProvider`
- Sends templated email with OTP and TTL information
- Template file: `otp-email.ftl` (line 40)

**Key Security Features:**
- Uses `SecureRandom` for OTP generation
- Stores OTP in authentication session (temporary, user-specific)
- Automatic email verification on successful OTP entry
- Configurable expiration times
- Brute force protection through parent class

---

## 4. Conditional Authenticator Classes

These classes handle **conditional authentication** - they determine whether a user should be required to complete OTP authentication based on certain conditions.

### AcceptsFullContextInConfiguredFor.java (Interface)

This is a **custom interface** that extends Keycloak's standard configuration checking.

**Purpose:**
- Standard Keycloak authenticators use `configuredFor(session, realm, user)` 
- This interface adds `configuredFor(context, config)` which provides more information
- Allows access to the full authentication context and authenticator configuration

**Why This Matters:**
- Your EmailOTPFormAuthenticator needs access to configuration settings (like user roles)
- The standard interface doesn't provide enough context to check role-based conditions
- This interface allows checking if OTP should be required based on user roles

### CustomConditionalUserConfiguredAuthenticator.java

This is the **logic engine** that decides whether to require OTP based on conditions.

**Key Method - matchCondition() (line 19):**
```java
public boolean matchCondition(AuthenticationFlowContext context) {
    return matchConditionInFlow(context, context.getExecution().getParentFlow());
}
```
- This method determines: "Should we require OTP for this user?"
- Returns `true` if OTP should be required, `false` if it should be skipped

**Core Logic - matchConditionInFlow() (line 23):**

**Step 1: Collect Execution Steps (lines 24-36)**
```java
List<AuthenticationExecutionModel> requiredExecutions = new LinkedList<>();
List<AuthenticationExecutionModel> alternativeExecutions = new LinkedList<>();
```
- Scans the authentication flow for required vs alternative steps
- Filters out conditional authenticators to avoid infinite loops
- Separates execution steps by their requirement level

**Step 2: Apply Logic (lines 37-42)**
```java
if (!requiredExecutions.isEmpty()) {
    return requiredExecutions.stream().allMatch(e -> isConfiguredFor(e, context));
} else if (!alternativeExecutions.isEmpty()) {
    return alternativeExecutions.stream().anyMatch(e -> isConfiguredFor(e, context));
}
```

**Logic Rules:**
- **Required steps:** ALL must be configured for the user
- **Alternative steps:** ANY ONE must be configured for the user
- **No steps:** Always return true (default to requiring OTP)

**Enhanced Configuration Check - isConfiguredFor() (line 55):**
```java
if (authenticator instanceof AcceptsFullContextInConfiguredFor) {
    return ((AcceptsFullContextInConfiguredFor) authenticator).configuredFor(context, config);
}
return authenticator.configuredFor(context.getSession(), context.getRealm(), context.getUser());
```

**How This Works:**
1. Checks if authenticator implements our enhanced interface
2. If yes: calls enhanced method with full context and config
3. If no: falls back to standard Keycloak method
4. This allows your EmailOTP authenticator to check role-based conditions

### CustomConditionalUserConfiguredAuthenticatorFactory.java

Simple **factory class** that creates instances of the conditional authenticator.

**Purpose:**
- Extends Keycloak's built-in conditional authenticator factory
- Returns singleton instance of your custom conditional authenticator
- Registers this as a service provider with Keycloak

**Real-World Usage Example:**
1. User tries to log in
2. Conditional authenticator checks: "Does this user have the 'admin' role?"
3. If user has admin role and configuration says "require OTP for admins": return `true`
4. If user doesn't have admin role and configuration says "skip OTP for non-admins": return `false`
5. Authentication flow proceeds accordingly - either shows OTP form or skips it

---

## 5. Helper Classes (ConfigHelper)

ConfigHelper is a **utility class** that makes it easy to read configuration settings from Keycloak's admin UI. Think of it as a "configuration translator" that converts the admin settings into usable values for your code.

### Purpose
When admins configure your authenticator in Keycloak's UI, the settings are stored as text strings in a Map. ConfigHelper provides clean, type-safe methods to access these settings with proper defaults and error handling.

### Configuration Getter Methods

**Role-Based Configuration:**
```java
public static String getRole(AuthenticationFlowContext context) {
    return ConfigHelper.getRole(context.getAuthenticatorConfig());
}
```
- **getRole() (line 18):** Gets the role name that should/shouldn't require OTP
- **getNegateRole() (line 30):** Gets boolean flag for inverse role logic
- Supports both direct config access and context-based access

**OTP Generation Settings:**
```java
public static String getOtpCodeAlphabet(AuthenticationFlowContext context) {
    return ConfigHelper.getOtpCodeAlphabet(context.getAuthenticatorConfig());
}
```
- **getOtpCodeAlphabet() (line 54):** Characters used to generate OTP (default: `23456789ABCDEFGHJKLMNPQRSTUVWXYZ`)
- **getOtpCodeLength() (line 66):** Length of generated OTP (default: 6)
- **getOtpLifetime() (line 42):** How long OTP is valid in seconds (default: 600 = 10 minutes)

### Core Utility Methods

**String Configuration (line 74):**
```java
public static String getConfigStringValue(AuthenticatorConfigModel config, String key, String defaultValue) {
    if (null == config || !config.getConfig().containsKey(key)) {
        return defaultValue;  // Config missing or key not found
    }
    
    String value = config.getConfig().get(key);
    if (null == value || value.isEmpty()) {
        return defaultValue;  // Value is null or empty
    }
    
    return value;  // Valid value found
}
```

**Integer Configuration with Error Handling (line 91):**
```java
public static int getConfigIntValue(AuthenticatorConfigModel config, String key, int defaultValue) {
    // ... null checks ...
    
    try {
        return Integer.parseInt(value);
    } catch (NumberFormatException e) {
        return defaultValue;  // Return default if parsing fails
    }
}
```

**Boolean Configuration (line 113):**
```java
public static boolean getConfigBooleanValue(AuthenticatorConfigModel config, String key, boolean defaultValue) {
    // ... null checks ...
    
    return Boolean.parseBoolean(config.getConfig().get(key));
}
```

### Design Pattern Benefits

**1. Overloaded Methods:**
- Each setting has two versions: `getX(config)` and `getX(context)`
- Context version extracts config automatically: `context.getAuthenticatorConfig()`
- Makes the API flexible for different usage scenarios

**2. Safe Defaults:**
- Every method has a default value from the Factory constants
- Handles null configurations gracefully
- Prevents crashes from misconfigured or missing settings

**3. Type Safety:**
- Converts string configurations to proper types (int, boolean)
- Handles parsing errors gracefully
- Prevents runtime exceptions from invalid admin input

### Real Usage Example

```java
// In EmailOTPFormAuthenticator.java:
String alphabet = ConfigHelper.getOtpCodeAlphabet(context);  // Gets "23456789ABC..." 
int length = ConfigHelper.getOtpCodeLength(context);         // Gets 6
int lifetime = ConfigHelper.getOtpLifetime(context);         // Gets 600

// Generate 6-character OTP from alphabet that expires in 10 minutes
SecureRandom random = new SecureRandom();
for (int i = 0; i < length; i++) {
    otp.append(alphabet.charAt(random.nextInt(alphabet.length())));
}
```

This design makes the code very clean and reduces repetitive null-checking and type conversion throughout your authenticator classes.

---

## 6. REST API Classes (OTPRestProvider, Resource, Factory)

These classes create a **REST API** that allows external applications to send and verify OTP codes programmatically. This is separate from the web-based authentication flow.

### OTPRestProviderFactory.java

**Factory for REST Resources:**
```java
public static final String ID = "email-otp-api";

public RealmResourceProvider create(KeycloakSession session) {
    return new OTPRestProvider(session);
}
```
- Registers the REST API with ID `"email-otp-api"`
- Creates provider instances when needed
- This makes the API available at URLs like: `/auth/realms/{realm}/email-otp-api/*`

### OTPRestProvider.java

**Simple Delegator:**
```java
public Object getResource() {
    return new OTPRestResource(session);
}
```
- Acts as a bridge between Keycloak's resource system and your actual REST endpoints
- Passes the Keycloak session to your resource class

### OTPRestResource.java (The Main API)

This is a **comprehensive REST API** with multiple endpoints for OTP operations. Let me break it down by functionality:

#### **Core Configuration (lines 27-32):**
```java
private static final String OTP_ALPHABET = "0123456789";  // Numeric only
private static final int OTP_LENGTH = getOTPLengthFromEnv();  // Configurable via env var
private static final int OTP_EXPIRY_SECONDS = 600;  // 10 minutes
```

#### **API Endpoints:**

**1. POST /send - Basic OTP Send (lines 41-97):**
```java
@POST
@Path("/send")
public Response sendOTP(OTPSendRequest request) {
```
- **Purpose:** Send OTP for login or signup
- **Input:** `{"email": "user@example.com", "method": "login"}`
- **Logic:**
  - For `login`: User must exist
  - For `signup`: Creates new user if doesn't exist
  - Generates numeric OTP and stores in authentication session
  - Sends email via TwilioEmailService
- **Output:** Session ID for later verification

**2. POST /verify - Basic OTP Verification (lines 99-170):**
```java
@POST
@Path("/verify")
public Response verifyOTP(OTPVerifyRequest request) {
```
- **Purpose:** Verify OTP code
- **Input:** `{"email": "user@example.com", "code": "123456", "sessionId": "..."}`
- **Logic:**
  - Finds authentication session by ID
  - Checks OTP expiry and validity
  - Marks user email as verified on success
- **Security:** Cleans up OTP after verification

**3. POST /login - Secure Login Flow (lines 172-274):**
```java
@POST
@Path("/login")
public Response secureLogin(SecureLoginRequest request) {
```
- **Purpose:** OTP-only login (no password required)
- **Two-phase process:**
  1. First call (no OTP): Sends OTP email, returns session ID
  2. Second call (with OTP): Verifies OTP and completes login
- **Input:** `{"email": "user@example.com", "otpCode": "123456"}` (otpCode optional on first call)
- **Security Features:**
  - Checks if user exists and is enabled
  - Generates login-specific OTP
  - Updates last_login timestamp

**4. POST /otp/send/email - Enhanced Email OTP (lines 278-326):**
```java
@POST
@Path("/otp/send/email")
public Response sendEmailOTP(OTPSendEmailRequest request) {
```
- **Purpose:** Send OTP via email (part of comprehensive system)
- **Enhanced features:**
  - Dedicated email OTP flow
  - Returns expiry information
  - More detailed session management

**5. POST /otp/send/sms - SMS OTP Support (lines 328-377):**
```java
@POST
@Path("/otp/send/sms")
public Response sendSMSOTP(OTPSendSMSRequest request) {
```
- **Purpose:** Send OTP via SMS
- **Features:**
  - Finds user by phone number attribute
  - Phone number masking for security
  - Currently logs SMS (implementation needed)

**6. POST /otp/login/verify-code - OAuth Integration (lines 379-457):**
```java
@POST
@Path("/otp/login/verify-code")
public Response verifyLoginOTP(OTPLoginVerifyRequest request) {
```
- **Purpose:** Verify OTP and get OAuth authorization code
- **Advanced Features:**
  - Supports both email and SMS OTP
  - Generates OAuth authorization code
  - Ready for OAuth 2.0 token exchange flow

#### **Key Helper Methods:**

**OTP Generation (line 548):**
```java
private String generateOTP() {
    SecureRandom secureRandom = new SecureRandom();
    // Generates random numeric code
}
```

**Client Management (line 590):**
```java
private ClientModel getOrCreateOTPApiClient(RealmModel realm) {
    // Creates internal Keycloak client for API operations
    // Configures client as public with appropriate settings
}
```

**User Lookup by Phone (line 461):**
```java
private UserModel findUserByPhoneNumber(RealmModel realm, String phoneNumber) {
    // Searches users by phoneNumber attribute
    // Includes fallback method for compatibility
}
```

#### **Security Features:**

1. **Session Management:** Uses Keycloak's authentication sessions for temporary OTP storage
2. **Expiry Checking:** All OTP codes expire after 10 minutes
3. **Cleanup:** Removes OTP data after verification
4. **User Validation:** Checks user existence and enabled status
5. **Phone Masking:** Protects phone numbers in logs
6. **OAuth Integration:** Provides authorization codes for token exchange

#### **Real-World Usage Example:**

**Mobile App Login Flow:**
1. App calls `/otp/send/email` with user's email
2. User receives OTP email
3. App calls `/otp/login/verify-code` with OTP
4. API returns authorization code
5. App exchanges authorization code for access token
6. User is authenticated

This REST API essentially provides a programmatic way to use your OTP authenticator outside of Keycloak's standard web-based login flows.

---

## 7. Service Classes (TwilioEmailService)

Despite its name, this class uses **SendGrid** (owned by Twilio) to send emails. It's the email delivery service for your OTP system.

### Purpose & Configuration

**Environment Variables Required:**
```java
private static final String SENDGRID_API_KEY = System.getenv("SENDGRID_API_KEY");    // Required
private static final String FROM_EMAIL = System.getenv("SENDGRID_FROM_EMAIL");        // Required  
private static final String FROM_NAME = System.getenv("SENDGRID_FROM_NAME");          // Optional
```

**Constructor Initialization (line 22):**
```java
public TwilioEmailService() {
    if (SENDGRID_API_KEY != null) {
        sendGrid = new SendGrid(SENDGRID_API_KEY);
        logger.info("Twilio SendGrid initialized successfully");
    } else {
        logger.warn("SendGrid API key not found. Set SENDGRID_API_KEY environment variable.");
    }
}
```
- Validates API key presence at startup
- Creates SendGrid client instance
- Logs warnings if misconfigured

### Email Methods

**1. sendOTPEmail() - General OTP (lines 31-63):**
```java
public void sendOTPEmail(String toEmail, String otpCode, int expiryMinutes) {
```
- **Purpose:** Send OTP for email verification/signup
- **Subject:** "Your Email Verification Code"
- **Content:** Uses `buildOTPEmailContent()` template
- **Error Handling:** Throws RuntimeException on failure

**2. sendLoginOTPEmail() - Login-specific OTP (lines 65-97):**
```java
public void sendLoginOTPEmail(String toEmail, String otpCode, int expiryMinutes) {
```
- **Purpose:** Send OTP for login authentication
- **Subject:** "Your Login Verification Code"  
- **Content:** Uses `buildLoginOTPEmailContent()` template
- **Same error handling as above**

### Email Content Templates

**General OTP Email (line 99):**
```java
private String buildOTPEmailContent(String otpCode, int expiryMinutes) {
    return String.format(
        "Hello,\n\n" +
        "Your email verification code is: %s\n\n" +          // OTP code
        "This code will expire in %d minutes.\n\n" +         // Expiry time
        "If you did not request this code, please ignore this email.\n\n" +
        "Best regards,\n" +
        "Your Application Team",
        otpCode, expiryMinutes
    );
}
```

**Login OTP Email (line 111):**
```java
private String buildLoginOTPEmailContent(String otpCode, int expiryMinutes) {
    return String.format(
        "Hello,\n\n" +
        "Your login verification code is: %s\n\n" +
        "This code will expire in %d minutes.\n\n" +
        "If you did not initiate this login, please secure your account immediately.\n\n" +  // Security warning
        "Best regards,\n" +
        "Your Application Team",
        otpCode, expiryMinutes
    );
}
```

### SendGrid API Integration

**Email Construction (lines 37-42):**
```java
Email from = new Email(FROM_EMAIL, FROM_NAME != null ? FROM_NAME : "OTP Service");
Email to = new Email(toEmail);
String subject = "Your Email Verification Code";
Content content = new Content("text/plain", buildOTPEmailContent(otpCode, expiryMinutes));

Mail mail = new Mail(from, subject, to, content);
```

**API Request (lines 44-49):**
```java
Request request = new Request();
request.setMethod(Method.POST);
request.setEndpoint("mail/send");
request.setBody(mail.build());

Response response = sendGrid.api(request);
```

**Response Handling (lines 51-57):**
```java
if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
    logger.infof("Email sent successfully to %s via SendGrid", toEmail);
} else {
    logger.errorf("Failed to send email via SendGrid. Status: %d, Body: %s", 
                 response.getStatusCode(), response.getBody());
    throw new RuntimeException("Failed to send email via SendGrid. Status: " + response.getStatusCode());
}
```

### Key Features

**1. Configuration Validation:**
- Checks for required environment variables
- Provides clear error messages if misconfigured
- Fails fast rather than silent failures

**2. Template System:**
- Separate templates for different OTP types
- Dynamic content injection (OTP code, expiry time)
- Security-conscious messaging

**3. Error Handling:**
- Comprehensive logging for debugging
- HTTP status code checking
- Throws meaningful exceptions for caller handling

**4. Security Considerations:**
- Uses environment variables (not hardcoded credentials)
- Different messaging for different OTP types
- Includes security warnings in login emails

### Setup Requirements

**Environment Variables:**
```bash
export SENDGRID_API_KEY="SG.your-sendgrid-api-key-here"
export SENDGRID_FROM_EMAIL="noreply@yourapp.com"
export SENDGRID_FROM_NAME="Your App Name"  # Optional
```

**SendGrid Account Setup:**
1. Create SendGrid account
2. Generate API key with mail sending permissions
3. Verify sender domain/email address
4. Configure environment variables in your deployment

This service abstracts all email complexity from your authenticator classes, providing a simple interface: "send this OTP code to this email address."

---

## 8. META-INF Service Registration Files

These files are the **magic** that tells Keycloak about your plugin! They're part of Java's **Service Provider Interface (SPI)** system.

### What are META-INF Service Files?

Think of these as "business cards" for your plugin. When Keycloak starts up, it automatically scans these files to discover what services your JAR file provides.

**File Naming Convention:**
- File name = Full interface name that you're implementing
- File content = Full class names of your implementations

### Service Registration Files

**1. org.keycloak.authentication.AuthenticatorFactory**
```
ch.jacem.for_keycloak.email_otp_authenticator.EmailOTPFormAuthenticatorFactory
ch.jacem.for_keycloak.email_otp_authenticator.authentication.authenticators.conditional.CustomConditionalUserConfiguredAuthenticatorFactory
```

**What this does:**
- Tells Keycloak: "This JAR contains 2 authenticator factories"
- **Line 1:** Registers your main email OTP authenticator
- **Line 2:** Registers your conditional authenticator
- **Result:** Both appear in Keycloak's "Authentication" admin section

**2. org.keycloak.services.resource.RealmResourceProviderFactory**
```
ch.jacem.for_keycloak.email_otp_authenticator.rest.OTPRestProviderFactory
```

**What this does:**
- Tells Keycloak: "This JAR provides a REST API resource"
- Registers your OTPRestProviderFactory
- **Result:** API becomes available at `/auth/realms/{realm}/email-otp-api/*`

### How the SPI System Works

**1. Plugin Installation:**
```bash
# When you copy your JAR to Keycloak's providers directory:
cp your-plugin.jar /opt/keycloak/providers/
```

**2. Keycloak Startup Scanning:**
```java
// Keycloak does something like this internally:
ServiceLoader<AuthenticatorFactory> loader = ServiceLoader.load(AuthenticatorFactory.class);
for (AuthenticatorFactory factory : loader) {
    // Registers each factory found in META-INF files
    registerAuthenticator(factory);
}
```

**3. Runtime Registration:**
- Your `EmailOTPFormAuthenticatorFactory.getId()` returns `"email-otp-form"`
- Keycloak registers this ID and can create instances when needed
- Admin UI shows your authenticator as "Email OTP Form"

### What Happens Without These Files?

**Without service registration:**
- Your classes exist in the JAR but Keycloak doesn't know about them
- No errors, but your plugin doesn't appear in admin UI
- Your authenticators aren't available for use

**Common Mistakes:**
1. **Wrong file names:** Must match interface names exactly
2. **Wrong class names:** Must be fully qualified class names
3. **File encoding:** Must be UTF-8
4. **Extra whitespace:** Can cause issues with class loading

### Debugging Service Registration

**Check if services are registered:**
```bash
# Look in Keycloak logs for messages like:
[org.keycloak.provider] Loading provider email-otp-form (ch.jacem.for_keycloak.email_otp_authenticator.EmailOTPFormAuthenticatorFactory)
```

**Verify JAR contents:**
```bash
# Check your JAR contains the service files:
jar -tf your-plugin.jar | grep META-INF/services/
```

**Test in Admin UI:**
1. Go to Authentication → Flows
2. Create new flow or edit existing
3. Look for "Email OTP Form" in available authenticators
4. Your REST API should be accessible at the registered path

### Why This Design?

**Benefits of SPI System:**
1. **Loose Coupling:** Keycloak doesn't need to know your specific classes
2. **Dynamic Discovery:** New plugins work without code changes to Keycloak
3. **Modularity:** Each plugin is self-contained and self-describing
4. **Hot Deployment:** Add plugins without rebuilding Keycloak

**Real-World Analogy:**
- Think of a restaurant with a "specials" board
- META-INF files are like posting your dishes on the board
- Customers (Keycloak) can see what's available
- Without posting, no one knows your dishes exist

This is why your plugin JAR is completely self-contained - it includes both the implementation AND the registration information that tells Keycloak how to use it.

---

## 9. Theme Templates (FreeMarker Templates)

These are **FreeMarker (.ftl) templates** that define the user interface for your OTP authentication. Keycloak uses FreeMarker for rendering both web pages and emails.

### Template Types

**1. Login Form Template: `login-email-otp.ftl`**
This creates the web page where users enter their OTP code.

**2. Email Templates: `otp-email.ftl`**
- **HTML version:** `html/otp-email.ftl` - Rich HTML email
- **Text version:** `text/otp-email.ftl` - Plain text fallback

### Login Form Template Analysis

**Template Structure (lines 1-2):**
```freemarker
<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('email-otp'); section>
```
- **Import:** Uses Keycloak's base template system
- **Layout:** Wraps content in standard Keycloak login page layout
- **Error handling:** Shows/hides messages based on validation errors

**Header Section (lines 4-5):**
```freemarker
<#if section = "header">
    ${msg("doLogIn")}
```
- Displays internationalized "Login" message
- `msg()` function looks up text in message properties files

**Form Section (lines 6-43):**
```freemarker
<form id="kc-otp-login-form" ... action="${url.loginAction}" method="post">
```
- **Form action:** `${url.loginAction}` - Keycloak's login processing URL
- **CSS classes:** `${properties.kcFormClass!}` - Uses Keycloak's theme CSS

**OTP Input Field (lines 13-21):**
```freemarker
<input id="email-otp" name="email-otp" 
       autocomplete="one-time-code"     <!-- Tells browsers this is OTP -->
       type="text" 
       class="${properties.kcInputClass!}" 
       autofocus=true                   <!-- Cursor starts here -->
       dir="ltr" />                     <!-- Left-to-right text -->
```

**Error Display (lines 16-20):**
```freemarker
<#if messagesPerField.existsError('email-otp')>
    <span class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
        ${kcSanitize(messagesPerField.get('email-otp'))?no_esc}
    </span>
</#if>
```
- Shows validation errors for the OTP field
- `kcSanitize()` prevents XSS attacks
- `aria-live="polite"` - Screen reader accessibility

**Action Buttons (lines 27-39):**
```freemarker
<!-- Login Button -->
<button name="login" type="submit">
    ${kcSanitize(msg("doLogIn"))?no_esc}
</button>

<!-- Resend Button -->
<button name="resend-email" type="submit">
    ${kcSanitize(msg("doResendEmail"))?no_esc}
</button>
```
- **Login button:** Submits OTP for verification
- **Resend button:** Sends new OTP code (handled in authenticator)

### Email Templates

**HTML Email Template (`html/otp-email.ftl`):**
```freemarker
<#import "template.ftl" as layout>
<@layout.emailLayout>
<p>${kcSanitize(msg("emailOtpYourAccessCode"))?no_esc}</p>    <!-- "Your access code is:" -->
<h1>${otp?no_esc}</h1>                                        <!-- The OTP code -->
<p>${kcSanitize(msg("emailOtpExpiration", (ttl / 60)?int))?no_esc}</p>  <!-- Expiry message -->
</@layout.emailLayout>
```

**Text Email Template (`text/otp-email.ftl`):**
```freemarker
<#ftl output_format="plainText">                             <!-- Plain text format -->
${kcSanitize(msg("emailOtpYourAccessCode"))}

${otp}                                                       <!-- OTP code -->

${kcSanitize(msg("emailOtpExpiration", (ttl / 60)?int))}    <!-- Expiry in minutes -->
```

### Template Variables Available

**From your authenticator (EmailOTPFormAuthenticator.java:248-260):**
```java
Map<String, Object> attributes = new HashMap<String, Object>();
attributes.put("otp", otp);           // The OTP code
attributes.put("ttl", ConfigHelper.getOtpLifetime(context));  // Time to live in seconds

context.getSession()
    .getProvider(EmailTemplateProvider.class)
    .send(OTP_EMAIL_SUBJECT_KEY, OTP_EMAIL_TEMPLATE_NAME, attributes);
```

**Built-in Keycloak Variables:**
- `${msg("key")}` - Internationalized messages
- `${properties.kcInputClass!}` - Theme CSS classes
- `${url.loginAction}` - Form submission URL
- `${messagesPerField}` - Validation errors
- `${kcSanitize()}` - XSS protection function

### Template Processing

**How templates are used:**

1. **Login Form:**
   - User reaches OTP step in authentication flow
   - Authenticator calls `context.challenge(this.challenge(context, null))`
   - `createLoginForm()` returns `form.createForm("login-email-otp.ftl")`
   - Keycloak renders template with current context

2. **Email Templates:**
   - Authenticator calls `EmailTemplateProvider.send()`
   - Keycloak finds both HTML and text versions
   - Email clients receive multipart message (HTML + text fallback)
   - Variables (otp, ttl) are injected into templates

### Customization Points

**CSS Styling:**
- All CSS classes come from `${properties.kcXxxClass!}` variables
- Customize by modifying Keycloak theme CSS files

**Internationalization:**
- All user text uses `${msg("key")}` function
- Add translations in message properties files

**Layout:**
- Uses standard Keycloak layouts for consistency
- Can be overridden for custom branding

**Responsive Design:**
- Keycloak's base templates are mobile-responsive
- Your OTP form inherits this behavior

This template system ensures your OTP authentication looks consistent with the rest of Keycloak while providing customization options for branding and internationalization.

---

## 10. Internationalization (Message Properties)

These files provide **multi-language support** for your Keycloak plugin. Each language has its own `.properties` file with translated text.

### File Structure

**Naming Convention:**
- `messages_en.properties` - English (default)
- `messages_fr.properties` - French  
- `messages_de.properties` - German
- `messages_es.properties` - Spanish
- And 20+ more languages...

### Message Categories

**1. Form Labels & Buttons (User Interface):**

**English (`messages_en.properties`):**
```properties
loginEmailOtp=Your access code
doResendEmail=Resend email
errorInvalidEmailOtp=Invalid code
errorExpiredEmailOtp=Code expired, we have sent you a new one, please check your email
```

**French (`messages_fr.properties`):**
```properties
loginEmailOtp=Votre code d'accès
doResendEmail=Renvoyer l'e-mail
errorInvalidEmailOtp=Code invalide
errorExpiredEmailOtp=Code expiré, nous vous en avons envoyé un nouveau, veuillez vérifier votre e-mail
```

**German (`messages_de.properties`):**
```properties
loginEmailOtp=Ihr Zugangscode
doResendEmail=E-Mail erneut senden
errorInvalidEmailOtp=Ungültiger Code
errorExpiredEmailOtp=Code abgelaufen, wir haben Ihnen einen neuen gesendet, bitte überprüfen Sie Ihre E-Mail
```

**2. Email Content (Communication):**

**Email Subject & Body:**
```properties
emailOtpSubject=Your access code
emailOtpYourAccessCode=Your access code is:
emailOtpExpiration=The code will expire in {0} minutes.
```

**Parameterized Messages:**
- `{0}` is a placeholder for dynamic values
- `emailOtpExpiration=The code will expire in {0} minutes.`
- In code: `msg("emailOtpExpiration", (ttl / 60)?int)` → "The code will expire in 10 minutes."

**3. Admin UI (Configuration Interface):**
```properties
email-otp-form-display-name=Email OTP
email-otp-form-help-text=Send a one-time code to your email address.
```
- These appear in Keycloak's admin console
- Help administrators understand what your authenticator does

### How Internationalization Works

**1. Template Usage:**
```freemarker
<!-- In login-email-otp.ftl -->
<label for="email-otp">${msg("loginEmailOtp")}</label>
<button name="resend-email">${msg("doResendEmail")}</button>
```

**2. Java Code Usage:**
```java
// In EmailOTPFormAuthenticator.java
context.failureChallenge(
    AuthenticationFlowError.INVALID_CREDENTIALS,
    this.challenge(context, "errorInvalidEmailOtp", OTP_FORM_CODE_INPUT_NAME)
);
```

**3. Email Templates:**
```freemarker
<!-- In otp-email.ftl -->
<p>${msg("emailOtpYourAccessCode")}</p>
<h1>${otp}</h1>
<p>${msg("emailOtpExpiration", (ttl / 60)?int)}</p>
```

### Language Selection Process

**1. User's Browser Language:**
- Keycloak reads `Accept-Language` HTTP header
- Chooses best matching language file
- Falls back to English if language not available

**2. Realm Settings:**
- Admin can configure default languages per realm
- Can force specific language regardless of user preference

**3. Fallback Chain:**
```
User requests German (de) → messages_de.properties
If key missing in German → messages_en.properties  
If key missing in English → Keycloak's built-in messages
If still missing → Display the key name itself
```

### Message Key Patterns

**Error Messages:**
- Pattern: `error[Context][SpecificError]`
- Examples: `errorInvalidEmailOtp`, `errorExpiredEmailOtp`

**Action Labels:**
- Pattern: `do[Action]`
- Examples: `doResendEmail`, `doLogIn` (built-in Keycloak)

**Email Content:**
- Pattern: `email[Component][Purpose]`
- Examples: `emailOtpSubject`, `emailOtpYourAccessCode`

**Admin Display:**
- Pattern: `[component-name]-display-name` / `[component-name]-help-text`
- Examples: `email-otp-form-display-name`

### Adding New Languages

**To support a new language (e.g., Spanish):**

1. **Create file:** `messages_es.properties`
2. **Translate all keys:**
```properties
# Spanish translations
loginEmailOtp=Su código de acceso
doResendEmail=Reenviar correo electrónico
errorInvalidEmailOtp=Código inválido
# ... etc
```
3. **No code changes needed** - Keycloak automatically detects the file

### Character Encoding

**Important:** All `.properties` files must be **UTF-8 encoded** to support international characters (accents, umlauts, etc.).

**Example of proper encoding:**
```properties
# French with accents
emailOtpExpiration=Le code expirera dans {0} minutes.
# German with umlauts  
errorExpiredEmailOtp=Code abgelaufen, wir haben Ihnen einen neuen gesendet
```

This internationalization system ensures your OTP authenticator works seamlessly for users worldwide, automatically adapting to their preferred language without any additional configuration.

---

## Complete Plugin Architecture Overview

Your plugin is a comprehensive **email-based OTP authentication system** for Keycloak with these key components:

### **Core Authentication System:**
1. **EmailOTPFormAuthenticatorFactory** - Configuration and creation of authenticator instances
2. **EmailOTPFormAuthenticator** - Main business logic for OTP generation, validation, and email sending
3. **Conditional Authenticators** - Smart logic to determine when OTP should be required based on user roles

### **Support Systems:**
4. **ConfigHelper** - Utility class for reading admin configuration safely
5. **TwilioEmailService** - SendGrid integration for reliable email delivery
6. **REST API** - Complete programmatic interface for mobile apps and external systems

### **User Interface & Experience:**
7. **FreeMarker Templates** - Web forms and email layouts that users see
8. **Internationalization** - 25+ language support for global users
9. **META-INF Service Registration** - The "magic" that makes Keycloak discover your plugin

### **Key Features Your Plugin Provides:**

**For End Users:**
- Secure email-based authentication
- User-friendly web forms with resend functionality
- Localized experience in their preferred language
- Mobile-friendly responsive design

**For Administrators:**
- Role-based conditional authentication
- Configurable OTP length, alphabet, and expiration
- Comprehensive logging and error handling
- Easy installation via single JAR file

**For Developers:**
- REST API for mobile apps
- OAuth 2.0 integration ready
- SMS OTP foundation (extensible)
- Clean, maintainable code architecture

This is a production-ready plugin that demonstrates excellent software engineering practices - security-first design, comprehensive error handling, internationalization, and extensible architecture. As a DevOps engineer, you can confidently deploy this knowing it follows enterprise-grade patterns and provides the monitoring/logging capabilities you need for operations.

### **Development Best Practices Demonstrated:**

1. **Security:** SecureRandom for OTP generation, XSS protection, session management
2. **Reliability:** Comprehensive error handling and logging throughout
3. **Maintainability:** Clean separation of concerns, utility classes, configuration abstraction
4. **Extensibility:** Plugin architecture, REST API, conditional logic framework
5. **Operations:** Environment-based configuration, detailed logging, graceful error handling
6. **User Experience:** Internationalization, responsive design, accessibility features

### **Deployment Considerations for DevOps:**

**Environment Variables Required:**
```bash
SENDGRID_API_KEY=your-sendgrid-api-key
SENDGRID_FROM_EMAIL=noreply@yourdomain.com
SENDGRID_FROM_NAME=Your App Name
OTP_LENGTH=6  # Optional, defaults to 6
```

**Installation Steps:**
1. Build JAR: `mvn clean package`
2. Copy to Keycloak: `cp target/email-otp-authenticator-*.jar /opt/keycloak/providers/`
3. Restart Keycloak
4. Configure authentication flows in admin UI
5. Test with different user scenarios

**Monitoring Points:**
- SendGrid email delivery status
- OTP generation and validation rates
- Authentication session timeouts
- Error rates by type (invalid OTP, expired codes, etc.)
- User flow completion rates

This plugin represents a solid foundation for enterprise authentication requirements with clear paths for customization and extension.