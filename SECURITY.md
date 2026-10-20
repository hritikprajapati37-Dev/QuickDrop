# QuickDrop Security Architecture & Compliance

QuickDrop is engineered with production-grade security controls covering both the Flask backend and Android client application, aligned with OWASP Top 10 and Android security best practices.

## 1. Authentication & Session Security
- **Password Hashing**: Passwords are securely hashed using `bcrypt` with a strong work factor (12 rounds). Plaintext passwords are never stored or logged.
- **Tokens & Sessions**: JSON Web Tokens (JWT) with HMAC SHA-256 signing are issued upon successful authentication, with strict 8-hour expiration.
- **Rate Limiting**: Rate limiting (powered by `Flask-Limiter`) is enforced on sensitive endpoints (`/api/auth/login`, `/api/auth/signup`, `/api/delivery/request-otp`, `/api/delivery/verify-otp`) to prevent brute-force and credential stuffing attacks.
- **Credential Enumeration Protection**: Authentication endpoints return generic error responses ("Invalid credentials") for failed login attempts.

## 2. Role-Based Access Control (RBAC) & IDOR Protection
- **Role Enforcement**: Strict backend decorator checks (`@role_required`) enforce authorization for protected routes across user roles:
  - `ADMIN`: Restricted administrative operations.
  - `SENDER`: Access and manage only owned profiles, deliveries, orders, and payment records.
  - `PARTNER`: Access only assigned deliveries and minimum necessary fulfillment data.
- **Receiver Tracking**: Receivers do not require an account. Order tracking is secured via high-entropy cryptographically unguessable tracking tokens (`secrets.token_urlsafe(32)`), preventing IDOR and enumeration.

## 3. Database & Backend Hardening
- **Secrets Management**: Credentials (`MONGO_URI`, `JWT_SECRET_KEY`) are loaded exclusively from environment variables via `.env`. A template `.env.example` is provided. `.env` is excluded in `.gitignore`.
- **Security Headers**: Applied to all HTTP responses:
  - `X-Content-Type-Options: nosniff`
  - `X-Frame-Options: DENY`
  - `X-XSS-Protection: 1; mode=block`
  - `Strict-Transport-Security: max-age=31536000; includeSubDomains`
  - `Content-Security-Policy: default-src 'self'`
- **Request Size Limits**: Max content length is restricted to 1MB to mitigate payload overflow attacks.
- **CORS & Error Handling**: Configured securely. Generic error messages prevent internal stack traces or database IDs from leaking.

## 4. Android Client Security
- **Secure Storage**: User session data, roles, and auth tokens are stored using `EncryptedSharedPreferences` backed by the Android Keystore (AES-256-GCM / AES-256-SIV).
- **Network Security**: Cleartext traffic is disabled (`usesCleartextTraffic="false"`) with a custom network security configuration enforcing HTTPS.
- **Logout Clears State**: Secure session clearing removes all stored credentials upon logout.

## 5. Delivery Workflow Protection (OTPs)
- **Cryptographic OTPs**: 4-digit OTPs for pickup and delivery verification are generated using cryptographically secure random number generators (`secrets.randbelow`), assigned a 10-minute expiration, and rate-limited against brute force (maximum 3 failed attempts).
