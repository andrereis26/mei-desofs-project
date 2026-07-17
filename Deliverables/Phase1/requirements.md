# Phase 1 - Requirements

## 1. Scope and Method
This document defines requirements for a secure REST API.

Design scope:
- Aggregates: User, Document, Department, Audit.
- Roles: ADMIN, MANAGER, USER.
- Threat references in this document use IDs (`T-*`) defined in `threat-models-overview.md` and (AB-*) for abuse/misuse cases in `abuse-and-misuse-case-catalog.md`.

## 2. Functional Requirements (FR)

| ID | Requirement | Acceptance Criteria | Related Threat ID(s) |
|---|---|---|---|
| FR-001 | The system shall provide user account registration and profile initialization. | A new account is created with required profile fields and persisted as an active user. | T-003, T-004, T-005 |
| FR-002 | The system shall provide user authentication and session start. | Valid credentials start a user session and return an authentication token with user context. | T-001, T-002, T-005, T-011, T-019 |
| FR-003 | The system shall provide user profile retrieval capabilities. | The API returns persisted profile information for a requested user account. | T-006, T-007, T-009 |
| FR-004 | The system shall provide user profile update capabilities. | Editable profile attributes are updated and persisted correctly after a valid update request. | T-006, T-007 |
| FR-005 | The system shall provide a user directory for administration workflows. | The API exposes a paginated user listing with role-based filtering fields for management operations. | T-007, T-008, T-009 |
| FR-006 | The system shall support department management and membership workflows. | Departments can be created, updated, deleted, and joined, and manager/member relationships are persisted. | T-007, T-009, T-013, T-014 |
| FR-007 | The system shall support document upload with domain metadata capture. | Uploaded documents are stored together with metadata such as owner, filename, size, and optional department association. | T-007, T-009, T-016, T-017, T-018 |
| FR-008 | The system shall provide document catalog and retrieval operations. | Documents can be listed and fetched by identifier, including associated metadata needed by the domain. | T-005, T-007, T-008, T-009, T-013, T-014 |
| FR-009 | The system shall support document lifecycle maintenance operations. | Existing documents can be replaced or removed, and document state remains consistent after each operation. | T-001, T-006, T-007, T-009, T-013 |
| FR-010 | The system shall provide immutable audit trail generation for domain events. | User, department, and document create/update/delete actions generate audit entries with actor, action, target, and timestamp. | T-010 |
| FR-011 | The system shall provide initial platform bootstrap capability. | A first-run setup flow initializes the initial administrative account and baseline organizational data. | T-002, T-004 |

## 3. Non-Functional Requirements (NFR)

