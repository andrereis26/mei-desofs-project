# Phase 1 - Security Test Strategy and Detailed Plan

## 1. Test Strategy Objective
Define a design-stage verification strategy for Phase 2 implementation.

Goals:
- Convert requirements and threats into executable security tests.
- Prioritize tests by risk and implementation order.
- Maintain explicit traceability to requirement, threat, control, and ASVS references.


## 2. Security Testing Methodology 


### 2.1 Shift-Left Security Methodology

Instead of treating security as a final gate, our methodology adopts a Shift-Left approach, integrating security testing continuously from design through deployment.

Core Methodological Principles for Phase 2:
- Threat-Informed Backlog: Security acceptance criteria and negative testing scenarios (derived from our Abuse Cases) will be defined as first-class work items in sprint planning before coding begins.
- Link each planned security test to requirement IDs (`FR-*`, `SR-*`, `NFR-*`) and threat IDs (`T-*`) before coding starts.
- Continuous Automation: Fast security feedback loops (Unit, SAST, SCA) will be enforced on every pull request, preventing insecure code patterns from merging into the main branch.
- Apply deny-by-default authorization verification in service-layer tests.


### 2.2 Static Application Security Testing (SAST)
Objective: detect insecure coding patterns early in Java source code and configuration before runtime.

Planned tooling and usage:
- SonarCloud: Static Application Security Testing (SAST) tool used in CI to identify code quality issues and security vulnerabilities before runtime.
- SpotBugs: Bytecode-level static analysis executed in CI to detect common Java security and correctness issues.

Primary rules of interest for this project:

- Injection Vulnerabilities: Strict detection of unparameterized database queries and unsafe data access patterns. This directly addresses our identified SQL injection misuse case (AB-013) and aligns with payload-tampering threats (T-005, T-006).

- Insecure File Handling & Path Traversal: Enforcement of canonical path verification to prevent attackers from escaping the intended storage root. This rule is critical to mitigate the traversal scenarios defined in AB-009 and threat T-017.

- Broken Access Control: Detection of missing method-level security annotations and insecure ownership validations. This automated check supports the mitigation of unauthorized profile/department access (AB-005, AB-006, AB-007) and related access-control threats (T-006, T-007).

- Hardcoded Secrets and Weak Bootstrap: Identification of plain-text credentials, tokens, or weak default configurations committed to the source code, directly supporting our operational security priorities (AB-012) and privilege/claim threats (T-002, T-004).

- Improper Error Handling & Information Exposure: Detection of generic exception handling that could leak sensitive stack traces or system details, mitigating disclosure threats (T-008).

SAST gate policy:
- Fail PR and CI SAST gates when any configured `Critical`/`High` security issue remains open without an approved, time-bound exception.
- Block merge when the required SonarCloud SAST check is missing, failed, or skipped on protected branches.


### 2.3 Software Composition Analysis (SCA)
Objective: identify vulnerable third-party libraries and transitive dependencies before release.

Planned tooling and usage:
- Dependabot: continuous dependency advisory monitoring and automated update pull requests.
- OWASP Dependency-Check: CI dependency vulnerability scan for Maven packages.

Coverage scope:
- `backend/pom.xml` direct and transitive dependencies.
- Base image and package risks where applicable to containerized runtime.
- Risk Mitigation Focus: Preventing inherited vulnerabilities that could bypass our primary security controls, such as vulnerable JWT parsers enabling token forgery (T-002, T-001) or unsafe file-handling libraries facilitating traversal/malicious uploads (T-017, T-016).

SCA gate policy:
- Fail pipeline on vulnerabilities with CVSS >= 7.0 unless approved exception exists.
- Prioritize fixes for internet-exposed attack paths (auth, document handling, file I/O).
- Enforce dependency update cadence and verify fix effectiveness with rescan.


### 2.4 Security Unit and Integration Testing
Objective: enforce security requirements as executable tests in the codebase.

Security Unit Test Plan:

- RBAC Policy Assertions: Use parameterized JUnit tests to validate method-level role decisions (matrix of ADMIN, MANAGER, USER), ensuring a strictly "deny-by-default" response for unmapped roles (T-007).

