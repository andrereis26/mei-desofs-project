# Threat Modeling Report - Document Deletion Functionality

---

# 1. DFD Overview

## System Context

The document deletion functionality allows authenticated callers to request deletion using `DELETE /api/documents/{id}`.
Requests cross the public API boundary, pass through JWT validation, reach controller/service logic, affect document persistence, and generate audit events.

System components:
- API Client (authenticated actor)
- Security Filter Chain + JWT Authentication Filter
- Document Controller (delete endpoint)
- Document DB (document metadata/persistence state)
- Audit Log Store (deletion accountability)

---

## DFD Diagram

![Delete Document DFD](./images/deleteDocumentDFD.svg)


---

## Main Components

### Actor
- API Client

### Processes
- Backend API (document deletion endpoint)
- Authentication
- Authorization
- Delete Document Processing
- File Processing
- Audit Logging

### Data Stores
- Database (document records and audit entries)
- File System (stored files)

### Trust Boundaries
- Between API Client and Backend API (public API boundary)
- Between document deletion processing and Database (internal trust boundary)
- Between file processing and File System (internal trust boundary)

---

# 2. Threat Mapping

| Delete Flow Focus | Threat IDs | Notes |
|---|---|---|
| Token misuse and replay | T-001, T-019 | Covers stolen token and replay abuse on destructive endpoint. |
| Identifier tampering and IDOR | T-006, T-005 | Covers `{id}` manipulation and request tampering. |
| Authorization and scope bypass | T-007 | Covers role/object-level delete policy failures. |
| Disclosure and audit leakage | T-008, T-009, T-010 | Covers existence disclosure, metadata leakage, and repudiation risk. |
| Availability and persistence pressure | T-013, T-014 | Covers delete floods and DB contention amplification. |

## Risk Prioritization (Flow View)

### High Risk
- T-006 Object identifier tampering and IDOR on delete target
- T-007 Broken authorization for destructive operations
- T-001 Token theft misuse on protected delete operations

### Medium Risk
- T-013 High-rate destructive abuse
- T-005 Request tampering and malformed delete intent
- T-008/T-009 response and metadata disclosure

### Low Risk
- T-010 Repudiation if audit evidence is incomplete

---

# 3. Countermeasures and Mitigation

## Accept
- Minor non-security metadata omissions can be accepted if actor identity, target ID, and outcome remain mandatory in audit records.

## Eliminate
- Authorization logic that relies only on client-supplied identifiers.
- Verbose delete responses that disclose record existence.
- Direct delete permissions without ownership/scope checks in service layer.

## Mitigate

### Authentication and Authorization
- Enforce strict JWT validation (signature, expiry, issuer, audience).
- Apply deny-by-default delete authorization in service layer.
- Validate caller role and document ownership/department scope before deletion.

### Request and Data Integrity
- Validate and normalize `{id}` parameters before repository access.
- Use transactional delete semantics to avoid partial inconsistent state.
- Consider soft-delete plus retention/restore controls for high-value records.

### Availability Protection
- Rate limit destructive operations per principal and per source.
- Add anomaly detection for bursts of delete actions.
- Apply DB safeguards (timeouts, lock tuning, and retries with backoff).

### Logging and Audit
- Log actor ID, target document ID, timestamp, source IP, and outcome.
- Protect audit data with tamper-evident storage and restricted write paths.
- Alert on suspicious delete patterns (cross-department or bulk behavior).

## Transfer
- Use centralized WAF/API gateway policies for brute-force and request anomaly filtering.
- Send security events to external SIEM for retention, correlation, and incident response.

