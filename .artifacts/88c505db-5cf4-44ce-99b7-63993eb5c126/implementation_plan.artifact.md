# QuickDrop Security Hardening & Authorization Plan

Comprehensive production-oriented security implementation for QuickDrop (Flask backend + Android app) covering authentication, RBAC, data privacy, delivery workflow protection (OTPs, tracking tokens), and infrastructure hardening.

## User Review Required

> [!IMPORTANT]
> - **Authentication & Passwords**: Passwords will be securely hashed using `bcrypt`. JWT tokens (or secure signed session tokens) will be implemented for API authentication with short expiry and proper revocation.
> - **Role-Based Access Control (RBAC)**: Strict backend enforcement for Admin, Sender, Delivery Partner, and Receiver (via high-entropy tracking tokens). Prevents IDOR and privilege escalation.
> - **Delivery Workflows (OTPs & Tracking)**: Cryptographically secure random OTP generation with rate-limiting and expiry. High-entropy unguessable tracking tokens for receivers.

## Open Questions
- None. Following strict OWASP top 10 and Android security best practices.

## Proposed Changes

### Backend (Python Flask)

#### [MODIFY] [app.py](file:///C:/Users/ItsBo/AndroidStudioProjects/Quick_Drop/app.py)
- Add security headers (CSP, HSTS, X-Content-Type-Options, X-Frame-Options).
- Add CORS restrictions and request-size limits (max content length).
- Implement password hashing (bcrypt) and JWT-based authentication decorators (`@login_required`, `@role_required`).
- Implement rate limiting on sensitive endpoints (/login, /signup, /otp, /reset).
- Implement secure OTP generation, verification, and expiration logic for order pickup and delivery.
- Implement high-entropy tracking token generation for order tracking.
- Implement robust input validation and safe error handling (no stack traces or internal IDs exposed).

#### [NEW] [.env.example](file:///C:/Users/ItsBo/AndroidStudioProjects/Quick_Drop/.env.example)
- Provide template environment variables (`MONGO_URI`, `JWT_SECRET_KEY`, etc.) without real secrets.

#### [NEW] [SECURITY.md](file:///C:/Users/ItsBo/AndroidStudioProjects/Quick_Drop/SECURITY.md)
- Document implemented security controls, secret management, configuration, and deployment guidelines.

#### [MODIFY] [README.md](file:///C:/Users/ItsBo/AndroidStudioProjects/Quick_Drop/README.md)
- Update setup instructions, required environment variables, and security notes.

#### [MODIFY] [requirements.txt](file:///C:/Users/ItsBo/AndroidStudioProjects/Quick_Drop/requirements.txt)
- Add security libraries: `bcrypt`, `PyJWT`, `Flask-Limiter` (or custom rate limiter), etc.

### Android App

#### [MODIFY] [UserSession.java](file:///C:/Users/ItsBo/AndroidStudioProjects/Quick_Drop/app/src/main/java/com/example/quick_drop/utils/UserSession.java)
- Update to store auth tokens and session data securely using `EncryptedSharedPreferences` (backed by Android Keystore).
- Ensure logout clears credentials securely.

#### [NEW] [NetworkSecurityConfig](file:///C:/Users/ItsBo/AndroidStudioProjects/Quick_Drop/app/src/main/res/xml/network_security_config.xml)
- Enforce HTTPS / restrict cleartext traffic in production.

## Verification Plan

### Automated Tests
- Run unit and integration tests for authentication, role permissions, ownership isolation (IDOR), rate limiting, and OTP validation.
- Run Python tests via pytest / unittest.

### Manual Verification
- Test login, role-based navigation, order creation, partner assignment, secure OTP verification, and receiver tracking token access.