- Sanitization & Input Logic: Test utility classes responsible for filename sanitization and canonical path verification against known traversal strings (e.g., ../, null bytes). This guarantees the foundational logic resisting AB-009 and AB-010, and threats T-016/T-017, works in isolation.

- Audit Logging Completeness: Validate that security-relevant create/update/delete operations produce immutable audit entries with actor, action, target, and timestamp fields. This directly covers AB-011 and threat T-010.

- Token Handling Logic: Unit test the JWT utility classes to ensure expired, malformed, or incorrectly signed tokens throw the expected cryptographic exceptions natively (AB-003, T-002, T-001)

Security Integration Test Plan:

- Endpoint Authorization: Execute mock HTTP requests to verify that protected endpoints correctly return 401 Unauthorized (missing/invalid tokens) or 403 Forbidden (insufficient roles), addressing session abuse scenarios (AB-004, T-001, T-019).

- Cross-Tenant Restrictions: Instantiate isolated test environments with seeded data (e.g., Mock DB) to assert that cross-department and cross-user document actions are physically denied by the persistence layer, mitigating IDOR and governance bypasses (AB-005, AB-007, AB-008, T-006, T-007).

- Injection Safety in Repositories: Write Spring Data JPA integration tests passing adversarial payloads (e.g., ' OR 1=1--) into query parameters to strictly prove the ORM layer neutralizes them natively (AB-013, T-005, T-006).


### 2.5 DAST and IAST for Running API Security Validation
Objective: validate runtime behavior against real attack patterns, focusing on authorization abuse and injection vectors.

DAST approach (OWASP ZAP):
- Run authenticated scans against a deployed test environment with seeded roles (`ADMIN`, `MANAGER`, `USER`).
- Execute baseline scan on pull requests and deeper active scan in scheduled/nightly pipelines.
- Include targeted attack scripts for project-critical threats.

DAST Focus Areas & Threat Traceability:

- Authentication & Session Abuse: Fuzzing login and registration endpoints to validate rate-limiting and anti-automation controls (AB-001, AB-002, T-001, T-011, T-003). Testing token forgery, expiration, and reuse scenarios against the running gateway (AB-003, AB-004, T-002, T-019, T-001).

- Broken Access Control (BAC): Attempting vertical privilege escalation and horizontal cross-tenant access. This dynamically validates our mitigations against unauthorized profile updates, governance bypasses, and cross-department document access (AB-005, AB-006, AB-007, AB-008, T-006, T-007, T-005).

- Availability Abuse on Department and Delete Flows: Execute high-rate and probing scenarios against create/read/delete endpoints to validate throttling and resource protections (T-013, T-014).

- Injection, Path Traversal, and Malicious Uploads: Sending adversarial payloads (SQLi strings, OS command operators, encoded traversal variants like ../, and suspicious file content patterns) against exposed request parameters and upload flows to verify runtime rejection (AB-009, AB-010, AB-013, T-016, T-017, T-005, T-006).

IAST approach:
- Enable IAST instrumentation during integration test execution in a controlled environment.
- Correlate request input sources to sensitive sinks (filesystem access, command execution, persistence layer).
- Flag confirmed exploitable flows with code-level context to accelerate remediation.

DAST/IAST gate policy:
- Fail release gate on confirmed exploitable `High`/`Critical` findings.
- Require security sign-off for unresolved runtime findings.




### 2.6 Pipeline Integration as Security Gates (GitHub Actions)
Security tooling is integrated as progressive gates before deployment.

Pipeline secret handling expectations:
- Secrets used in CI/CD are injected through Doppler with least-privilege access; application secrets are not stored in source control.

Planned CI security flow:
1. Build and unit tests.
2. Secret scanning (Gitleaks).
3. SAST (SpotBugs and SonarCloud).
4. SCA (Dependabot alerts + OWASP Dependency-Check).
5. Integration tests with security assertions and IAST instrumentation when enabled.
6. DAST against running API test environment.
7. Container image build plus vulnerability scan gate (Trivy, block on High/Critical).
8. Security gate decision before publish/release/deploy.

Gate decision criteria:
- No open `Critical` findings from SAST, SCA, DAST, or IAST.
- No open `High` findings in internet-exposed attack surfaces.
- Required security unit/integration suites must pass.





## 3. Baseline Planned Test Set (Core Flow Tests)

Scope note:
- Core Flow tests are the minimum critical path subset used as baseline regression for primary business and access-control flows.
- `P1` expresses implementation priority/risk and is not equivalent to Core Flow classification.
- Some `P1` tests are intentionally kept in the extended catalog because they validate high-risk but non-baseline scenarios (for example, specialized abuse-path, bootstrap, or deep cross-context checks).

| Test ID | Level | Priority | Planned Verification | Requirement IDs | Threat IDs |
|---|---|---|---|---|---|
| ST-001 | Integration | P1 | Registration success/failure flow with validation boundaries | FR-001, SR-008 | T-003, T-005, T-004, T-006 |
| ST-002 | Unit/Integration | P1 | Identity uniqueness and conflict handling | FR-001, SR-013 | T-003, T-005, T-004, T-008, T-015, T-014 |
| ST-003 | Unit | P1 | Authentication with invalid principal/credential paths | FR-002 | T-001, T-002, T-005, T-011, T-019 |
| ST-004 | Unit/Integration | P1 | Department governance deny-path (non-manager/non-admin updates) | FR-006 | T-009, T-013, T-014, T-007 |
| ST-005 | Integration | P1 | Department listing/read/join authorization policy behavior | FR-006 | T-009, T-013, T-014, T-007 |
| ST-006 | Integration | P1 | Manager scope restrictions on unowned department operations | FR-006 | T-009, T-013, T-014, T-007 |
| ST-007 | Unit/Integration | P1 | Document upload acceptance and policy validation | FR-007 | T-016, T-017, T-009, T-018, T-007 |

## 4. Test Environment and Execution Rules

### 4.1 Test Data and Environment Planning
- Dedicated isolated test environments per pipeline stage.
- Deterministic role and relationship test fixtures.
- Dedicated storage sandbox for filesystem security tests.
- Separate environments for unit/integration and dynamic security tests.


### 4.2 Planned Evidence Package & Naming Conventions

 When a planned test is implemented, record:
1. Test ID and implementation commit reference.
2. Execution context and timestamp.
3. Pass/fail result and defect reference if failed.
4. Linked requirement/threat/control/ASVS references.


Suggested naming format for executable tests:
- `should_<expected_behavior>__req_<ID>__threat_<ID>()`


## 5. Extended Security Test Catalog (Phase 2 Roadmap)

Catalog note:
- This catalog extends the Core Flow baseline with additional coverage depth.
- It includes extra `P1` tests required for full high-risk coverage, plus `P2`/`P3` tests for broader hardening and operational assurance.

| Test ID | Priority | Level | Planned Verification | Requirement IDs | Threat IDs |
|---|---|---|---|---|---|
| ST-101 | P1 | Integration | Protected endpoints reject missing/invalid token with `401` | FR-002, NFR-001, SR-004 | T-001, T-002, T-005, T-011, T-003, T-019 |
| ST-102 | P1 | Integration | Administrative user listing is role-restricted | FR-005, SR-005 | T-006, T-007, T-009, T-005, T-008 |
| ST-103 | P1 | Integration | Unauthorized profile reads are denied | FR-003, SR-005 | T-006, T-007, T-009, T-005 |
| ST-104 | P1 | Integration | Unauthorized profile updates are denied | FR-004, SR-005 | T-006, T-007, T-009, T-005 |
| ST-105 | P1 | Integration | Unauthorized department-scoped upload is denied | FR-007, SR-005 | T-006, T-007, T-016, T-017, T-009, T-018, T-005 |
| ST-106 | P1 | Integration | Manager upload restrictions are enforced by relationship policy | FR-007, SR-005 | T-006, T-007, T-016, T-017, T-009, T-018, T-005 |
| ST-107 | P1 | Integration | Document replace/delete policy matches role/ownership matrix | FR-009, SR-005 | T-001, T-006, T-009, T-013, T-007, T-005 |
| ST-108 | P1 | Unit | Filesystem traversal payloads are rejected | FR-007, FR-009, SR-006 | T-001, T-006, T-009, T-013, T-007, T-016, T-017, T-018 |
| ST-109 | P1 | Integration | Oversized upload is consistently rejected | FR-007, NFR-002 | T-016, T-017, T-009, T-018, T-007 |
| ST-110 | P2 | Integration | Expired token usage is denied | FR-002, NFR-001, SR-003 | T-001, T-002, T-005, T-011, T-019 |
| ST-111 | P2 | Unit/Integration | Filename sanitization policy edge cases | FR-009, SR-007 | T-001, T-006, T-009, T-013, T-007, T-016, T-017 |
| ST-112 | P2 | Integration | Error contracts avoid sensitive leakage | SR-008, SR-009 | T-008, T-009, T-005, T-006 |
| ST-113 | P2 | Unit/Integration | Audit events emitted for all sensitive operations | FR-010, SR-010 | T-010 |
| ST-114 | P2 | Integration | Registration blocks role escalation attempts | FR-001, SR-011 | T-002, T-003, T-005, T-004 |
| ST-115 | P3 | Security/Perf | Anti-automation behavior for auth/register | FR-002, SR-015 | T-001, T-002, T-005, T-011, T-019 |
| ST-116 | P3 | Security | Malicious upload/content policy verification | FR-007, SR-007, SR-017 | T-016, T-017, T-009, T-018, T-007 |
| ST-117 | P3 | Security | Token revocation and post-revocation denial | FR-002, NFR-001, SR-003, SR-016 | T-001, T-002, T-005, T-011, T-019 |
| ST-118 | P1 | Operational/Security | Privileged bootstrap process hardening verification | FR-011, SR-018 | T-004, T-002 |
| ST-119 | P2 | Container | Least-privilege runtime profile verification | NFR-005 | T-017, T-013 |
| ST-120 | P2 | CI/Policy | CI gate coverage for build/test/security checks | NFR-004 | T-002, T-016, T-017 |
| ST-121 | P1 | Integration | Unauthorized cross-context document access is denied | FR-008, SR-005, SR-012 | T-006, T-007, T-009, T-005, T-008, T-013, T-014 |
| ST-122 | P2 | Integration | API response data minimization policy is enforced | NFR-006 | T-008, T-009 |
| ST-123 | P2 | Integration | Migration reproducibility from clean environment | NFR-003 | T-015, T-014 |
| ST-124 | P2 | Unit/Integration | Passwords are persisted only as secure hashes | SR-001 | T-001, T-012 |
| ST-125 | P2 | Unit | Secret-strength policy blocks weak secret configuration | SR-002 | T-002, T-001 |
| ST-126 | P2 | Integration | Department uniqueness constraints enforced | FR-006, SR-013 | T-003, T-008, T-015, T-009, T-013, T-014, T-007 |
| ST-127 | P1 | Security/Integration | SQL injection payloads in query, filter, and body inputs are neutralized by parameterized query handling | SR-014 | T-005, T-006 |
| ST-128 | P2 | CI/Policy | Main workflow publishes versioned Docker image to Docker Hub and records digest | NFR-007 | T-009, T-013 |
| ST-129 | P2 | CI/Policy | GitHub Release created with version tag and artifact links after successful gates | NFR-008 | T-009 |
| ST-130 | P2 | CI/Deployment | Automated deployment to the Terraform-managed AWS environment uses the versioned image and records outcome | NFR-009 | T-013 |
| ST-131 | P1 | CI/Security | Container image vulnerability scan blocks publish or release on High/Critical findings | SR-019 | T-009, T-013 |
| ST-132 | P1 | CI/Security | Doppler secret injection and least-privilege deployment policy validated; no application secrets are stored in source control | SR-020 | T-001, T-002, T-009 |








