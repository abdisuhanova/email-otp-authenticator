# 🔐 Comprehensive OTP Authentication API

Complete OTP system supporting both **Email** and **SMS** verification with **OAuth integration**.

## 📱 **Complete Mobile App Flow**

### **Step 1: Send OTP (Email or SMS)**

#### **Email OTP**
```bash
POST /realms/{realm}/email-otp-api/otp/send/email
{
  "email": "user@example.com"
}
```

**Response:**
```json
{
  "success": true,
  "message": "OTP sent to email successfully",
  "data": {
    "sessionId": "session-abc123",
    "type": "email",
    "expirySeconds": 600
  }
}
```

#### **SMS OTP**
```bash
POST /realms/{realm}/email-otp-api/otp/send/sms
{
  "phoneNumber": "+1234567890"
}
```

**Response:**
```json
{
  "success": true,
  "message": "OTP sent to phone successfully",
  "data": {
    "sessionId": "session-xyz789",
    "type": "sms", 
    "expirySeconds": 600,
    "maskedPhone": "+12****90"
  }
}
```

### **Step 2: Verify OTP & Get OAuth Code**

```bash
POST /realms/{realm}/email-otp-api/otp/login/verify-code
{
  "sessionId": "session-abc123",
  "otpCode": "123456"
}
```

**Response:**
```json
{
  "success": true,
  "message": "OTP verified successfully. Use authorization code to get access token.",
  "data": {
    "authorizationCode": "oauth-code-xyz123abc",
    "userId": "user-id-123",
    "email": "user@example.com",
    "otpType": "email",
    "expiresIn": 300
  }
}
```

### **Step 3: Exchange OAuth Code for Access Token**

```bash
POST /realms/{realm}/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code
&code=oauth-code-xyz123abc
&client_id=your-mobile-app
&client_secret=your-client-secret
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIs...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "refresh_token": "eyJhbGciOiJIUzI1NiIs..."
}
```

## 🛠️ **API Endpoints Summary**

| Endpoint | Method | Purpose | Returns |
|----------|--------|---------|---------|
| `/otp/send/email` | POST | Send email OTP | sessionId |
| `/otp/send/sms` | POST | Send SMS OTP | sessionId |
| `/otp/login/verify-code` | POST | Verify OTP | OAuth code |
| `/login` | POST | Legacy email-only login | Direct auth |

## 🔧 **Configuration**

### **Environment Variables**

```bash
# SendGrid Email
export SENDGRID_API_KEY="your-sendgrid-key"
export SENDGRID_FROM_EMAIL="noreply@yourapp.com"
export SENDGRID_FROM_NAME="Your App"

# OTP Settings
export OTP_LENGTH="6"  # 4-10 digits

# SMS Integration (TODO: Add Twilio SMS)
export TWILIO_ACCOUNT_SID="your-twilio-sid"
export TWILIO_AUTH_TOKEN="your-twilio-token"
export TWILIO_FROM_PHONE="+1234567890"
```

### **User Requirements**

#### **For Email OTP:**
- User must exist in Keycloak
- User must have `email` attribute set
- User must be enabled

#### **For SMS OTP:**
- User must exist in Keycloak  
- User must have `phoneNumber` attribute set
- User must be enabled

## 📱 **Mobile App Implementation**

### **React Native / Flutter Example**

```javascript
class OTPAuth {
  async sendEmailOTP(email) {
    const response = await fetch('/otp/send/email', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email })
    });
    
    const result = await response.json();
    if (result.success) {
      this.sessionId = result.data.sessionId;
      return result;
    }
    throw new Error(result.message);
  }
  
  async sendSMSOTP(phoneNumber) {
    const response = await fetch('/otp/send/sms', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phoneNumber })
    });
    
    const result = await response.json();
    if (result.success) {
      this.sessionId = result.data.sessionId;
      return result;
    }
    throw new Error(result.message);
  }
  
  async verifyOTP(otpCode) {
    const response = await fetch('/otp/login/verify-code', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sessionId: this.sessionId,
        otpCode: otpCode
      })
    });
    
    const result = await response.json();
    if (result.success) {
      return await this.exchangeCodeForToken(result.data.authorizationCode);
    }
    throw new Error(result.message);
  }
  
  async exchangeCodeForToken(authCode) {
    // Exchange OAuth code for access token
    const response = await fetch('/realms/your-realm/protocol/openid-connect/token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        grant_type: 'authorization_code',
        code: authCode,
        client_id: 'your-mobile-app'
      })
    });
    
    return await response.json();
  }
}
```

## 🔐 **Security Features**

### **Session Security**
- ✅ **Unique Sessions**: Each OTP request creates new session
- ✅ **Session Expiry**: 10-minute session timeout
- ✅ **One-time Use**: Sessions cleaned after successful verification
- ✅ **Type Binding**: Email/SMS OTPs bound to specific session types

### **OTP Security**
- ✅ **Secure Generation**: Cryptographically secure random
- ✅ **Numeric Only**: Easy to enter, harder to confuse
- ✅ **Configurable Length**: 4-10 digits via environment variable
- ✅ **Expiration**: 10-minute timeout (configurable)

### **OAuth Security**
- ✅ **Authorization Code Flow**: Standard OAuth 2.0 flow
- ✅ **Short-lived Codes**: 5-minute expiry for auth codes
- ✅ **Session Binding**: Auth codes tied to verified sessions

## 🚨 **Error Handling**

### **Common Errors**

```json
// User not found
{
  "success": false,
  "message": "User not found"
}

// Invalid session
{
  "success": false,
  "message": "Invalid or expired session ID"
}

// Expired OTP
{
  "success": false,
  "message": "Invalid or expired OTP code"
}

// SMS not configured
{
  "success": false,
  "message": "SMS service not available"
}
```

## 📞 **SMS Integration (TODO)**

The SMS functionality is implemented but requires Twilio SMS integration:

```java
// Add to TwilioEmailService or create TwilioSMSService
public void sendSMS(String phoneNumber, String message) {
    Message.creator(
        new PhoneNumber(phoneNumber),
        new PhoneNumber(twilioFromPhone),
        message
    ).create();
}
```

## 🔄 **Migration from Old System**

### **Old System (Legacy)**
```bash
POST /send → sessionId
POST /verify → success
```

### **New System**
```bash
POST /otp/send/email → sessionId  
POST /otp/login/verify-code → OAuth code
```

**Both systems coexist** - no breaking changes to existing integrations.

## 🧪 **Testing**

### **Test Email OTP Flow**
```bash
# Step 1: Send email OTP
curl -X POST "http://localhost:8080/realms/master/email-otp-api/otp/send/email" \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com"}'

# Step 2: Verify OTP (check email for code)
curl -X POST "http://localhost:8080/realms/master/email-otp-api/otp/login/verify-code" \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId":"session-from-step1",
    "otpCode":"123456"
  }'
```

## 🏗️ **Architecture**

```
Mobile App
    ↓
[Send OTP API] → Email/SMS → User
    ↓
[User enters OTP]
    ↓  
[Verify API] → OAuth Code
    ↓
[Token Exchange] → Access Token
    ↓
[Authenticated]
```

This provides a **complete, secure, OAuth-compliant** authentication system for your mobile application!