| ID | Requirement | Acceptance Criteria | Justification |
|---|---|---|---|
| NFR-001 | The API shall follow stateless interaction principles. | No server session state is required to authorize standard API calls. | Stateless interactions reduce session hijacking surface and improve secure horizontal scaling. |
| NFR-002 | File upload limits shall be configurable by environment. | Maximum upload size is externally configurable and enforced consistently. | Environment-tuned limits reduce denial-of-service risk from oversized payloads. |
| NFR-003 | Database schema evolution shall be versioned and repeatable. | Schema changes are managed by migration artifacts and are reproducible across environments. | Versioned migrations prevent drift and insecure manual database changes across environments. |
| NFR-004 | CI shall include automated build, tests, and security analysis gates. | Each change is validated by build/test and configured security checks in pipeline. | Automated gates detect regressions early and enforce secure SDLC compliance per change. |
| NFR-005 | Runtime deployment shall support least-privilege execution. | Application runtime can be configured to run as a non-root process. | Least-privilege runtime limits blast radius if the application or container is compromised. |
| NFR-006 | API responses shall minimize sensitive/internal data exposure. | Internal storage paths and implementation details are not returned in public DTOs. | Reducing response detail limits reconnaissance value and sensitive information leakage. |
| NFR-007 | The main workflow shall publish a Docker image to Docker Hub using versioned tags. | On pushes to `main`, the pipeline builds and pushes the image with an immutable version tag (e.g., `vX.Y.Z`) and updates a `latest` tag; the image digest is recorded. | Versioned image publication enables traceable releases and reproducible deployments. |
| NFR-008 | The main workflow shall create a GitHub Release for each versioned build. | After all gates pass, the workflow creates or updates a GitHub Release with the version tag, release notes, and the published image digest/link. | Releases provide an auditable trail and make rollbacks safer and faster. |
| NFR-009 | The main workflow shall deploy the application to a Terraform-managed AWS production environment. | After security gates pass, the workflow deploys the versioned image to the AWS host provisioned in `eu-west-3`, applies environment-specific configuration, and records the deployment outcome. | Automated deployment ensures consistent promotion to the required environment. |
| NFR-010 | The production environment shall provide Prometheus monitoring for API, database, and host metrics. | Prometheus is deployed with scrape configs and baseline alerts; metrics are visible in the Prometheus UI after deployment. | Monitoring enables early detection of availability and performance issues. |
| NFR-011 | The production environment shall provide Grafana-based observability for logs and metrics. | Grafana is deployed with log and metric data sources and baseline dashboards; API logs and metrics are visible in Grafana. | Centralized observability supports incident response and operational insight. |
| NFR-012 | The production environment shall automate backups for PostgreSQL and Redis volumes. | Scheduled backups run with retention defined; restore steps are documented and a restore test succeeds in staging. | Backups reduce data loss risk and improve recovery capability. |
| NFR-013 | The application shall emit structured logs suitable for centralized ingestion. | Logs are in JSON with consistent fields (timestamp, level, service, request id) and are parsable by the logging stack. | Structured logging enables reliable search, correlation, and alerting. |
| NFR-014 | API HTTP semantics shall be explicit and verifiable. | JSON responses, ProblemDetail errors, and binary downloads declare appropriate media types; unsupported HTTP methods are rejected; supported-but-unmapped methods do not execute application handlers. | Explicit API contracts reduce ambiguous client behavior, content-type confusion, method abuse, and regressions in REST security behavior. |

## 4. Security Requirements (SR)

