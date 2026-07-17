# Phase 1 - Threat Models Overview

## 1. Objective
This document is the threat catalog for Phase 1.

Instead of maintaining separate threat tables per endpoint file, all identified threats are merged into one table to make tracking and cross-document traceability simpler at application level.

Per-flow files under `Deliverables/Phase1/threat-models/` now keep DFD context and mitigation guidance, while threat IDs are centralized here.

## 2. Modeled Threat-Model Set

| Flow | Threat Model File | Flow Threat Count |
|---|---|---:|
| [Authentication Login](./threat-models/threat-model-auth-login.md) | `threat-model-auth-login.md` | 10 |
| [Authentication Registration](./threat-models/threat-model-auth-register.md) | `threat-model-auth-register.md` | 10 |
| [Document Deletion](./threat-models/threat-model-delete-document.md) | `threat-model-delete-document.md` | 11 |
| [Department Creation](./threat-models/threat-model-post-department.md) | `threat-model-post-department.md` | 11 |
| [Document Upload](./threat-models/threat-model-uploadDocument.md) | `threat-model-uploadDocument.md` | 12 |
| [Department GET Endpoints](./threat-models/threat-model-get-department.md) | `threat-model-get-department.md` | 11 |

Total threats identified in flow analyses: **65**  
Total threats used for cross-document traceability: **19**

## 3. Unified Threat Table

| ID | STRIDE | Threat | Brief Description |
|---|---|---|---|
| T-001 | S | Credential/token theft and session spoofing | Stolen credentials or tokens are reused to act as legitimate users. |
| T-002 | S/E | Token or claim forgery | Weak key handling or claim validation enables forged privileged identity context. |
| T-003 | S/D | Registration identity and bot abuse | Fake identities and automated registrations are used to seed later attacks. |
| T-004 | T/E | Role assignment abuse in provisioning | Client-side role fields or weak defaults allow privilege assignment during registration/bootstrap. |
| T-005 | T | Input/payload tampering and validation bypass | Crafted input alters intended behavior or bypasses validation constraints. |
| T-006 | T/E | IDOR and object identifier tampering | Attackers manipulate identifiers to access records outside authorized ownership/scope. |
| T-007 | E | Broken authorization and scope bypass | Role/object checks are missing or incomplete, allowing unauthorized operations. |
| T-008 | I | Enumeration and verbose error leakage | Response behavior reveals account/resource existence and internal decision details. |
| T-009 | I | Sensitive data exposure in logs/DTOs | Logs or API responses expose confidential metadata or internal identifiers. | 
| T-010 | R/T | Audit integrity and repudiation gaps | Missing/tamperable audit events reduce accountability and forensic quality. | 
| T-011 | D | Authentication and registration flooding | High-rate auth/registration traffic exhausts API and persistence resources. | 
| T-012 | D | Computational exhaustion in credential processing | Expensive credential-hash operations are abused to consume CPU. | 
| T-013 | D | Endpoint flooding and destructive abuse | Sustained high-rate traffic on read/create/delete endpoints degrades service and data operations. | 
| T-014 | D | Persistence contention and probing amplification | Repeated conflicting writes or probing behavior causes lock/contention degradation. |
| T-015 | T | Race-condition and uniqueness tampering | Concurrent requests exploit uniqueness windows and create integrity inconsistencies. | 
| T-016 | T/D | Malicious file payload and decompression abuse | Weaponized files (including zip bombs) are uploaded to trigger processing abuse. | 
| T-017 | T/E | Path traversal and restricted path overwrite | File path manipulation escapes storage root or overwrites sensitive paths. | 
| T-018 | D | Large-upload resource exhaustion | Oversized upload payloads consume memory, storage, and processing capacity. | 
| T-019 | S | Token replay abuse | Captured valid bearer tokens are replayed before expiration. | 

## 4. DREAD Calculation

Formula used:

$$
DREAD = \frac{Damage + Reproducibility + Exploitability + AffectedUsers + Discoverability}{5}
$$

Scores use a 1-10 scale per criterion.

| ID | Damage | Reproducibility | Exploitability | Affected Users | Discoverability | DREAD Score |
|---|---:|---:|---:|---:|---:|---:|
| T-001 | 8 | 9 | 8 | 7 | 7 | 7.8 |
| T-002 | 8 | 8 | 7 | 6 | 7 | 7.2 |
| T-003 | 7 | 8 | 8 | 6 | 7 | 7.2 |
| T-004 | 8 | 8 | 7 | 6 | 7 | 7.2 |
| T-005 | 7 | 7 | 7 | 6 | 6 | 6.6 |
| T-006 | 9 | 9 | 8 | 8 | 9 | 8.6 |
| T-007 | 8 | 8 | 7 | 7 | 7 | 7.4 |
| T-008 | 7 | 8 | 7 | 6 | 8 | 7.2 |
| T-009 | 7 | 7 | 6 | 6 | 7 | 6.6 |
| T-010 | 6 | 6 | 5 | 5 | 6 | 5.6 |
| T-011 | 8 | 9 | 8 | 7 | 7 | 7.8 |
| T-012 | 7 | 8 | 7 | 6 | 8 | 7.2 |
| T-013 | 8 | 8 | 7 | 7 | 8 | 7.6 |
| T-014 | 7 | 7 | 6 | 6 | 7 | 6.6 |
| T-015 | 7 | 7 | 7 | 6 | 7 | 6.8 |
| T-016 | 9 | 9 | 8 | 8 | 9 | 8.6 |
| T-017 | 8 | 8 | 7 | 7 | 8 | 7.6 |
| T-018 | 8 | 8 | 7 | 7 | 7 | 7.4 |
| T-019 | 7 | 8 | 7 | 7 | 6 | 7.0 |

### Brief DREAD Justifications

- T-006 remains high (8.6) because successful IDOR can directly expose and tamper with non-owned resources, with high reproducibility once identifier patterns are learned.
- T-017 remains above T-018 because traversal and restricted-path overwrite can impact both integrity and confidentiality, while oversized upload attacks are primarily availability-focused.
- T-011 stays high on reproducibility because credential-stuffing and flooding are cheap to automate and scale, even when individual attempts are blocked.
- T-010 remains comparatively lower because audit and repudiation gaps are force multipliers for other attacks rather than immediate compromise vectors on their own.