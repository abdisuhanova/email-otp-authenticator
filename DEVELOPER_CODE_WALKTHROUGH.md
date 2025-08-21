# Developer Code Walkthrough - Learn to Code with Real Examples

## Table of Contents
1. [Introduction for Aspiring Developers](#introduction-for-aspiring-developers)
2. [Maven Build File (pom.xml) - Line by Line](#maven-build-file-pomxml---line-by-line)
3. [Factory Pattern Implementation](#factory-pattern-implementation)
4. [Main Authenticator Logic](#main-authenticator-logic)
5. [Helper Classes and Utilities](#helper-classes-and-utilities)
6. [REST API Implementation](#rest-api-implementation)
7. [Email Service Integration](#email-service-integration)
8. [Template System](#template-system)
9. [Configuration Management](#configuration-management)
10. [Programming Concepts You'll Learn](#programming-concepts-youll-learn)

---

## Introduction for Aspiring Developers

This document will teach you programming by examining **real production code**. Instead of simple "Hello World" examples, you'll learn from a complete, working system that handles authentication, email sending, REST APIs, and more.

### What You'll Learn:
- **Java Programming** - Object-oriented concepts, inheritance, interfaces
- **Design Patterns** - Factory, Singleton, Strategy patterns in action
- **Web Development** - REST APIs, HTTP methods, JSON handling
- **Database Concepts** - Session management, data persistence
- **Security Practices** - Input validation, secure random generation
- **Software Architecture** - How large applications are structured
- **Enterprise Integration** - How systems communicate with each other

---

## Maven Build File (pom.xml) - Line by Line

Maven is a **build tool** that manages dependencies and compiles your Java project. Think of it as a recipe that tells the computer how to build your application.

```xml
<?xml version="1.0" encoding="UTF-8"?>
```
**Line 1**: This tells the computer "this file is in XML format, using UTF-8 character encoding"
- **XML**: A way to structure data using tags like `<name>value</name>`
- **UTF-8**: Character encoding that supports international characters (é, ñ, 中文, etc.)

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" 
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
```
**Lines 2-4**: Defines the XML namespace and schema
- **xmlns**: "XML namespace" - tells what type of XML this is
- **schemaLocation**: Points to a file that defines the rules for this XML format
- Think of this as saying "I'm writing a Maven project file, here are the rules"

```xml
<modelVersion>4.0.0</modelVersion>
```
**Line 5**: Specifies which version of Maven's project structure to use
- Maven has evolved over time, version 4.0.0 is the current standard

```xml
<groupId>ch.jacem.for_keycloak</groupId>
<artifactId>email_otp_authenticator</artifactId>
<version>1.1.4</version>
```
**Lines 6-8**: Project identification (like a mailing address for your code)
- **groupId**: Organization/company identifier (usually reverse domain name)
- **artifactId**: Project name (what this specific project does)
- **version**: Current version of your project (helps track changes)

```xml
<packaging>jar</packaging>
```
**Line 9**: Output format
- **jar**: "Java Archive" - bundles all your code into one file
- Other options: `war` (web applications), `pom` (parent projects)

```xml
<properties>
    <java.version>11</java.version>
    <maven.compiler.source>${java.version}</maven.compiler.source>
    <maven.compiler.target>${java.version}</maven.compiler.target>
</properties>
```
**Lines 28-32**: Configuration variables
- **java.version**: Which version of Java to use (Java 11 has modern features)
- **compiler.source/target**: Tell Maven to compile for Java 11
- `${}` syntax means "use the value of this variable"

```xml
<keycloak.version>26.2.0</keycloak.version>
```
**Line 36**: Defines which version of Keycloak this plugin works with
- Different Keycloak versions have different APIs
- This ensures compatibility

**Programming Concept - Variables:**
```java
// In programming, variables store values
String javaVersion = "11";  // Like <java.version>11</java.version>
int keycloakVersion = 26;   // Numbers for versions
```

**Dependency Management:**
```xml
<dependencies>
    <dependency>
        <groupId>org.keycloak</groupId>
        <artifactId>keycloak-server-spi</artifactId>
        <version>${keycloak.version}</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```
**Lines 80-86**: External libraries your project needs
- **groupId/artifactId**: Which library to download
- **version**: Which version of that library
- **scope>provided**: "Keycloak will provide this library at runtime, don't bundle it"

**Real-world analogy**: Like a recipe saying "you need flour, but the kitchen already has it"

---

## Factory Pattern Implementation

### EmailOTPFormAuthenticatorFactory.java - Line by Line

```java
package ch.jacem.for_keycloak.email_otp_authenticator;
```
**Line 1**: Package declaration
- **Package**: Groups related classes together (like folders for files)
- **Naming convention**: Reverse domain name ensures uniqueness globally
- Think of it as your class's "address" in the codebase

```java
import java.util.Arrays;
import java.util.List;
```
**Lines 3-4**: Import statements
- **Import**: Tells Java "I want to use classes from other packages"
- `java.util.Arrays`: Utility methods for working with arrays
- `java.util.List`: Interface for ordered collections of items

**Programming Concept - Collections:**
```java
// Arrays store multiple values
String[] fruits = {"apple", "banana", "orange"};

// Lists are more flexible than arrays
List<String> fruits = Arrays.asList("apple", "banana", "orange");
```

```java
import org.keycloak.Config.Scope;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
```
**Lines 6-8**: Import Keycloak-specific classes
- These are interfaces and classes that Keycloak provides
- Your code will implement these interfaces to integrate with Keycloak

```java
public class EmailOTPFormAuthenticatorFactory implements AuthenticatorFactory {
```
**Line 14**: Class declaration with interface implementation
- **public**: Other classes can use this class
- **class**: Defines a blueprint for creating objects
- **implements**: This class promises to provide all methods defined in AuthenticatorFactory interface

**Programming Concept - Interfaces:**
```java
// An interface is like a contract
interface AuthenticatorFactory {
    Authenticator create(KeycloakSession session);  // Must implement this
    String getId();                                  // Must implement this
}

// Your class signs the contract
public class EmailOTPFormAuthenticatorFactory implements AuthenticatorFactory {
    // Now you MUST provide these methods
}
```

```java
public final static String PROVIDER_ID = "email-otp-form";
private final static EmailOTPFormAuthenticator SINGLETON = new EmailOTPFormAuthenticator();
```
**Lines 15-16**: Class constants
- **public static final**: A constant that never changes, shared by all instances
- **private static final**: A constant only this class can see
- **SINGLETON pattern**: Only one instance of EmailOTPFormAuthenticator exists

**Programming Concepts - Static vs Instance:**
```java
public class Calculator {
    public static final double PI = 3.14159;  // Shared by all calculators
    private double currentValue = 0;           // Each calculator has its own
    
    public static double square(double x) {    // Can call without creating calculator
        return x * x;
    }
    
    public void add(double x) {                // Need a calculator instance
        currentValue += x;
    }
}

// Usage:
double area = Calculator.PI * Calculator.square(5);  // Static access
Calculator calc = new Calculator();                  // Create instance
calc.add(10);                                        // Instance access
```

```java
public static final String SETTINGS_KEY_USER_ROLE = "user-role";
public static final String SETTINGS_DEFAULT_VALUE_USER_ROLE = null;
```
**Lines 18-19**: Configuration constants
- **Naming convention**: `SETTINGS_KEY_*` for configuration keys, `SETTINGS_DEFAULT_VALUE_*` for defaults
- These constants prevent typos when referencing configuration settings

**Programming Concept - Constants vs Magic Numbers:**
```java
// Bad - "magic numbers/strings"
if (user.getRole().equals("admin-role")) {  // What if you mistype later?
    // ...
}

// Good - named constants
public static final String ADMIN_ROLE_KEY = "admin-role";
if (user.getRole().equals(ADMIN_ROLE_KEY)) {  // Clear intent, no typos
    // ...
}
```

```java
@Override
public Authenticator create(KeycloakSession session) {
    return SINGLETON;
}
```
**Lines 29-32**: Method implementation
- **@Override**: Annotation telling compiler "this method implements interface method"
- **public**: Other classes can call this method
- **Authenticator**: Return type (what this method gives back)
- **create**: Method name
- **KeycloakSession session**: Parameter (input to this method)
- **return SINGLETON**: Give back the single instance

**Programming Concept - Methods:**
```java
// Method structure:
[access modifier] [return type] [method name]([parameters]) {
    // method body
    return [value of return type];
}

// Examples:
public String getName() {           // Returns a string, takes no parameters
    return this.name;
}

public int add(int a, int b) {      // Returns int, takes two int parameters
    return a + b;
}

public void printHello() {          // Returns nothing (void), no parameters
    System.out.println("Hello!");
}
```

```java
@Override
public void init(Scope config) {}

@Override  
public void postInit(KeycloakSessionFactory factory) {}

@Override
public void close() {}
```
**Lines 34-41**: Lifecycle methods
- **void**: These methods don't return anything
- **Empty body {}**: No implementation needed for this plugin
- These are called by Keycloak during startup and shutdown

```java
@Override
public String getId() {
    return PROVIDER_ID;
}
```
**Lines 43-46**: Unique identifier
- Returns the string constant defined earlier
- Keycloak uses this ID to identify your authenticator in the admin UI

```java
@Override
public List<ProviderConfigProperty> getConfigProperties() {
    return Arrays.asList(
        new ProviderConfigProperty(
            SETTINGS_KEY_USER_ROLE,                    // Internal key
            "User Role",                               // Display name
            "The OTP will only be required...",       // Help text
            ProviderConfigProperty.ROLE_TYPE,         // Input type
            SETTINGS_DEFAULT_VALUE_USER_ROLE           // Default value
        ),
        // ... more properties
    );
}
```
**Lines 78-119**: Configuration UI definition
- **List<ProviderConfigProperty>**: Returns a list of configuration options
- **Arrays.asList()**: Creates a list from individual items
- **new ProviderConfigProperty()**: Creates a configuration field for the admin UI

**Programming Concept - Constructor Parameters:**
```java
// A constructor creates new objects
public class Person {
    private String name;
    private int age;
    
    // Constructor with parameters
    public Person(String name, int age) {
        this.name = name;  // "this" refers to the object being created
        this.age = age;
    }
}

// Usage:
Person john = new Person("John", 25);  // Calls constructor
```

---

## Main Authenticator Logic

### EmailOTPFormAuthenticator.java - Core Methods

```java
public class EmailOTPFormAuthenticator extends AbstractUsernameFormAuthenticator 
    implements AcceptsFullContextInConfiguredFor
```
**Line 31**: Class inheritance and interface implementation
- **extends**: Inherits functionality from AbstractUsernameFormAuthenticator
- **implements**: Promises to implement AcceptsFullContextInConfiguredFor methods

**Programming Concept - Inheritance:**
```java
// Parent class (superclass)
public class Vehicle {
    protected String brand;
    protected int year;
    
    public void start() {
        System.out.println("Vehicle starting...");
    }
}

// Child class (subclass)
public class Car extends Vehicle {
    private int doors;
    
    @Override
    public void start() {
        System.out.println("Car engine starting...");  // Overrides parent behavior
    }
    
    public void honk() {
        System.out.println("Beep beep!");  // New method only cars have
    }
}

// Usage:
Car myCar = new Car();
myCar.start();  // Calls Car's version
myCar.honk();   // Car-specific method
```

```java
public static final String AUTH_NOTE_OTP_KEY = "for-kc-email-otp-key";
public static final String AUTH_NOTE_OTP_CREATED_AT = "for-kc-email-otp-created-at";
```
**Lines 33-34**: Session storage keys
- These constants define where to store the OTP code and creation time in the user's session
- **Session**: Temporary storage that exists while user is logging in

```java
@Override
public void authenticate(AuthenticationFlowContext context) {
    this.generateOtp(context, false);
    
    context.challenge(
        this.challenge(context, null)
    );
}
```
**Lines 124-131**: Main authentication entry point
- **authenticate()**: Called when user reaches this authentication step
- **this.generateOtp()**: Calls the OTP generation method
- **context.challenge()**: Shows the login form to the user

**Programming Concept - Method Calls:**
```java
public class Calculator {
    public int add(int a, int b) {
        return a + b;
    }
    
    public int multiply(int a, int b) {
        return a * b;
    }
    
    public int calculate(int x, int y) {
        int sum = this.add(x, y);        // Call add method
        int result = this.multiply(sum, 2); // Call multiply method
        return result;
    }
}
```

```java
@Override
public void action(AuthenticationFlowContext context) {
    MultivaluedMap<String, String> inputData = context.getHttpRequest().getDecodedFormParameters();
    AuthenticationSessionModel authenticationSession = context.getAuthenticationSession();
```
**Lines 45-48**: Handle form submission
- **action()**: Called when user submits the OTP form
- **MultivaluedMap**: Data structure holding form fields (like a dictionary)
- **getDecodedFormParameters()**: Gets the data user typed in the form

**Programming Concept - Data Structures:**
```java
// Map stores key-value pairs
Map<String, String> userData = new HashMap<>();
userData.put("name", "John");
userData.put("email", "john@example.com");

String email = userData.get("email");  // Gets "john@example.com"

// MultivaluedMap allows multiple values per key
MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
formData.add("hobby", "reading");
formData.add("hobby", "swimming");   // Same key, different value

List<String> hobbies = formData.get("hobby");  // Gets ["reading", "swimming"]
```

```java
if (inputData.containsKey(OTP_FORM_RESEND_ACTION_NAME)) {
    logger.debug("Resending a new OTP");
    
    this.generateOtp(context, true);
    
    context.challenge(
        this.challenge(context, null)
    );
    
    return;
}
```
**Lines 67-79**: Handle resend button
- **containsKey()**: Check if user clicked the resend button
- **logger.debug()**: Write a message to the log file for debugging
- **generateOtp(context, true)**: Generate new OTP (true = force regeneration)
- **return**: Exit the method early

**Programming Concept - Conditional Logic:**
```java
public void processOrder(Order order) {
    if (order.getItems().isEmpty()) {
        System.out.println("Cannot process empty order");
        return;  // Exit early
    }
    
    if (order.getCustomer().isPremium()) {
        applyDiscount(order);
    } else {
        applyStandardPricing(order);
    }
    
    processPayment(order);
}
```

```java
String otp = inputData.getFirst(OTP_FORM_CODE_INPUT_NAME);

if (null == otp) {
    context.challenge(
        this.challenge(context, null)
    );
    return;
}
```
**Lines 81-89**: Validate OTP input
- **getFirst()**: Get the value of the OTP input field
- **null == otp**: Check if user didn't enter anything
- Show the form again if no OTP entered

**Programming Concept - Null Checks:**
```java
public void processName(String name) {
    if (name == null) {
        System.out.println("Name cannot be null");
        return;
    }
    
    if (name.isEmpty()) {
        System.out.println("Name cannot be empty");
        return;
    }
    
    // Safe to use name here
    System.out.println("Hello, " + name);
}
```

```java
if (otp.isEmpty() || !otp.equals(authenticationSession.getAuthNote(AUTH_NOTE_OTP_KEY))) {
    context.getEvent().user(user).error(Errors.INVALID_USER_CREDENTIALS);
    context.failureChallenge(
        AuthenticationFlowError.INVALID_CREDENTIALS,
        this.challenge(context, "errorInvalidEmailOtp", OTP_FORM_CODE_INPUT_NAME)
    );
    return;
}
```
**Lines 91-98**: Validate OTP correctness
- **isEmpty()**: Check if OTP is blank
- **!otp.equals()**: Check if entered OTP doesn't match stored OTP
- **||**: Logical OR - if either condition is true
- **getAuthNote()**: Retrieve stored OTP from session
- **failureChallenge()**: Show error message to user

**Programming Concept - Boolean Logic:**
```java
// Logical operators
boolean isAdult = age >= 18;
boolean hasLicense = user.hasDriversLicense();
boolean canDrive = isAdult && hasLicense;  // AND: both must be true

boolean isWeekend = day.equals("Saturday") || day.equals("Sunday");  // OR: either true

boolean isNotEmpty = !text.isEmpty();  // NOT: opposite of isEmpty()

// Complex conditions
if (isLoggedIn && (isAdmin || isOwner) && !isSuspended) {
    allowAccess();
}
```

```java
private String generateOtp(AuthenticationFlowContext context, boolean forceRegenerate) {
    String existingOtp = context.getAuthenticationSession().getAuthNote(AUTH_NOTE_OTP_KEY);
    if (!forceRegenerate && existingOtp != null && !existingOtp.isEmpty() && !this.isOtpExpired(context)) {
        return existingOtp;
    }
```
**Lines 191-196**: Check for existing OTP
- **private**: Only this class can call this method
- **boolean forceRegenerate**: Parameter controlling whether to create new OTP
- **Complex condition**: Only reuse existing OTP if all conditions are met

**Programming Concept - Method Parameters:**
```java
// Methods can have different signatures
public void sendEmail(String recipient) {
    sendEmail(recipient, "Default Subject", "Default Body");
}

public void sendEmail(String recipient, String subject) {
    sendEmail(recipient, subject, "Default Body");
}

public void sendEmail(String recipient, String subject, String body) {
    // Actually send the email
    emailService.send(recipient, subject, body);
}

// Method overloading - same name, different parameters
```

```java
String alphabet = ConfigHelper.getOtpCodeAlphabet(context);
int length = ConfigHelper.getOtpCodeLength(context);

SecureRandom secureRandom = new SecureRandom();
StringBuilder otpBuilder = new StringBuilder(length);
for (int i = 0; i < length; i++) {
    otpBuilder.append(alphabet.charAt(secureRandom.nextInt(alphabet.length())));
}
String otp = otpBuilder.toString();
```
**Lines 198-207**: Generate random OTP
- **ConfigHelper.getOtpCodeAlphabet()**: Get allowed characters from configuration
- **SecureRandom**: Cryptographically secure random number generator
- **StringBuilder**: Efficient way to build strings
- **for loop**: Repeat the code inside brackets
- **charAt()**: Get character at specific position
- **nextInt()**: Generate random number

**Programming Concept - Loops:**
```java
// For loop structure: for (initialization; condition; increment)
for (int i = 0; i < 5; i++) {
    System.out.println("Count: " + i);
}
// Prints: Count: 0, Count: 1, Count: 2, Count: 3, Count: 4

// While loop
int count = 0;
while (count < 3) {
    System.out.println("While count: " + count);
    count++;  // Increment count
}

// For-each loop (for arrays/collections)
String[] names = {"Alice", "Bob", "Charlie"};
for (String name : names) {
    System.out.println("Hello, " + name);
}
```

**Programming Concept - Random Numbers:**
```java
import java.util.Random;
import java.security.SecureRandom;

// Basic random (not secure)
Random random = new Random();
int randomNumber = random.nextInt(10);  // 0 to 9

// Secure random (for passwords, OTPs)
SecureRandom secureRandom = new SecureRandom();
int secureNumber = secureRandom.nextInt(10);  // 0 to 9

// Generate random string
String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
StringBuilder password = new StringBuilder();
for (int i = 0; i < 8; i++) {
    int index = secureRandom.nextInt(chars.length());
    password.append(chars.charAt(index));
}
```

---

## Helper Classes and Utilities

### ConfigHelper.java - Configuration Management

```java
public class ConfigHelper {
    
    public static String getRole(AuthenticatorConfigModel config) {
        return ConfigHelper.getConfigStringValue(
            config,
            EmailOTPFormAuthenticatorFactory.SETTINGS_KEY_USER_ROLE,
            EmailOTPFormAuthenticatorFactory.SETTINGS_DEFAULT_VALUE_USER_ROLE
        );
    }
```
**Lines 8-16**: Public configuration getter
- **static**: Can call without creating ConfigHelper instance
- Method delegates to more general `getConfigStringValue` method
- Uses constants from Factory for key and default value

**Programming Concept - Method Delegation:**
```java
public class MathHelper {
    // Specific methods delegate to general ones
    public static double getCircleArea(double radius) {
        return getArea("circle", radius);
    }
    
    public static double getSquareArea(double side) {
        return getArea("square", side);
    }
    
    // General method does the real work
    private static double getArea(String shape, double measurement) {
        switch (shape) {
            case "circle": return Math.PI * measurement * measurement;
            case "square": return measurement * measurement;
            default: return 0;
        }
    }
}
```

```java
public static String getConfigStringValue(AuthenticatorConfigModel config, String key, String defaultValue) {
    if (null == config || !config.getConfig().containsKey(key)) {
        return defaultValue;
    }
    
    String value = config.getConfig().get(key);
    if (null == value || value.isEmpty()) {
        return defaultValue;
    }
    
    return value;
}
```
**Lines 74-85**: Safe configuration reading with null checks
- **Multiple null checks**: Prevent crashes if configuration is missing
- **Early returns**: Exit method as soon as we know the result
- **Defensive programming**: Assume things might be null or missing

**Programming Concept - Defensive Programming:**
```java
// Bad - crashes if things are null
public void processUser(User user) {
    String email = user.getEmail().toLowerCase();  // Crashes if user or email is null
    sendEmail(email);
}

// Good - handles null cases
public void processUser(User user) {
    if (user == null) {
        logger.warn("User is null, cannot process");
        return;
    }
    
    String email = user.getEmail();
    if (email == null || email.isEmpty()) {
        logger.warn("User has no email address");
        return;
    }
    
    sendEmail(email.toLowerCase());
}
```

```java
public static int getConfigIntValue(AuthenticatorConfigModel config, String key, int defaultValue) {
    if (null == config || !config.getConfig().containsKey(key)) {
        return defaultValue;
    }
    
    String value = config.getConfig().get(key);
    if (null == value || value.isEmpty()) {
        return defaultValue;
    }
    
    try {
        return Integer.parseInt(value);
    } catch (NumberFormatException e) {
        return defaultValue;
    }
}
```
**Lines 91-106**: Safe integer parsing with error handling
- **try-catch**: Handle errors gracefully
- **Integer.parseInt()**: Convert string to integer
- **NumberFormatException**: Thrown when string can't be converted to number

**Programming Concept - Exception Handling:**
```java
// Exceptions are errors that can happen during program execution
public int divide(int a, int b) {
    try {
        return a / b;  // This might throw ArithmeticException if b is 0
    } catch (ArithmeticException e) {
        System.out.println("Cannot divide by zero!");
        return 0;  // Return safe default
    }
}

// Multiple catch blocks
public void readFile(String filename) {
    try {
        String content = Files.readString(Paths.get(filename));
        processContent(content);
    } catch (IOException e) {
        System.out.println("Cannot read file: " + e.getMessage());
    } catch (SecurityException e) {
        System.out.println("Permission denied: " + e.getMessage());
    } catch (Exception e) {
        System.out.println("Unexpected error: " + e.getMessage());
    }
}
```

---

## REST API Implementation

### OTPRestResource.java - HTTP Endpoints

```java
@Path("")
public class OTPRestResource {
```
**Lines 23-24**: JAX-RS REST endpoint class
- **@Path("")**: Defines URL path for this resource
- **JAX-RS**: Java standard for creating REST APIs

```java
@POST
@Path("/send")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public Response sendOTP(OTPSendRequest request) {
```
**Lines 41-45**: REST endpoint definition
- **@POST**: This endpoint accepts HTTP POST requests
- **@Path("/send")**: URL will be `/send`
- **@Consumes**: This endpoint accepts JSON input
- **@Produces**: This endpoint returns JSON output
- **Response**: HTTP response object

**Programming Concept - HTTP Methods:**
```java
// Different HTTP methods for different actions
@GET     // Read data (like getting a user profile)
@POST    // Create new data (like creating a new user)
@PUT     // Update existing data (like changing user email)
@DELETE  // Remove data (like deleting a user)

// RESTful URL design
@GET @Path("/users/{id}")           // GET /users/123
@POST @Path("/users")               // POST /users (create new)
@PUT @Path("/users/{id}")           // PUT /users/123 (update)
@DELETE @Path("/users/{id}")        // DELETE /users/123
```

```java
try {
    RealmModel realm = session.getContext().getRealm();
    
    if (request.email == null || request.email.isEmpty()) {
        return createErrorResponse("Email is required");
    }
```
**Lines 46-51**: Input validation and error handling
- **try-catch**: Wrap risky code in error handling
- **Null checks**: Ensure required data is present
- **Early returns**: Return error immediately if validation fails

```java
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
```
**Lines 57-72**: Business logic with branching
- **getUserByEmail()**: Database query to find user
- **if-else if**: Different logic for login vs signup
- **Object creation**: Create new user for signup

**Programming Concept - Database Operations:**
```java
// CRUD operations (Create, Read, Update, Delete)
public class UserService {
    
    // CREATE
    public User createUser(String email, String name) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setCreatedAt(new Date());
        return database.save(user);
    }
    
    // READ
    public User findUserByEmail(String email) {
        return database.query("SELECT * FROM users WHERE email = ?", email);
    }
    
    // UPDATE
    public void updateUser(User user) {
        user.setUpdatedAt(new Date());
        database.update(user);
    }
    
    // DELETE
    public void deleteUser(long userId) {
        database.delete("users", userId);
    }
}
```

```java
// Generate OTP
String otpCode = generateOTP();

// Store OTP in authentication session
RootAuthenticationSessionModel rootSession = session.authenticationSessions().createRootAuthenticationSession(realm);
AuthenticationSessionModel authSession = rootSession.createAuthenticationSession(client);
authSession.setAuthNote(AUTH_NOTE_OTP_KEY, otpCode);
authSession.setAuthNote(AUTH_NOTE_OTP_CREATED_AT, String.valueOf(System.currentTimeMillis() / 1000));
```
**Lines 74-84**: Session management
- **generateOTP()**: Call helper method to create random code
- **Session creation**: Create temporary storage for this login attempt
- **setAuthNote()**: Store data in session
- **System.currentTimeMillis()**: Get current time in milliseconds

**Programming Concept - Time Handling:**
```java
// Working with time in Java
long currentTimeMillis = System.currentTimeMillis();  // Milliseconds since 1970
long currentTimeSeconds = currentTimeMillis / 1000;   // Convert to seconds

// Date formatting
Date now = new Date();
SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
String formattedDate = formatter.format(now);

// Time calculations
long oneHourInMillis = 60 * 60 * 1000;  // 60 seconds * 60 minutes * 1000 ms
long oneHourFromNow = currentTimeMillis + oneHourInMillis;

// Check if something expired
boolean isExpired = System.currentTimeMillis() > expirationTime;
```

```java
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
```
**Lines 557-571**: Helper methods for consistent API responses
- **HashMap**: Creates key-value pairs for JSON response
- **Response.ok()**: Creates HTTP 200 (success) response
- **Response.status()**: Creates HTTP error response (400 = Bad Request)

**Programming Concept - JSON APIs:**
```java
// JSON structure for API responses
{
    "success": true,
    "message": "OTP sent successfully",
    "data": {
        "sessionId": "abc123",
        "expiryTime": 1634567890
    }
}

// Error response
{
    "success": false,
    "message": "Email is required",
    "data": null
}

// Java code to create JSON response
Map<String, Object> apiResponse = new HashMap<>();
apiResponse.put("success", true);
apiResponse.put("message", "Operation completed");
apiResponse.put("data", resultData);

// Framework converts Map to JSON automatically
```

---

## Email Service Integration

### TwilioEmailService.java - External API Integration

```java
public class TwilioEmailService {
    
    private static final Logger logger = Logger.getLogger(TwilioEmailService.class);
    
    private static final String SENDGRID_API_KEY = System.getenv("SENDGRID_API_KEY");
    private static final String FROM_EMAIL = System.getenv("SENDGRID_FROM_EMAIL");
    private static final String FROM_NAME = System.getenv("SENDGRID_FROM_NAME");
```
**Lines 12-18**: Class setup and configuration
- **Logger**: Object for writing debug and error messages
- **System.getenv()**: Read environment variables (configuration outside code)
- **static final**: Constants that never change

**Programming Concept - Environment Variables:**
```java
// Environment variables are set outside your program
// They're used for configuration that changes between environments

// Development environment:
// SENDGRID_API_KEY=test_key_12345
// FROM_EMAIL=dev@myapp.com

// Production environment:
// SENDGRID_API_KEY=live_key_98765
// FROM_EMAIL=noreply@myapp.com

// Java code reads them:
String apiKey = System.getenv("SENDGRID_API_KEY");
String databaseUrl = System.getenv("DATABASE_URL");

// This way, same code works in different environments
```

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
**Lines 22-29**: Constructor with validation
- **Constructor**: Special method called when creating new object
- **Conditional initialization**: Only create SendGrid client if API key exists
- **Logging**: Record what happened for debugging

**Programming Concept - Constructors:**
```java
public class Car {
    private String make;
    private String model;
    private int year;
    
    // Default constructor
    public Car() {
        this.make = "Unknown";
        this.model = "Unknown";
        this.year = 2023;
    }
    
    // Constructor with parameters
    public Car(String make, String model, int year) {
        this.make = make;
        this.model = model;
        this.year = year;
    }
    
    // Constructor with validation
    public Car(String make, String model, int year) {
        if (make == null || make.isEmpty()) {
            throw new IllegalArgumentException("Make cannot be empty");
        }
        this.make = make;
        this.model = model;
        this.year = year;
    }
}

// Usage:
Car car1 = new Car();                          // Uses default constructor
Car car2 = new Car("Toyota", "Camry", 2023);   // Uses parameterized constructor
```

```java
public void sendOTPEmail(String toEmail, String otpCode, int expiryMinutes) {
    try {
        if (SENDGRID_API_KEY == null || FROM_EMAIL == null) {
            throw new RuntimeException("SendGrid credentials not configured...");
        }

        Email from = new Email(FROM_EMAIL, FROM_NAME != null ? FROM_NAME : "OTP Service");
        Email to = new Email(toEmail);
        String subject = "Your Email Verification Code";
        Content content = new Content("text/plain", buildOTPEmailContent(otpCode, expiryMinutes));

        Mail mail = new Mail(from, subject, to, content);
```
**Lines 31-42**: Email composition
- **Method parameters**: Input values needed to send email
- **Validation**: Check required configuration exists
- **Object creation**: Create email objects for SendGrid API
- **Ternary operator**: `condition ? valueIfTrue : valueIfFalse`

**Programming Concept - Ternary Operator:**
```java
// Long form if-else
String displayName;
if (name != null) {
    displayName = name;
} else {
    displayName = "Anonymous";
}

// Short form ternary operator
String displayName = name != null ? name : "Anonymous";

// More examples
int max = (a > b) ? a : b;  // Get larger of two numbers
String message = isLoggedIn ? "Welcome back!" : "Please log in";
String status = age >= 18 ? "Adult" : "Minor";
```

```java
Request request = new Request();
request.setMethod(Method.POST);
request.setEndpoint("mail/send");
request.setBody(mail.build());

Response response = sendGrid.api(request);
```
**Lines 44-49**: API request execution
- **Request/Response pattern**: How programs communicate over internet
- **Method.POST**: HTTP POST request (sending data)
- **setBody()**: The email data to send
- **sendGrid.api()**: Make the actual API call

**Programming Concept - HTTP Requests:**
```java
// HTTP is how web applications communicate
// Think of it like sending letters:

// Request (your letter to the post office):
// - Method: POST (what you want to do)
// - URL: https://api.sendgrid.com/v3/mail/send (where to send)
// - Headers: Content-Type: application/json (type of content)
// - Body: { "from": "...", "to": "...", "subject": "..." } (the content)

// Response (reply from post office):
// - Status: 200 OK (success) or 400 Bad Request (error)
// - Headers: Content-Type: application/json
// - Body: { "message": "Email sent successfully" }

// Java HTTP example:
HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://api.example.com/send"))
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString("{\"message\":\"hello\"}"))
    .build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println("Status: " + response.statusCode());
System.out.println("Body: " + response.body());
```

```java
if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
    logger.infof("Email sent successfully to %s via SendGrid", toEmail);
} else {
    logger.errorf("Failed to send email via SendGrid. Status: %d, Body: %s", 
                 response.getStatusCode(), response.getBody());
    throw new RuntimeException("Failed to send email via SendGrid. Status: " + response.getStatusCode());
}
```
**Lines 51-57**: Response handling and error checking
- **Status code checking**: 200-299 are success codes
- **String formatting**: `%s` for strings, `%d` for numbers
- **throw new RuntimeException**: Create and throw error if email failed

**Programming Concept - HTTP Status Codes:**
```java
// HTTP status codes tell you what happened
// 2xx = Success
// 200 OK - Everything worked
// 201 Created - New resource created
// 204 No Content - Success, but no data to return

// 4xx = Client Error (your mistake)
// 400 Bad Request - Invalid data sent
// 401 Unauthorized - Need to log in
// 403 Forbidden - Logged in but not allowed
// 404 Not Found - Resource doesn't exist

// 5xx = Server Error (their mistake)
// 500 Internal Server Error - Something broke on server
// 503 Service Unavailable - Server overloaded

// Checking status codes:
if (statusCode >= 200 && statusCode < 300) {
    // Success
} else if (statusCode >= 400 && statusCode < 500) {
    // Client error - fix your request
} else if (statusCode >= 500) {
    // Server error - try again later
}
```

```java
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
```
**Lines 99-109**: String template method
- **private**: Only this class can call this method
- **String.format()**: Insert values into string template
- **%s**: Placeholder for string value
- **%d**: Placeholder for integer value
- **\n**: New line character

**Programming Concept - String Formatting:**
```java
// Different ways to build strings

// 1. Concatenation (joining strings)
String message = "Hello " + name + ", you have " + count + " messages";

// 2. String.format() - like printf in C
String message = String.format("Hello %s, you have %d messages", name, count);

// 3. StringBuilder - efficient for many concatenations
StringBuilder sb = new StringBuilder();
sb.append("Hello ");
sb.append(name);
sb.append(", you have ");
sb.append(count);
sb.append(" messages");
String message = sb.toString();

// 4. String templates (Java 21+)
String message = STR."Hello \{name}, you have \{count} messages";

// Format specifiers:
// %s = String
// %d = Integer
// %f = Float
// %.2f = Float with 2 decimal places
// %10s = String padded to 10 characters
// %-10s = String left-aligned in 10 characters

String formatted = String.format("Price: $%.2f, Item: %-10s, Qty: %3d", 
                                 19.99, "Widget", 5);
// Result: "Price: $19.99, Item: Widget    , Qty:   5"
```

---

## Template System

### login-email-otp.ftl - User Interface Template

```freemarker
<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('email-otp'); section>
```
**Lines 1-2**: Template system setup
- **FreeMarker**: Template language for generating HTML
- **import**: Use Keycloak's base template
- **@layout.registrationLayout**: Wrap content in standard Keycloak page layout
- **displayMessage**: Show/hide messages based on errors

**Programming Concept - Template Engines:**
```html
<!-- Static HTML -->
<h1>Welcome, John!</h1>
<p>You have 5 messages</p>

<!-- Template with variables -->
<h1>Welcome, ${userName}!</h1>
<p>You have ${messageCount} messages</p>

<!-- Template with conditions -->
<#if user.isAdmin>
    <p>You are an administrator</p>
</#if>

<!-- Template with loops -->
<ul>
<#list messages as message>
    <li>${message.subject}</li>
</#list>
</ul>
```

```freemarker
<#if section = "header">
    ${msg("doLogIn")}
<#elseif section = "form">
```
**Lines 4-6**: Template sections
- **<#if>**: Conditional content
- **section**: Variable telling which part of page to render
- **msg()**: Function to get internationalized text

```freemarker
<form id="kc-otp-login-form" class="${properties.kcFormClass!}" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
```
**Line 7**: HTML form definition
- **${properties.kcFormClass!}**: Get CSS class from theme
- **!**: Default to empty string if property missing
- **onsubmit**: JavaScript code when form submitted
- **${url.loginAction}**: Keycloak-generated URL for form submission

**Programming Concept - HTML Forms:**
```html
<!-- Basic form structure -->
<form action="/submit" method="post">
    <input type="text" name="username" placeholder="Username" required>
    <input type="password" name="password" placeholder="Password" required>
    <button type="submit">Login</button>
</form>

<!-- Form attributes -->
<!-- action: where to send data when submitted -->
<!-- method: GET (in URL) or POST (in body) -->
<!-- enctype: how to encode data (multipart/form-data for files) -->

<!-- Input types -->
<input type="text">        <!-- Regular text -->
<input type="email">       <!-- Email validation -->
<input type="password">    <!-- Hidden text -->
<input type="number">      <!-- Numeric input -->
<input type="checkbox">    <!-- Checkboxes -->
<input type="radio">       <!-- Radio buttons -->
<input type="file">        <!-- File upload -->
<input type="hidden">      <!-- Hidden values -->
```

```freemarker
<input id="email-otp" name="email-otp" autocomplete="one-time-code" type="text" class="${properties.kcInputClass!}" autofocus=true aria-invalid="<#if messagesPerField.existsError('email-otp')>true</#if>" dir="ltr" />
```
**Line 14**: OTP input field
- **name="email-otp"**: Form field identifier
- **autocomplete="one-time-code"**: Browser hint for password managers
- **autofocus=true**: Cursor starts in this field
- **aria-invalid**: Accessibility attribute for screen readers
- **dir="ltr"**: Text direction (left-to-right)

```freemarker
<#if messagesPerField.existsError('email-otp')>
    <span id="input-error-email-otp-code" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
        ${kcSanitize(messagesPerField.get('email-otp'))?no_esc}
    </span>
</#if>
```
**Lines 16-20**: Error message display
- **existsError()**: Check if validation error exists
- **aria-live="polite"**: Screen reader announces changes
- **kcSanitize()**: Prevent XSS attacks while allowing HTML
- **?no_esc**: Don't escape HTML entities

**Programming Concept - Web Accessibility:**
```html
<!-- Accessibility (a11y) makes websites usable by everyone -->

<!-- Screen reader support -->
<label for="email">Email Address</label>
<input id="email" name="email" type="email" aria-required="true">

<!-- Error messages -->
<input aria-invalid="true" aria-describedby="email-error">
<div id="email-error" role="alert">Please enter a valid email</div>

<!-- Live regions for dynamic content -->
<div aria-live="polite">Status updates appear here</div>
<div aria-live="assertive">Urgent alerts appear here</div>

<!-- Keyboard navigation -->
<button tabindex="1">First</button>
<button tabindex="2">Second</button>
<button tabindex="-1">Not keyboard accessible</button>

<!-- Semantic HTML -->
<main>           <!-- Primary content -->
<nav>            <!-- Navigation -->
<aside>          <!-- Sidebar content -->
<section>        <!-- Document sections -->
<article>        <!-- Standalone content -->
```

---

## Configuration Management

### messages_en.properties - Internationalization

```properties
# Form
loginEmailOtp=Your access code
doResendEmail=Resend email
errorInvalidEmailOtp=Invalid code
errorExpiredEmailOtp=Code expired, we have sent you a new one, please check your email
```
**Lines 1-5**: User interface text
- **Properties file**: Key-value pairs for configuration
- **Comments**: Lines starting with # are comments
- **Key=Value**: Simple format for storing text

**Programming Concept - Properties Files:**
```properties
# Configuration file format
# Comments start with # or !

# Database settings
database.url=jdbc:mysql://localhost:3306/myapp
database.username=admin
database.password=secret123

# Application settings
app.name=My Application
app.version=1.0.0
debug.enabled=true

# Internationalization
welcome.message=Welcome to our application
error.invalid.email=Please enter a valid email address
button.submit=Submit
button.cancel=Cancel
```

```java
// Reading properties in Java
Properties props = new Properties();
props.load(new FileInputStream("config.properties"));

String dbUrl = props.getProperty("database.url");
String appName = props.getProperty("app.name", "Default App Name");  // With default
```

```properties
# Email
emailOtpSubject=Your access code
emailOtpYourAccessCode=Your access code is:
emailOtpExpiration=The code will expire in {0} minutes.
```
**Lines 7-10**: Email content templates
- **{0}**: Placeholder for first parameter
- **Message formatting**: Similar to String.format() in Java

**Programming Concept - Message Formatting:**
```properties
# Properties with parameters
welcome.user=Welcome, {0}!
order.summary=Order #{0} for ${1} contains {2} items
date.format=Today is {0,date,short}
number.format=Price: {0,number,currency}

# Multiple files for different languages
# messages_en.properties
welcome.user=Welcome, {0}!
goodbye.user=Goodbye, {0}!

# messages_es.properties  
welcome.user=¡Bienvenido, {0}!
goodbye.user=¡Adiós, {0}!

# messages_fr.properties
welcome.user=Bienvenue, {0}!
goodbye.user=Au revoir, {0}!
```

---

## Programming Concepts You'll Learn

### 1. Object-Oriented Programming (OOP)

**Classes and Objects:**
```java
// Class is a blueprint
public class Car {
    // Properties (attributes)
    private String make;
    private String model;
    private int year;
    
    // Constructor
    public Car(String make, String model, int year) {
        this.make = make;
        this.model = model;
        this.year = year;
    }
    
    // Methods (behaviors)
    public void start() {
        System.out.println("Car is starting...");
    }
    
    public String getInfo() {
        return year + " " + make + " " + model;
    }
}

// Object is an instance of a class
Car myCar = new Car("Toyota", "Camry", 2023);
myCar.start();  // Calls the start method
String info = myCar.getInfo();  // Gets "2023 Toyota Camry"
```

**Encapsulation (Data Hiding):**
```java
public class BankAccount {
    private double balance;  // Private - can't access directly
    
    // Public methods control access to private data
    public void deposit(double amount) {
        if (amount > 0) {
            balance += amount;
        }
    }
    
    public boolean withdraw(double amount) {
        if (amount > 0 && amount <= balance) {
            balance -= amount;
            return true;
        }
        return false;
    }
    
    public double getBalance() {
        return balance;  // Read-only access
    }
}
```

**Inheritance:**
```java
// Parent class
public class Animal {
    protected String name;
    
    public Animal(String name) {
        this.name = name;
    }
    
    public void eat() {
        System.out.println(name + " is eating");
    }
}

// Child class inherits from Animal
public class Dog extends Animal {
    public Dog(String name) {
        super(name);  // Call parent constructor
    }
    
    public void bark() {
        System.out.println(name + " is barking");
    }
    
    @Override
    public void eat() {
        System.out.println(name + " is eating dog food");
    }
}
```

### 2. Design Patterns

**Factory Pattern:**
```java
// Factory creates objects without exposing creation logic
public class VehicleFactory {
    public static Vehicle createVehicle(String type) {
        switch (type.toLowerCase()) {
            case "car":
                return new Car();
            case "motorcycle":
                return new Motorcycle();
            case "truck":
                return new Truck();
            default:
                throw new IllegalArgumentException("Unknown vehicle type: " + type);
        }
    }
}

// Usage
Vehicle car = VehicleFactory.createVehicle("car");
Vehicle bike = VehicleFactory.createVehicle("motorcycle");
```

**Singleton Pattern:**
```java
// Only one instance can exist
public class Database {
    private static Database instance = null;
    private Connection connection;
    
    private Database() {  // Private constructor
        connection = createConnection();
    }
    
    public static Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }
    
    public void query(String sql) {
        // Use the single database connection
    }
}

// Usage
Database db1 = Database.getInstance();
Database db2 = Database.getInstance();
// db1 and db2 are the same object
```

### 3. Error Handling

**Try-Catch-Finally:**
```java
public void readFile(String filename) {
    FileReader file = null;
    try {
        file = new FileReader(filename);
        // Read file content
        String content = file.read();
        processContent(content);
    } catch (FileNotFoundException e) {
        System.out.println("File not found: " + filename);
    } catch (IOException e) {
        System.out.println("Error reading file: " + e.getMessage());
    } catch (Exception e) {
        System.out.println("Unexpected error: " + e.getMessage());
    } finally {
        // Always runs, even if exception occurs
        if (file != null) {
            try {
                file.close();
            } catch (IOException e) {
                System.out.println("Error closing file");
            }
        }
    }
}
```

**Creating Custom Exceptions:**
```java
// Custom exception class
public class InsufficientFundsException extends Exception {
    private double balance;
    private double amount;
    
    public InsufficientFundsException(double balance, double amount) {
        super("Insufficient funds: tried to withdraw " + amount + " but only have " + balance);
        this.balance = balance;
        this.amount = amount;
    }
    
    public double getBalance() { return balance; }
    public double getAmount() { return amount; }
}

// Using custom exception
public void withdraw(double amount) throws InsufficientFundsException {
    if (amount > balance) {
        throw new InsufficientFundsException(balance, amount);
    }
    balance -= amount;
}
```

### 4. Collections and Data Structures

**Lists:**
```java
// ArrayList - resizable array
List<String> names = new ArrayList<>();
names.add("Alice");
names.add("Bob");
names.add("Charlie");

// Access elements
String first = names.get(0);  // "Alice"
int size = names.size();      // 3

// Loop through list
for (String name : names) {
    System.out.println("Hello, " + name);
}

// LinkedList - good for frequent insertions/deletions
List<String> linkedNames = new LinkedList<>();
linkedNames.add("David");
linkedNames.add(0, "Eve");  // Insert at beginning
```

**Maps:**
```java
// HashMap - key-value pairs
Map<String, Integer> ages = new HashMap<>();
ages.put("Alice", 25);
ages.put("Bob", 30);
ages.put("Charlie", 35);

// Access values
Integer aliceAge = ages.get("Alice");  // 25
boolean hasBob = ages.containsKey("Bob");  // true

// Loop through map
for (Map.Entry<String, Integer> entry : ages.entrySet()) {
    System.out.println(entry.getKey() + " is " + entry.getValue() + " years old");
}
```

**Sets:**
```java
// HashSet - unique values only
Set<String> uniqueNames = new HashSet<>();
uniqueNames.add("Alice");
uniqueNames.add("Bob");
uniqueNames.add("Alice");  // Duplicate, won't be added

System.out.println(uniqueNames.size());  // 2, not 3
```

### 5. Functional Programming Concepts

**Lambda Expressions:**
```java
// Old way - anonymous class
List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
names.sort(new Comparator<String>() {
    @Override
    public int compare(String a, String b) {
        return a.compareTo(b);
    }
});

// New way - lambda expression
names.sort((a, b) -> a.compareTo(b));
// Even shorter
names.sort(String::compareTo);
```

**Stream API:**
```java
List<Person> people = Arrays.asList(
    new Person("Alice", 25),
    new Person("Bob", 30),
    new Person("Charlie", 35),
    new Person("Diana", 28)
);

// Filter and collect
List<Person> adults = people.stream()
    .filter(person -> person.getAge() >= 30)
    .collect(Collectors.toList());

// Map and reduce
int totalAge = people.stream()
    .mapToInt(Person::getAge)
    .sum();

// Complex operations
List<String> adultNames = people.stream()
    .filter(person -> person.getAge() >= 30)
    .map(Person::getName)
    .map(String::toUpperCase)
    .sorted()
    .collect(Collectors.toList());
```

### 6. Multithreading and Concurrency

**Basic Threading:**
```java
// Extending Thread class
public class CounterThread extends Thread {
    @Override
    public void run() {
        for (int i = 0; i < 10; i++) {
            System.out.println("Count: " + i);
            try {
                Thread.sleep(1000);  // Wait 1 second
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

// Using Runnable interface
public class CounterRunnable implements Runnable {
    @Override
    public void run() {
        for (int i = 0; i < 10; i++) {
            System.out.println("Count: " + i);
        }
    }
}

// Usage
CounterThread thread1 = new CounterThread();
thread1.start();

Thread thread2 = new Thread(new CounterRunnable());
thread2.start();

// Lambda version
Thread thread3 = new Thread(() -> {
    for (int i = 0; i < 10; i++) {
        System.out.println("Count: " + i);
    }
});
thread3.start();
```

### 7. Database Integration

**JDBC Basics:**
```java
public class UserDAO {
    private Connection connection;
    
    public UserDAO(String dbUrl, String username, String password) {
        try {
            connection = DriverManager.getConnection(dbUrl, username, password);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }
    
    public User findUserById(long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return new User(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("email")
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding user", e);
        }
        return null;
    }
    
    public void createUser(User user) {
        String sql = "INSERT INTO users (name, email) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error creating user", e);
        }
    }
}
```

This comprehensive walkthrough shows you how real-world Java applications are built, from basic syntax to advanced concepts like REST APIs, database integration, and security patterns. Each concept builds on the previous ones, giving you a solid foundation for becoming a professional developer.