| ID | Requirement | Acceptance Criteria | Justification | Related Threat ID(s) | Related Abuse/Misuse ID(s) |
|---|---|---|---|---|---|
| SR-001 | Passwords shall be stored using adaptive one-way hashing. | No plaintext password persistence is permitted. | Adaptive hashing reduces impact of credential-store compromise. | T-001, T-012 | AB-001 |
| SR-002 | Cryptographic secrets shall meet minimum strength requirements. | Startup/deploy validation blocks weak secret configuration. | Strong secrets are required to prevent token forgery and cryptographic control bypass. | T-002, T-001 | AB-003, AB-012 |
| SR-003 | Access tokens shall include bounded validity and expiration. | Tokens include issue/expiration claims and are rejected when expired. | Token expiration constrains attacker dwell time when credentials or tokens are stolen. | T-001, T-019 | AB-004 |
| SR-004 | Public attack surface shall be explicitly minimized. | Only approved public endpoints are unauthenticated; all others require authentication. | Minimizing unauthenticated endpoints reduces externally reachable attack surface by default. | T-005, T-003, T-011 | AB-001, AB-002 |
| SR-005 | Authorization shall enforce both RBAC and object-level access control. | Role checks plus ownership/relationship checks are applied to sensitive actions. | Combined RBAC and object checks prevent horizontal and vertical privilege escalation. | T-006, T-007, T-005, T-009 | AB-005, AB-006, AB-007, AB-008 |
| SR-006 | File operations shall enforce path traversal protection. | Canonicalized paths outside trusted storage root are rejected. | Canonical-path enforcement blocks file-system escape and unauthorized file access. | T-017 | AB-009 |
| SR-007 | User-controlled filenames shall be sanitized before storage. | Unsafe filename characters and path-like input are normalized/rejected. | Filename sanitization prevents path tricks, collisions, and unsafe storage behavior. | T-016, T-017 | AB-009, AB-010 |
| SR-008 | Input validation shall be enforced at API boundary and domain constraints. | Invalid payloads are rejected with deterministic error responses. | Strong validation reduces injection risk and preserves domain data integrity. | T-005, T-006 | AB-009, AB-013 |
| SR-009 | Error handling shall avoid sensitive information leakage. | Authentication/authorization failures return generic and non-sensitive messages. | Safe error contracts deny attackers internal implementation and configuration details. | T-008, T-009 | AB-005, AB-007 |
| SR-010 | Audit logging shall capture actor, action, target, and timestamp. | Security-relevant operations are traceable in tamper-resistant records. | Auditability supports detection, incident response, and accountability for sensitive actions. | T-010 | AB-011 |
| SR-011 | Privilege escalation through client-controlled role fields shall be prevented. | Registration and profile flows cannot self-assign elevated roles. | Server-side role control prevents direct self-promotion to privileged permissions. | T-004, T-002 | AB-006 |
| SR-012 | Resource identifiers shall reduce predictable enumeration risk. | Identifier strategy prevents trivial sequential enumeration. | Non-predictable identifiers make large-scale ID probing and data harvesting harder. | T-006, T-009, T-008 | AB-005, AB-007 |
| SR-013 | Identity and domain uniqueness constraints shall be enforced. | Duplicate identity/domain entries are rejected by application and persistence rules. | Uniqueness constraints prevent conflicting identities and authorization ambiguity. | T-003, T-008, T-015, T-014 | AB-002 |
| SR-014 | Database query operations shall prevent SQL injection through parameterized statements or ORM-safe query construction. | Injection payloads are treated as data and cannot alter intended query semantics. | SQL injection can expose, modify, or destroy data and can bypass authorization logic. | T-005, T-006 | AB-013 |
| SR-015 | Authentication and registration endpoints shall enforce rate limiting. | Requests exceeding configurable per-client or per-identity thresholds are rejected with `429` and do not perform credential verification. | Rate limiting reduces credential stuffing and auth-flooding impact while preserving availability. | T-011 | AB-001, AB-002 |
| SR-016 | Access tokens shall support revocation and session invalidation. | Revoked tokens are rejected immediately and cannot be used to access protected endpoints. | Revocation limits impact of token compromise and enables rapid incident response. | T-001, T-019 | AB-004 |
| SR-017 | Uploaded content shall be subject to malicious-content protections. | Files failing security checks (type validation, scanning, or policy rules) are rejected and not stored. | Content controls reduce risk from weaponized uploads. | T-016 | AB-010 |
| SR-018 | Privileged bootstrap shall follow a secure initialization policy. | Initial admin provisioning requires controlled setup steps and rejects default or weak credentials. | Secure bootstrap prevents persistent unauthorized admin access. | T-004, T-002 | AB-012 |
| SR-019 | Container images shall be scanned for vulnerabilities before publish or release. | The pipeline runs an image scan and blocks publication or release on High/Critical findings; scan reports are archived. | Image scanning reduces the risk of deploying known vulnerable components. | T-009, T-013 | AB-014 |
| SR-020 | Secrets shall be managed via Doppler and injected at runtime or pipeline execution. | No application secrets are stored in source control; deployment uses Doppler-managed secret injection with least-privilege access and auditable execution paths. | Centralized secret management reduces leakage and strengthens rotation/traceability. | T-001, T-002, T-009 | AB-015 |
| SR-021 | Domain models shall enforce user and department invariants through value objects. | User identity and department value objects reject invalid data before persistence; invalid domain objects cannot be constructed. | Domain-level validation prevents invalid state and reduces bypass of authorization logic. | T-005, T-006 | AB-009, AB-013 |
| SR-022 | Domain models shall enforce document and audit invariants through value objects. | Document metadata and audit value objects reject invalid data before persistence; invalid domain objects cannot be constructed. | Domain-level validation preserves integrity for document and audit records. | T-005, T-006 | AB-009, AB-013 |
| SR-023 | Untrusted input shall be encoded, sanitized, or escaped before interpreter-sensitive use. | Response headers are built through safe framework APIs or allowlisted values; database search patterns escape wildcard metacharacters; user input is not used to build dynamic scripts, templates, regexes, or command strings. | Treating untrusted values strictly as data reduces injection, header-splitting, wildcard abuse, and unsafe interpreter boundary risks. | T-005, T-008, T-016 | AB-010, AB-013 |

