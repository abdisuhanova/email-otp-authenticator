# Keycloak Email OTP Authentication API Plugin

This Keycloak plugin extends the existing Email OTP Authenticator with REST API endpoints for sending and verifying OTP codes via Twilio SendGrid.

## Overview

The plugin adds two REST API endpoints to your Keycloak realm:

1. **Send OTP API** - Sends an OTP code to an email address for login or signup
2. **Verify OTP API** - Verifies the OTP code and updates email verification status

## API Endpoints

### Base URL
```
https://your-keycloak-domain/realms/{realm-name}/email-otp-api
```

### 1. Send OTP Code

**Endpoint:** `POST /realms/{realm-name}/email-otp-api/send`

**Request Body:**
```json
{
  "email": "user@example.com",
  "method": "login"
}
```

**Parameters:**
- `email` (required): User's email address
- `method` (required): Either "login" or "signup"

**Response:**
```json
{
  "success": true,
  "message": "OTP sent successfully",
  "data": {
    "sessionId": "auth-session-id-12345"
  }
}
```

**Behavior:**
- For `method: "login"`: Checks if user exists in Keycloak. Returns error if user not found.
- For `method: "signup"`: Checks if user already exists. Creates new user in Keycloak if not found.
- Generates a 6-character OTP code using secure random
- Stores OTP in Keycloak authentication session with 10-minute expiration
- Sends OTP via Twilio SendGrid

### 2. Verify OTP Code

**Endpoint:** `POST /realms/{realm-name}/email-otp-api/verify`

**Request Body:**
```json
{
  "email": "user@example.com",
  "code": "ABC123",
  "sessionId": "auth-session-id-12345"
}
```

**Parameters:**
- `email` (required): User's email address
- `code` (required): 6-character OTP code
- `sessionId` (required): Session ID returned from send OTP call

**Response:**
```json
{
  "success": true,
  "message": "OTP verified successfully",
  "data": null
}
```

**Behavior:**
- Validates OTP code against stored record in Keycloak session
- Checks expiration (10-minute timeout)
- Sets `emailVerified = true` for the user in Keycloak upon successful verification
- Removes OTP record from session after verification

## Installation

### 1. Build the Plugin

```bash
# Build for the default Keycloak version (26.2.0)
mvn clean package

# Or build for a specific Keycloak version
mvn clean package -Pkeycloak-25.0.6
```

### 2. Deploy to Keycloak

#### Option A: Docker Installation
Add to your Dockerfile:
```dockerfile
# Copy the plugin JAR
COPY target/email-otp-authenticator-*.jar /opt/keycloak/providers/
```

#### Option B: Manual Installation
1. Copy the JAR file to your Keycloak `providers` directory:
```bash
cp target/email-otp-authenticator-*.jar /path/to/keycloak/providers/
```

2. Restart Keycloak:
```bash
/path/to/keycloak/bin/kc.sh start
```

## Configuration

### 1. Twilio SendGrid Setup

