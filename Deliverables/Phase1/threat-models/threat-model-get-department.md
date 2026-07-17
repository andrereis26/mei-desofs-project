# Threat Modeling Report - Department GET Endpoints Functionality

---

# 1. DFD Overview

## System Context

The department read functionality allows authenticated callers to request `GET /api/departments`, `GET /api/departments/{id}`, and `GET /api/departments/by-name/{name}`.
Requests cross the public API boundary, pass JWT-based authentication, evaluate authorization context, retrieve department data from persistence, and return list/single Department DTO responses.

System components:
- API Client (authenticated actor)
- Backend API (department GET endpoints)
- Authentication process
- Authorization process
- Department Service (read and mapping logic)
- Database (department persistence)

---

## DFD Diagram

![Get Department DFD](./images/getDepartmentDFD.svg)

---

## Main Components

### Actor
- API Client (JWT-authenticated caller)

### Processes
- Backend API:
	- Coordinates authentication/authorization context and delegates read operations.
- Authentication:
	- Validates authentication context before protected processing.
- Authorization:
	- Returns caller permission decision/context used by backend flow.
- Department Service:
	- Retrieves department collection and single department records by id/name.
	- Maps results to Department DTO responses.

### Data Stores
- Database:
	- Stores department records used for get operations.

### Trust Boundaries
- Public API trust boundary between API Client and Backend API.
- Internal trust boundary around authentication/service/database interactions.

### Key Data Flows
- `get request` (API Client to Backend API for list/by-id/by-name)
- `authenticate` and `auth result` (Backend API and Authentication)
- `authenticate user` and `auth result` (Authentication and Database)
- `user` and `permissions` (Backend API and Authorization)
- `get Department` and `Department` (Department Service and Database)
- `get Department` and `Department DTO` (Backend API and Department Service)
- `response` (Backend API to API Client)

---

# 2. Threat Mapping

| Department GET Flow Focus | Threat IDs | Notes |
|---|---|---|
| Token and identity spoofing | T-001, T-002 | Covers stolen token and forged-claim access paths. |
| Lookup tampering and IDOR | T-006, T-005 | Covers manipulated `{id}`/`{name}` access attempts. |
| Authorization and scope bypass | T-007 | Covers missing role/scope enforcement on read endpoints. |
| Information disclosure | T-008, T-009 | Covers over-broad responses and enumeration side channels. |
| Availability abuse | T-013, T-014 | Covers read floods and probing amplification. |
| Accountability | T-010 | Covers read-action repudiation and missing evidence. |

## Risk Prioritization (Flow View)

### High Risk
- T-007 Broken access control on read endpoints
- T-006 IDOR through lookup parameter manipulation
- T-009 Over-broad department data exposure

### Medium Risk
- T-008 Enumeration behavior in list/by-id/by-name paths
- T-013 and T-014 high-rate probing and read abuse
- T-001/T-002 token misuse and forged claims

### Low Risk
- T-010 Repudiation if read-audit coverage is insufficient

---

# 3. Countermeasures and Mitigation

## Accept
- Exposure of non-sensitive metadata fields can be accepted only when data classification explicitly marks them as public to authenticated users.

## Eliminate
- Service paths that return department data without deny-by-default authorization checks.
- Error responses that differentiate unauthorized vs non-existent resources in ways that enable enumeration.
- Any dependency on client-supplied role/permission context.

## Mitigate

### Authentication and Authorization
- Enforce strict JWT validation.
- Apply endpoint-level and resource-level authorization in service layer for `listAll`, `findById`, and `findByName` using caller identity and scope.
- Enforce deny-by-default policy for unauthorized department reads.

### Input and Response Hardening
- Validate and normalize `{id}` and `{name}` before repository access.
- Return consistent failure behavior across list/by-id/by-name paths to reduce enumeration.
- Minimize DTO fields to least-privilege disclosure per caller role/scope.

### Availability Protection
- Rate limit read operations per token/IP.
- Detect and throttle UUID-scanning and name-bruteforce behavior patterns.
- Monitor database read load and tune indexes for authorized list and lookup paths.

### Logging and Accountability
- Log actor ID, requested department ID, decision (allow/deny), and timestamp for read attempts.
- Protect logs with tamper-evident controls and restricted write paths.
- Alert on repeated unauthorized access attempts to multiple department IDs.
- Define read-audit events for all department GET paths as a mandatory security requirement.

## Transfer
- Use API gateway/WAF protections for throttling and anomaly detection at ingress.
- Forward read-access security events to centralized SIEM for correlation and incident response.
