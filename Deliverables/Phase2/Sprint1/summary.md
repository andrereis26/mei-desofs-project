# Phase 2 - Sprint 1 Summary

## 1. Executive Summary

Phase 2 Sprint 1 moves the project from the Phase 1 SSDLC planning baseline into implementation, validation, and evidence collection. The sprint objective is to preserve the security requirements already defined in Phase 1 while converting selected functional, non-functional, security, and secure development requirements into implemented controls, repeatable engineering practices, and reviewable evidence.

The primary source for Sprint 1 content is [Planning.md](Planning.md), since all what was done for this sprint was planned there. 

The CI/CD and DevSecOps process is documented separately in [CI/CD Process and DevSecOps Pipeline](ci-cd-process.md). That document explains the branch-level CI workflow, the mandatory pull request pipeline, and the protected-branch promotion path that now enforces automated build, test, static analysis, dependency analysis, secret scanning, DAST, container image scanning, immutable image publication, GitHub Release creation, and Terraform-backed AWS deployment.

The deployment and infrastructure setup is documented in the [deployment README](../../../deployment/README.md) and the [Terraform setup guide](../../../infrastructure/terraform/README.md), which explain how to deploy the application to AWS using the published container images and the Terraform environment.

### Phase 2 Sprint 1 Activities Summary

Sprint 1 work is organized around the following themes derived from [Planning.md](Planning.md):

- Identity, authentication, and session controls: registration, login, token expiration, token revocation, rate limiting, stateless authorization, and minimal response disclosure.
- User profile and administrative capabilities: profile retrieval, profile update, administrative directory work, RBAC, and prevention of client-driven role escalation.
- Department governance and auditability: department management, membership flows, immutable audit trail generation, audit completeness, non-predictable identifiers, and uniqueness constraints.
- Document handling and storage security: upload metadata, document lifecycle maintenance, catalog and retrieval work, path traversal protection, safe filename handling, upload limits, and malicious upload protections.
- Platform bootstrap and runtime posture: controlled initial platform bootstrap, least-privilege runtime execution, injection-safe data access, and secure bootstrap policy.
- DevSecOps and delivery governance: secure coding checklist, ASVS consultation, secret leak prevention, static analysis, dependency risk control, DAST, mandatory CI/CD gates, container image release controls, Doppler-managed deployment, peer review, and documentation upkeep.

### Information Organization Rationale

Sprint 1 documentation follows the same traceability principle established in Phase 1:

**planning item -> implementation area -> validation activity -> evidence -> sprint summary**

## 2. Scope of this Delivery

The delivery is prepared to cover the following Sprint 1 outputs:

1. Sprint backlog planning with requirement status, workstream, priority, and dependencies.
2. Functional implementation evidence for completed `FR` items.
3. Non-functional implementation evidence for completed `NFR` items.
4. Security control evidence for completed `SR` items.
5. Secure development and DevSecOps evidence for completed `SDR` items.
6. CI/CD and branch pipeline documentation.
7. Secure coding, review, dependency, testing, and documentation governance.
8. Sprint-level handoff notes for remaining or partially covered work.

## 3. Current Delivered Artifacts