#### Create SendGrid Account
1. Sign up at [SendGrid](https://sendgrid.com)
2. Verify your sender email address
3. Generate an API key

#### Set Environment Variables
Configure these environment variables for your Keycloak instance:

```bash
export SENDGRID_API_KEY="your-sendgrid-api-key"
export SENDGRID_FROM_EMAIL="noreply@yourcompany.com"
export SENDGRID_FROM_NAME="Your Company Name"
```

#### Docker Environment
For Docker deployments, add to your docker-compose.yml:
```yaml
services:
  keycloak:
    image: keycloak/keycloak:26.2.0
    environment:
      - SENDGRID_API_KEY=your-sendgrid-api-key
      - SENDGRID_FROM_EMAIL=noreply@yourcompany.com
      - SENDGRID_FROM_NAME=Your Company Name
    volumes:
      - ./email-otp-authenticator.jar:/opt/keycloak/providers/email-otp-authenticator.jar
```

#### Kubernetes Deployment
For Kubernetes, create a secret:
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: sendgrid-credentials
type: Opaque
stringData:
  SENDGRID_API_KEY: "your-sendgrid-api-key"
  SENDGRID_FROM_EMAIL: "noreply@yourcompany.com"
  SENDGRID_FROM_NAME: "Your Company Name"
```

Then reference in your deployment:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: keycloak
spec:
  template:
    spec:
      containers:
      - name: keycloak
        image: keycloak/keycloak:26.2.0
        envFrom:
        - secretRef:
            name: sendgrid-credentials
```

### 2. Verify Installation

Check Keycloak logs for successful plugin loading:
```
INFO [org.keycloak.services] (ServerService Thread Pool -- 55) KC-SERVICES0001: Loading SPI realm-restapi-extension
INFO [ch.jacem.for_keycloak.email_otp_authenticator.service.TwilioEmailService] Twilio SendGrid initialized successfully
```

## Testing the APIs

### Using cURL

**Send OTP for Login:**
```bash
curl -X POST "https://your-keycloak-domain/realms/your-realm/email-otp-api/send" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "method": "login"
  }'
```

**Send OTP for Signup:**
```bash
curl -X POST "https://your-keycloak-domain/realms/your-realm/email-otp-api/send" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@example.com", 
    "method": "signup"
  }'
```

**Verify OTP:**
```bash
curl -X POST "https://your-keycloak-domain/realms/your-realm/email-otp-api/verify" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "code": "ABC123",
    "sessionId": "session-id-from-send-response"
  }'
```

### Using JavaScript/Frontend

```javascript
// Send OTP
const sendOTPResponse = await fetch('/realms/your-realm/email-otp-api/send', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    email: 'user@example.com',
    method: 'login'
  })
});

const sendResult = await sendOTPResponse.json();
console.log('Session ID:', sendResult.data.sessionId);

// Verify OTP
const verifyOTPResponse = await fetch('/realms/your-realm/email-otp-api/verify', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    email: 'user@example.com',
    code: 'ABC123',
    sessionId: sendResult.data.sessionId
  })
});

const verifyResult = await verifyOTPResponse.json();
console.log('Verification result:', verifyResult);
```

## Integration with Keycloak Users

### User Management
- **Login**: Uses existing Keycloak users
- **Signup**: Creates new users in Keycloak with:
  - Email as username
  - `emailVerified = false` initially
  - `enabled = true`

### Email Verification
- Upon successful OTP verification, sets `user.emailVerified = true`
- Integrates with Keycloak's built-in email verification system

### User Attributes
The plugin works with standard Keycloak user attributes:
- `email`: User's email address
- `emailVerified`: Boolean flag for email verification status
- `enabled`: User account status

## Security Considerations

1. **HTTPS Only**: Always use HTTPS in production
2. **Rate Limiting**: Implement rate limiting at the reverse proxy level
3. **Session Management**: OTPs are stored in Keycloak authentication sessions
4. **Expiration**: OTPs expire after 10 minutes
5. **Secure Random**: Uses Java SecureRandom for OTP generation
6. **One-time Use**: OTPs are deleted after successful verification or expiration

## Error Handling

Common error responses:

```json
{
  "success": false,
  "message": "User not found",
  "data": null
}
```

```json
{
  "success": false,
  "message": "SendGrid credentials not configured",
  "data": null
}
```

```json
{
  "success": false,
  "message": "OTP has expired",
  "data": null
}
```

## Monitoring and Logging

The plugin logs important events at these levels:

- **INFO**: Successful operations, SendGrid initialization
- **WARN**: Missing configuration
- **ERROR**: Email sending failures, verification errors
- **DEBUG**: Detailed OTP operations (if debug logging enabled)

### Enable Debug Logging
Add to your Keycloak configuration:
```bash
export QUARKUS_LOG_LEVEL=DEBUG
export QUARKUS_LOG_CATEGORY_CH_JACEM_FOR_KEYCLOAK_EMAIL_OTP_AUTHENTICATOR_LEVEL=DEBUG
```

## Troubleshooting

### Common Issues

1. **Plugin Not Loading**
   - Check if JAR is in the correct `providers` directory
   - Verify Keycloak has been restarted
   - Check for Java version compatibility

2. **SendGrid Errors**
   - Verify SENDGRID_API_KEY is correct
   - Check if sender email is verified in SendGrid
   - Review SendGrid account limits

3. **User Creation Issues**
   - Ensure realm allows user registration
   - Check email format validation
   - Verify duplicate email handling

4. **Session Errors**
   - Session IDs have limited lifetime
   - Use session ID immediately after receiving it
   - Check for session cleanup in Keycloak logs

## Compatibility

- **Keycloak Versions**: 24.0.5, 25.0.6, 26.0.8, 26.1.5, 26.2.0
- **Java**: 11 or higher
- **SendGrid**: All current API versions

## Existing Authenticator Integration

This plugin works alongside the existing Keycloak Email OTP Authenticator:
- **REST APIs**: Use the new endpoints for programmatic access
- **Authentication Flow**: Use the existing authenticator for Keycloak login flows
- **Configuration**: Both use the same OTP generation logic and character set

## Support and Development

### Building for Different Keycloak Versions
```bash
mvn clean package -Pkeycloak-26.2.0
mvn clean package -Pkeycloak-25.0.6
mvn clean package -Pkeycloak-24.0.5
```

### Testing with Docker Compose
```bash
# Build plugin
mvn clean package

# Start Keycloak with plugin
docker-compose up

# Access Keycloak at http://localhost:8080
# Test APIs at http://localhost:8080/realms/master/email-otp-api/send
```

For issues and questions:
1. Check Keycloak and plugin logs
2. Verify SendGrid configuration and account status
3. Test API endpoints with proper realm name
4. Confirm user exists (for login) or doesn't exist (for signup)