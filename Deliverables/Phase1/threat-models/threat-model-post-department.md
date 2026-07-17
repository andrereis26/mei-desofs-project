# Threat Modeling Report - Department Creation Functionality

---

# 1. DFD Overview

## System Context

The department creation functionality allows authenticated `ADMIN`/`MANAGER` callers to submit `POST /api/departments`.
Requests cross the public API boundary, pass JWT validation, execute role-aware create logic, persist validated records, and generate audit events.

System components:
- API Client (authenticated actor)
- Security Filter Chain + JWT Authentication Filter
- Department Controller (create endpoint)
- Department DB (department persistence with integrity constraints)
- Audit Log Store (creation accountability)

---

## DFD Diagram

![Post Department DFD](./images/postDepartmentDFD.svg)


---

## Main Components

### Actor
- API Client

### Processes
- Backend API (department creation endpoint)
- Authentication
- Authorization
- Save Department Process
- Audit Logging

### Data Stores
- Database (department records and audit entries)

### Trust Boundaries
- Between API Client and Backend API (public API boundary)
- Between department processing and Database (internal trust boundary)

---

# 2. Threat Mapping

| Department Create Flow Focus | Threat IDs | Notes |
|---|---|---|
| Token and claim spoofing | T-001, T-002 | Covers stolen privileged tokens and forged claim contexts. |
| Payload tampering and race abuse | T-005, T-015 | Covers invalid field tampering and uniqueness-window abuse. |
| Authorization and scope enforcement | T-007 | Covers RBAC and manager-scope bypass on create operations. |
| Disclosure and audit integrity | T-008, T-009, T-010 | Covers verbose errors, metadata leakage, and repudiation gaps. |
| Availability pressure | T-013, T-014 | Covers create floods and persistence contention. |

## Risk Prioritization (Flow View)

### High Risk
- T-007 Broken authorization on department creation
- T-002 Privileged claim forgery and weak claim validation
- T-013 High-rate create abuse

### Medium Risk
- T-005 Payload tampering
- T-015 Race/uniqueness tampering
- T-008 and T-009 disclosure via create responses/logging

### Low Risk
- T-010 Repudiation if create audit coverage is incomplete

---

# 3. Countermeasures and Mitigation

## Accept
- Minor UX-level validation message simplifications can be accepted if security controls and audit semantics remain complete.

## Eliminate
- Client-side role assumptions without server-side authorization enforcement.
- Acceptance of unmanaged or unknown fields in department create payloads.
- Detailed database exception leaks in API responses.

## Mitigate

### Authentication and RBAC
- Enforce strict JWT validation and claim verification.
- Keep deny-by-default create authorization in service layer (`ADMIN`/`MANAGER` only).
- Add scope constraints so managers can only create within assigned governance boundaries.

### Input and Persistence Integrity
- Apply strict DTO validation (length, charset, normalization, allowlists).
- Enforce uniqueness constraints and transactional create semantics.
- Implement idempotency or duplicate-request protections where appropriate.

### Availability Protection
- Rate limit create operations by principal/IP.
- Detect anomalous create bursts and repeated conflict attempts.
- Tune DB write path and lock handling for controlled contention behavior.

### Logging and Audit
- Log actor role, department payload fingerprint, timestamp, and outcome.
- Store immutable/tamper-evident audit entries for create actions.
- Alert on unauthorized create attempts and repeated policy denials.

## Transfer
- Use API gateway/WAF protections for abuse throttling and request anomaly screening.
- Forward create-operation security events to SIEM for correlation and incident handling.

