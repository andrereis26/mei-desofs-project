# 📄 Threat Modeling Report — Document Upload Functionality

---

# 1. DFD Overview

## System Context

The Document Upload functionality allows authenticated users to upload files that are validated, stored, and logged by the system.

System components:
- Frontend Web Application
- Backend API (Spring Boot)
- Database (metadata storage)
- File System (file storage)
- Audit Logging (internal service)

---

## DFD Diagram

![Upload Document DFD](./images/uploadDocumentDFD.svg)


---

## Main Components

### Actor
- User (Authenticated)

### Processes
- API Client
- Backend API (Upload Controller)
- Authentication
- Authorization
- Document Processing
- File Processing
- Audit Logging

### Data Stores
- Database (file metadata)
- File System (uploaded files)

### Trust Boundaries
- Between API Client and Backend (trusted)
- Between Backend and File System (trusted)
- Between Backend and Database (trusted)

---

# 2. Threat Mapping

| Upload Flow Focus | Threat IDs | Notes |
|---|---|---|
| Token and session misuse | T-001, T-019 | Covers stolen credential/token upload attempts. |
| Upload authorization failures | T-007 | Covers upload without required role/scope checks. |
| Payload and transport tampering | T-005 | Covers request/content manipulation before validation. |
| Malicious content and file abuse | T-016 | Covers dangerous payload formats and decompression abuse. |
| Filesystem boundary violations | T-017 | Covers path traversal and restricted-path overwrite. |
| Information disclosure | T-009 | Covers metadata/file path leakage in responses and logs. |
| Availability pressure | T-018, T-013 | Covers oversized uploads and endpoint flood pressure. |
| Audit and repudiation | T-010 | Covers missing or weak upload evidence. |

## Risk Prioritization (Flow View)

### High Risk
- T-017 Filesystem traversal and overwrite paths
- T-016 Malicious payload upload and processing abuse
- T-007 Upload authorization bypass

### Medium Risk
- T-018 Large upload exhaustion
- T-005 Tampered upload payloads
- T-009 Metadata and path disclosure

### Low Risk
- T-010 Repudiation if upload audit controls are incomplete

---

# 3. Countermeasures and Mitigation

---

## Accept
- Low impact logging inconsistencies (if centralized audit system exists)

---

## Eliminate
- User-controlled file paths
- Direct filesystem exposure from API input
- Execution permissions on upload directories

---

## Mitigate

### Authentication & Access Control
- Strict JWT validation (signature, expiry, issuer)
- Enforce authorization checks on upload endpoint

### File Security
- Server-side file validation (MIME, extension, signature)
- Generate server-side filenames (no user input paths)
- Restrict upload directory access
- Prevent execution of uploaded files

### System Protection
- File size limits
- Rate limiting per user/IP
- Sandbox processing for uploaded files

### Data Protection
- Encrypt sensitive files at rest (if needed)
- Use randomized file identifiers

### Logging & Audit
- Immutable audit logs
- Include userId, timestamp, IP, file hash
- Monitor anomalous upload patterns

---

## Transfer

- External storage (e.g., S3-compatible object storage)
- External antivirus/malware scanning service
- External file processing service (sandboxed environment)