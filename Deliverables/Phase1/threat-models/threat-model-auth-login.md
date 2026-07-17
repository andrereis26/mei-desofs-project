# Threat Modeling Report - Authentication Login Functionality

---

# 1. DFD Overview

## System Context

The login functionality allows an external client to submit credentials to `POST /api/auth/login`.
If authentication succeeds, the backend issues a JWT and records the outcome in audit logs.

System components:
- API Client (external actor)
- Auth Controller (HTTP entrypoint)
- Auth Service (credential verification and token issuance)
- User DB (user records and password hashes)
- Audit Log Store (security event traceability)

---

## DFD Diagram

![Login DFD](./images/loginDFD.svg)


---

## Main Components

### Actor
- API Client

### Processes
- Backend API (login endpoint)
- AuthManager (credential verification)
- Generate JWT Token

### Data Stores
- Database (user credentials)

### Trust Boundaries
- Between API Client and Backend API (public API boundary)
- Between authentication processing and Database (internal trust boundary)

---

# 2. Threat Mapping

| Login Flow Focus | Threat IDs | Notes |
|---|---|---|
| Credential and token abuse | T-001, T-002, T-019 | Covers credential stuffing, forged claims, and replay-style misuse. |
| Request and payload tampering | T-005 | Covers in-transit/request manipulation attempts. |
| Information disclosure | T-008, T-009 | Covers enumeration and sensitive log/response leakage. |
| Availability pressure | T-011, T-012 | Covers brute-force volume and expensive hash-path abuse. |
| Audit and accountability | T-010 | Covers repudiation and audit integrity concerns. |

## Risk Prioritization (Flow View)

### High Risk
- T-001 Credential/token theft and spoofing in authentication entrypoint
- T-011 Authentication flood and brute-force patterns
- T-002 Token/claim forgery paths

### Medium Risk
- T-008 Enumeration by response behavior
- T-009 Sensitive data exposure in logs/responses
- T-005 Request tampering

### Low Risk
- T-010 Repudiation concerns when audit controls are incomplete

---

# 3. Countermeasures and Mitigation

## Accept
- Minor non-security telemetry gaps can be accepted if authentication and audit-critical fields stay mandatory.

## Eliminate
- Detailed authentication error responses that differentiate user-not-found vs wrong-password.
- Logging of raw passwords, password hashes, or full bearer tokens.
- Weak JWT signing-key management and unvalidated claim construction.

## Mitigate

### Authentication and Abuse Protection
- Enforce strong password policy and optional MFA for privileged accounts.
- Apply per-account and per-IP rate limiting with exponential backoff.
- Add lockout/challenge controls for repeated failed attempts.

### Token Security
- Use strong JWT signing keys with rotation and strict issuer/audience/expiry validation.
- Keep claims minimal and derive sensitive authorization context server-side where possible.
- Use short token TTL and secure refresh handling.

### Data and Transport Protection
- Enforce TLS and HSTS for all auth traffic.
- Normalize authentication response patterns to reduce enumeration.
- Redact secrets in logs and traces.

### Logging and Audit
- Log user identifier, source IP, timestamp, and outcome for every login attempt.
- Protect audit logs with append-only or tamper-evident storage.
- Alert on anomalous login patterns.

## Transfer
- Use managed bot-detection/identity-protection services for credential abuse defense.
- Route security telemetry to centralized SIEM for anomaly detection and retention.