- [Planning.md](Planning.md)
- [ci-cd-process.md](ci-cd-process.md)
- [guidelines.md](guidelines.md)
- [../../../infrastructure/terraform/README.md](../../../infrastructure/terraform/README.md)
- [../../../deployment/README.md](../../../deployment/README.md)
- [../../../.github/dast/openapi.yml](../../../.github/dast/openapi.yml)
- [../../../.github/dast/docker-compose.dast.yml](../../../.github/dast/docker-compose.dast.yml)
- [../../../.github/workflows/branch-workflow.yml](../../../.github/workflows/branch-workflow.yml)
- [../../../.github/workflows/pull-request-workflow.yml](../../../.github/workflows/pull-request-workflow.yml)
- [../../../.github/workflows/main-workflow.yml](../../../.github/workflows/main-workflow.yml)
- [../../../.github/scripts/*](../../../.github/scripts/*)
- [../../../.github/actions/*](../../../.github/actions/*)
- [../../../.github/dependabot.yml](../../../.github/dependabot.yml)
- [diagrams/physicalView.svg](diagrams/physicalView.svg)
- [diagrams/logicalView.svg](diagrams/logicalView.svg)

## 4. Sprint 1 Evidence Register

| Evidence area | Related planning items | Current artifact |
|---|---|---|
| Sprint planning and workstreams | All `FR`, `NFR`, `SR`, and `SDR` items | [Planning.md](Planning.md) |
| CI/CD and DevSecOps process | `NFR-004`, `NFR-007`, `NFR-008`, `NFR-009`, `SR-019`, `SR-020`, `SDR-001`, `SDR-004`, `SDR-005`, `SDR-006`, `SDR-007`, `SDR-008`, `SDR-010` | [ci-cd-process.md](ci-cd-process.md), [deployment README](../../../../deployment/README.md), [Terraform setup guide](../../../../infrastructure/terraform/README.md) |
| Runtime DAST evidence | `SDR-008`, `SR-004`, `SR-005`, `SR-008`, `SR-009` | [ZAP OpenAPI scan definition](../../../.github/dast/openapi.yml), [DAST Docker Compose override](../../../.github/dast/docker-compose.dast.yml), `dast-zap-baseline` workflow reports |
| AWS runtime physical view | `NFR-005`, `NFR-009`, `SR-020` | [Physical deployment diagram](diagrams/physicalView.svg), [Visual Paradigm source](diagrams/physicalView.vpp) |
| Logical application view | Core `FR` items, `SR-005`, `SR-010` | [Logical view diagram](diagrams/logicalView.svg), [Visual Paradigm source](diagrams/logicalView.vpp) |
| Secure coding checklist and pull request template | `SDR-001`, `SDR-002`, `SDR-003`, `SDR-009`, `SDR-010` | [CI-CD documentation](ci-cd-process.md), [Guidelines](guidelines.md), `.github/PULL_REQUEST_TEMPLATE.md` |
| Third-party review discipline | `SDR-003`, `SDR-006` | [CI-CD documentation](ci-cd-process.md), [Guidelines](guidelines.md) |
| Security-focused peer review | `SDR-009` | [CI-CD documentation](ci-cd-process.md), [Guidelines](guidelines.md) |
| Department, audit, schema, identifiers, and uniqueness evidence | `FR-006`, `FR-010`, `NFR-003`, `SR-010`, `SR-012`, `SR-013` | [DepartmentEntity](backend/src/main/java/com/desofs/project/department/entities/DepartmentEntity.java) for e.g. |
| Identity, authentication, session, and rate-limiting evidence | `FR-001`, `FR-002`, `NFR-001`, `NFR-006`, `SR-001`, `SR-002`, `SR-003`, `SR-004`, `SR-015`, `SR-016` | [AuthController](backend/src/main/java/com/desofs/project/user/controller/AuthController.java), [SecurityConfig](backend/src/main/java/com/desofs/project/config/SecurityConfig.java), [JwtService](backend/src/main/java/com/desofs/project/config/JwtService.java), [JwtAuthenticationFilter](backend/src/main/java/com/desofs/project/config/JwtAuthenticationFilter.java), [TokenRevocationService](backend/src/main/java/com/desofs/project/user/services/TokenRevocationService.java), [AuthRateLimitGuard](backend/src/main/java/com/desofs/project/shared/ratelimit/AuthRateLimitGuard.java), [UserService](backend/src/main/java/com/desofs/project/user/services/UserService.java), [application.yml](backend/src/main/resources/application.yml) |
| User profile, administration, and authorization evidence | `FR-003`, `FR-004`, `FR-005`, `SR-005`, `SR-008`, `SR-011` | [UserController](backend/src/main/java/com/desofs/project/user/controller/UserController.java), [UserService](backend/src/main/java/com/desofs/project/user/services/UserService.java), [UserDto](backend/src/main/java/com/desofs/project/user/dtos/UserDto.java) |
| Document upload, catalog, lifecycle, and storage security evidence | `FR-007`, `FR-008`, `FR-009`, `NFR-002`, `SR-006`, `SR-007`, `SR-009`, `SR-017` | [DocumentController](backend/src/main/java/com/desofs/project/document/controller/DocumentController.java), [DocumentService](backend/src/main/java/com/desofs/project/document/services/DocumentService.java), [FileStorageService](backend/src/main/java/com/desofs/project/infrastructure/filesystem/FileStorageService.java), [DocumentDto](backend/src/main/java/com/desofs/project/document/dtos/DocumentDto.java), [application.yml](backend/src/main/resources/application.yml) |
| Bootstrap, runtime, and persistence security evidence | `FR-011`, `NFR-005`, `SR-014`, `SR-018` | [StartupDataBootstrap](backend/src/main/java/com/desofs/project/config/StartupDataBootstrap.java), [Dockerfile](backend/Dockerfile), [Dockerfile.prod](backend/Dockerfile.prod), [UserRepositoryImpl](backend/src/main/java/com/desofs/project/user/repositories/UserRepositoryImpl.java), [DocumentRepositoryImpl](backend/src/main/java/com/desofs/project/document/repositories/DocumentRepositoryImpl.java) |
| ASVS consultation and security test expansion evidence | `SDR-002`, `SDR-008` | - |

## 5. Current Consolidated Evidence

The following evidence is currently consolidated in this summary to avoid maintaining contributor-specific evidence files:

- `FR-006 Department management and membership`: implemented through `DepartmentController`, `DepartmentService`, department repositories, DTOs, and persistence mapping. Create, list, get, update, delete, and join flows are covered.
- `FR-010 Immutable audit trail generation` and `SR-010 Audit event completeness`: implemented through `AuditService` and `AuditRepositoryImpl`. Audit events require actor, action, target, and timestamp, and application-level repository behavior rejects saves with an existing audit id.
- `NFR-003 Repeatable schema evolution`: implemented through Flyway migrations in `backend/src/main/resources/db/migration`, with Hibernate DDL validation and Flyway enabled in production configuration.
- `NFR-004 Automated build and security gates` and `SDR-007 Mandatory DevSecOps pipeline`: implemented through `.github/workflows/branch-workflow.yml`, `.github/workflows/pull-request-workflow.yml`, and `.github/workflows/main-workflow.yml`.
- `NFR-007 Docker image publication`: implemented through the `publish-container-image` job in `.github/workflows/main-workflow.yml`, which builds the production runtime image from the validated JAR artifact, applies immutable version tagging, and records the published digest.
- `NFR-008 GitHub Release creation`: implemented through the `github-release` job in `.github/workflows/main-workflow.yml`, which creates or updates a release named with the immutable version tag and records the published image reference and digest.
- `NFR-009 AWS deployment`: implemented through the `deploy-production` job in `.github/workflows/main-workflow.yml`, the Terraform environment in `infrastructure/terraform/envs/prod`, and the Swarm deployment assets in `deployment/`. The AWS runtime topology is documented in the [physical deployment diagram](diagrams/physicalView.svg), with the editable Visual Paradigm source in [physicalView.vpp](diagrams/physicalView.vpp).
- `SDR-001 Secure coding checklist`: implemented through `.github/PULL_REQUEST_TEMPLATE.md` and the `secure-coding-checklist` job inside `.github/workflows/pull-request-workflow.yml`. The pull request template also records change purpose, change type, security impact, ASVS consultation, dependency review, and validation evidence.
- `SDR-008 Security test expansion`: implemented in CI through the `dast-zap-baseline` job in the pull request and protected-branch workflows. The job starts the API with Docker Compose, authenticates with a seeded admin user, runs OWASP ZAP against the OpenAPI endpoint definition, archives JSON/HTML/Markdown/XML reports, and blocks High/Critical runtime findings.
- `SR-019 Container image security policy`: implemented through the blocking Trivy image scan in `publish-container-image`, with archived SARIF and text reports.
- `SR-020 Doppler-managed secrets`: implemented through deployment-time rendering of the production Swarm compose file with Doppler-managed variables and without storing application secrets in source control.
- `SR-012 Non-predictable identifiers`: implemented through UUID identifiers in entities and migrations.
- `SR-013 Uniqueness constraints`: implemented with application-level duplicate checks and database unique constraints for users and departments.

## 7. Sprint 1 Handoff Priorities For Next Sprint

- Improve pipeline (workflow) outputs (summaries of the jobs/steps performed, links to reports/artifacts, and clear pass/fail status) to make it easier for reviewers to understand what was done and where to find evidence.
- In case of reported vulnerabilities, provide clear guidance on how to interpret the report, how to determine if the finding is a false positive or a real issue, and how to remediate it if it's valid.
- Production logging and monitoring controls.