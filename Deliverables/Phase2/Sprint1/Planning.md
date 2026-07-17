# Phase 2 - Sprint 1 Planning

Sprint goal: complete the remaining Phase 1 backlog needed for Sprint 1 while preserving the secure behavior that is already in place and turning the documented security controls into repeatable delivery practices.

Status rule: `Done` means the current implementation already satisfies the documented item at sprint planning time. `To Do` means the item is missing or only partially covered and must be completed during Sprint 1.

ASVS note: the scoped ASVS items from Phase 1 remain the validation baseline for this sprint. Sprint work must also close the remaining ASVS consultation and traceability gaps identified during planning.

| ID | Title | Description | Priority | Status | Assignee | Dependencies |
|---|---|---|---|--------|---|---|
| FR-001 | Registration and profile initialization | Allow a new user to create an account with the required identity data and a default non-privileged profile. | High | Done   | Diogo Silva (1211634) | - |
| FR-002 | Authentication and session start | Authenticate valid credentials and issue a time-bounded access token with user context. | High | Done   | André Reis (1191256) | - |
| FR-003 | Profile retrieval | Let an authorized user retrieve persisted profile information for a target account. | High | Done   | Diogo Silva (1211634) | FR-002 |
| FR-004 | Profile update | Let an authorized user change editable profile data while preserving privilege boundaries. | High | Done   | Diogo Silva (1211634) | FR-002, SR-005 |
| FR-005 | Administrative user directory | Complete administrative directory capabilities with pagination and role-based filtering for management work. | High | Done   | Diogo Silva (1211634) | FR-002, SR-005, SR-008 |
| FR-006 | Department management and membership | Maintain create, update, delete, and join flows for departments and their manager/member relationships. | High | Done   | Guilherme Cunha (1201506) | FR-002, SR-005 |
| FR-007 | Document upload with metadata | Allow authorized uploads with ownership, metadata capture, and optional department association. | High | Done   | Pedro Ferreira (1210825) | FR-002, SR-005, SR-006, SR-007 |
| FR-008 | Document catalog and retrieval | Complete document catalog and read capabilities so authorized users can access both document records and intended content safely. | High | Done   | Pedro Ferreira (1210825) | FR-002, SR-005, SR-006, SR-007, SR-008, SR-009 |
| FR-009 | Document lifecycle maintenance | Maintain replace and delete flows while preserving consistent document state and access control. | High | Done   | Pedro Ferreira (1210825) | FR-007 |
| FR-010 | Immutable audit trail generation | Record immutable audit entries for sensitive create, update, and delete activity across the domain. | High | Done   | Guilherme Cunha (1201506) | FR-002 |
| FR-011 | Initial platform bootstrap | Initialize the first administrative capability and baseline organizational data for first use. | Medium | Done   | Francisco Peixoto (1211648) | - |
| NFR-001 | Stateless API behavior | Keep normal API authorization stateless and independent of server-side session state. | High | Done   | André Reis (1191256) | FR-002 |
| NFR-002 | Configurable upload limits | Enforce environment-specific upload size limits consistently across document intake flows. | Medium | Done   | Pedro Ferreira (1210825) | FR-007 |
| NFR-003 | Repeatable schema evolution | Manage data model changes through versioned, repeatable migration steps. | Medium | Done   | Guilherme Cunha (1201506) | - |
| NFR-004 | Automated build and security gates | Validate every change through automated build, test, and security analysis gates before merge. | High | Done   | Guilherme Cunha (1201506) | SDR-005, SDR-006, SDR-007, SDR-008 |
| NFR-005 | Least-privilege runtime execution | Run the application with the minimum privileges required for normal operation. | Medium | Done   | Francisco Peixoto (1211648) | - |
| NFR-006 | Minimal response disclosure | Return only the data required for API use and avoid leaking internal implementation details. | High | Done   | André Reis (1191256) | SR-009 |
| NFR-007 | Docker image publishing | The main workflow shall publish a Docker image to Docker Hub using versioned tags. | Medium | Done   | André Reis (1191256) | NFR-004 |
| NFR-008 | GitHub Release creation | The main workflow shall create a GitHub Release for each versioned build. | Medium | Done   | André Reis (1191256) | NFR-004 |
| NFR-009 | AWS deployment | The main workflow shall deploy the application to the Terraform-managed AWS production environment. | Low | Done   | André Reis (1191256) | NFR-004 |
| SR-001 | Secure password storage | Store passwords only as adaptive one-way hashes. | High | Done   | André Reis (1191256) | FR-001 |
| SR-002 | Strong secret configuration | Reject weak cryptographic secret configuration and enforce minimum secret strength. | High | Done   | André Reis (1191256) | FR-002 |
| SR-003 | Token expiration enforcement | Issue tokens with bounded validity and reject expired tokens. | High | Done   | André Reis (1191256) | FR-002 |
| SR-004 | Minimized public attack surface | Keep only approved public endpoints unauthenticated and require authentication everywhere else by default. | High | Done   | André Reis (1191256) | FR-002 |
| SR-005 | RBAC and object-level authorization | Enforce role and ownership or relationship checks on sensitive operations. | High | Done   | Diogo Silva (1211634) | FR-002 |
| SR-006 | Path traversal protection | Reject file operations that resolve outside the trusted storage boundary. | High | Done   | Pedro Ferreira (1210825) | FR-007 |
| SR-007 | Safe filename handling | Sanitize user-controlled filenames before storage and reuse. | High | Done   | Pedro Ferreira (1210825) | FR-007 |
| SR-008 | Input validation at boundary and domain | Close remaining validation gaps so invalid or adversarial input is rejected consistently with deterministic responses. | High | Done   | Diogo Silva (1211634) | FR-005, FR-008 |
| SR-009 | Safe error handling | Normalize security-sensitive error responses so failures do not reveal internal details or resource hints. | High | Done  | Pedro Ferreira (1210825) | SR-008 |
| SR-010 | Audit event completeness | Capture actor, action, target, and timestamp for all security-relevant actions. | High | Done   | Guilherme Cunha (1201506) | FR-010 |
| SR-011 | No client-driven role escalation | Prevent self-assignment of elevated roles in registration and profile flows. | High | Done   | Diogo Silva (1211634) | FR-001, FR-004 |
| SR-012 | Non-predictable identifiers | Use identifiers that reduce trivial enumeration of protected resources. | Medium | Done   | Guilherme Cunha (1201506) | - |
| SR-013 | Uniqueness constraints | Reject duplicate identity and domain records through application and persistence rules. | Medium | Done   | Guilherme Cunha (1201506) | FR-001, FR-006 |
| SR-014 | Injection-safe data access | Ensure query inputs are handled as data and cannot change intended data access behavior. | High | Done   | Francisco Peixoto (1211648) | - |
| SR-015 | Auth and registration rate limiting | Enforce configurable rate limits for authentication and registration endpoints. | High | Done   | André Reis (1191256) | FR-001, FR-002 |
| SR-016 | Token revocation support | Reject revoked tokens and invalidate sessions when required. | High | Done   | André Reis (1191256) | FR-002 |
| SR-19 | Container image security policy | Enforce a container image policy with vulnerability scanning, trusted base images, and registry controls. | High | Done   | André Reis (1191256) | - |
| SR-20 | Doppler-managed secrets | Secrets shall be managed via Doppler and injected at runtime or pipeline execution. No secrets should be hardcoded or stored in source control. | High | Done   | André Reis (1191256) | - |
| SR-017 | Malicious upload protections | Reject uploaded content that fails type validation or security scanning rules. | High | Done  | Pedro Ferreira (1210825) | FR-007 |
| SR-018 | Secure bootstrap policy | Enforce controlled initial admin provisioning without weak defaults. | High | Done  | Francisco Peixoto (1211648) | FR-011 |
| SDR-001 | Secure coding checklist | Adopt a lightweight secure coding checklist for each change and require it during review. | High | Done   | Guilherme Cunha (1201506) | - |
| SDR-002 | ASVS consultation and traceability | Record consulted ASVS items for security-impacting work and close remaining sprint-level traceability gaps. | High | Done  | André Reis (1191256) | SDR-001 |
| SDR-003 | Third-party review discipline | Add a repeatable review step for new dependencies, external snippets, and generated external code before adoption. | Medium | Done   | Francisco Peixoto (1211648) | - |
| SDR-004 | Secret leak prevention controls | Add development-time secret handling, leak detection, and push-protection practices. | High | Done  | André Reis (1191256) | - |
| SDR-005 | Automated static analysis and review gates | Enforce automated static analysis and code review security gates on every change. | High | Done   | Diogo Silva (1211634) | SDR-001 |
| SDR-006 | Continuous dependency risk control | Enforce recurring dependency analysis, triage, and blocking rules for severe third-party findings. | High | Done  | Pedro Ferreira (1210825) | SDR-003 |
| SDR-007 | Mandatory DevSecOps pipeline | Integrate build, tests, secret scanning, static analysis, dependency analysis, and artifact scanning into required pre-merge checks. | High | Done   | Guilherme Cunha (1201506) | SDR-004, SDR-005, SDR-006, SDR-008 |
| SDR-008 | Security test expansion | Add the planned deny-path, abuse-case, traversal, safe-error, and runtime security tests for sprint scope. | High | Done   | André Reis (1191256) | FR-005, FR-008, SR-008, SR-009 |
| SDR-009 | Security-focused peer review | Require explicit security review approval for security-impacting backlog items. | High | Done   | Francisco Peixoto (1211648) | SDR-001 |
| SDR-010 | Documentation and evidence upkeep | Update delivery evidence and security documentation whenever controls, tests, or policies change. | Medium | Done   | Francisco Peixoto (1211648) | SDR-002, SDR-007, SDR-008 |
