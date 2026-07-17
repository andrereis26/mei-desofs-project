# CI/CD Process and DevSecOps Pipeline

## 1. Purpose and Scope

This document explains the Continuous Integration and Continuous Delivery process implemented for Phase 2, Sprint 1. The process operationalizes the security and delivery controls planned in [Planning.md](Planning.md), especially `NFR-004 Automated build and security gates`, `SDR-001 Secure coding checklist`, and `SDR-007 Mandatory DevSecOps pipeline`.

The implemented process focuses on pre-merge and post-merge assurance. It now uses three GitHub Actions workflows: [Feature Branch CI](../../../../.github/workflows/branch-workflow.yml) for lightweight development-branch feedback, [Pull Request Validation](../../../../.github/workflows/pull-request-workflow.yml) for mandatory merge validation, and [Production DevSecOps Pipeline](../../../../.github/workflows/main-workflow.yml) for production-branch pushes and manual dispatch. The main workflow now completes the protected-branch delivery path end to end: it validates the codebase, runs dynamic API security testing, builds the production container image from the validated JAR artifact, blocks publication on High/Critical container findings, publishes immutable Docker Hub tags, creates or updates a GitHub Release, and deploys the immutable image to the Terraform-managed AWS Docker Swarm environment.

## 2. Process Overview

The CI/CD process follows a layered control model:

1. Local development controls reduce preventable issues before code reaches the repository.
2. Branch-level CI gives early feedback while developers are still working outside the protected branch.
3. Pull request controls require explicit secure coding and review evidence through the embedded SDR-001 checklist gate.
4. GitHub Actions validates build correctness, test behavior, static security properties, dependency risk, dynamic API behavior, and code quality.
5. The main workflow promotes only already-validated artifacts into image publication, GitHub Release, and production deployment stages.
6. Pipeline outputs, reports, deployment logs, and documentation provide evidence for review and sprint traceability.

This structure links implementation activity to the Secure Software Development Life Cycle designed in Phase 1: requirements and secure development requirements are transformed into automated gates, and automated gates produce reviewable evidence for Phase 2 implementation.

## 3. Developer and Pull Request Entry Controls

Before CI executes, the project defines controls that shape developer behavior:

- [guidelines.md](guidelines.md) instructs contributors to install pre-commit hooks and avoid bypassing them.
- `.pre-commit-config.yaml` configures Gitleaks as a local secret-detection hook.
- `.github/PULL_REQUEST_TEMPLATE.md` requires developers to document the change, type of change, security impact, consulted ASVS controls, tests, evidence, and dependency/security review considerations.
- The `secure-coding-checklist` job inside `.github/workflows/pull-request-workflow.yml` enforces the `SDR-001` checklist for backend pull requests by failing the pull request when required checklist confirmations are missing.

These controls are important because CI/CD is not treated only as a technical build system. It is also used as a governance mechanism that requires developers and reviewers to explicitly reason about security impact before a change is merged.

### 3.1 Pull Request Template Rationale

The pull request template is implemented in `.github/PULL_REQUEST_TEMPLATE.md`. It was kept as part of the normal Pull Request Validation workflow instead of as a separate manual checklist because the pull request is the natural review boundary where code, discussion, automated checks, and approval evidence are already grouped.

The template is structured to make every change explain its purpose, risk, review needs, and validation evidence before merge:

| Template section | Decision | Rationale |
|---|---|---|
| Summary | Require a short explanation of what changed and why. | Reviewers need business and technical context before evaluating security or correctness impact. |
| Type of change | Classify the change as bug fix, feature, refactor, CI/tooling, or documentation. | Classification helps reviewers understand expected risk and whether the change should affect code, pipeline, documentation, or tests. |
| Secure coding checklist (`SDR-001`) | Require explicit confirmations for secure coding, input/output behavior, authentication/authorization impact, secrets, logging, and tests. | Security checks become part of the standard development workflow instead of being remembered informally after implementation. |
| Security impact (`SDR-009`) | Require the author to classify security impact as none, low, medium, or high. | Risk-based review is more practical than treating every pull request identically. Medium and high impact changes trigger stronger expectations. |
| OWASP ASVS consulted (`SDR-002`) | Ask authors to list relevant ASVS controls and how they influenced the change. | This preserves traceability between Phase 1 security requirements, implementation decisions, and review evidence. |
| Security review checklist | Group review questions by authentication, authorization, validation, file handling, secrets, logging, audit, and dependencies. | The checklist mirrors the main risk areas identified in Phase 1 and helps reviewers look for common failure modes consistently. |
| Dependencies (`SDR-003`) | Require explicit confirmation when third-party items are introduced or changed. | Dependency risk cannot be handled only by automated scanning; new dependencies also need a reason, ownership, and review discipline. |
| Tests and evidence | Require authors to list test runs, pipeline jobs, reports, screenshots, or other validation records. | Merge decisions should be based on verifiable evidence, not only on reviewer confidence or local assumptions. |

This template also supports automation. The embedded `secure-coding-checklist` job in the pull-request workflow reads the pull request body and fails backend pull requests when required secure coding confirmations are missing. This creates a direct link between human review discipline and CI enforcement.

### 3.2 Security-Focused Peer Review Enforcement (SDR-009)

SDR-009 is enforced through a combination of a CODEOWNERS policy file and branch protection rules on `main`.

- The repository uses a CODEOWNERS file to define which reviewers must approve changes to security-impacting paths (backend security boundaries, configuration, and CI workflows). See [.github/CODEOWNERS](.github/CODEOWNERS).
- Branch protection requires pull requests, requires review from Code Owners, and dismisses stale approvals when new commits are pushed. This blocks direct commits to `main` and guarantees security-focused peer review on sensitive changes.

## 4. Main Pipeline Architecture

The project uses three complementary GitHub Actions workflows:

- `.github/workflows/branch-workflow.yml`, named `Feature Branch CI`, for early validation of non-protected development branches.
- `.github/workflows/pull-request-workflow.yml`, named `Pull Request Validation`, for mandatory pull request validation and the embedded SDR-001 checklist gate.
- `.github/workflows/main-workflow.yml`, named `Production DevSecOps Pipeline`, for protected branch pushes and manual dispatch.

### 4.1 Feature Branch CI

The branch pipeline runs on pushes to development branches, excluding `main` and `master`:

```text
on:
  push:
    branches-ignore:
      - main
      - master
```

Its purpose is to give fast feedback before a pull request is opened or updated. This helps developers detect build and secret-management issues while the work is still isolated in a feature branch.

The Feature Branch CI workflow contains four jobs:

| Job | Objective |
|---|---|
| `build` | Runs a lightweight Maven `clean package` build with tests skipped to catch compile or packaging regressions early. |
| `unit-tests` | Runs Maven verification with integration tests skipped so the branch still gets fast unit feedback. |
| `integration-tests` | Runs Maven verification with unit tests skipped so the branch also validates integration behavior. |
| `secret-scan` | Runs Gitleaks to detect committed secrets in branch changes. |

The workflow also uses concurrency control with `cancel-in-progress: true`, so newer pushes to the same branch cancel older branch runs. This keeps the feedback focused on the latest branch state and avoids wasting CI capacity on obsolete commits.

The branch pipeline is intentionally lighter than the mandatory pull request pipeline. It is an early feedback mechanism, not the final merge authority. A branch can pass `Feature Branch CI` and still be required to pass the full Pull Request Validation workflow before merge.

### 4.2 Pull Request Validation

The primary mandatory validation workflow is `.github/workflows/pull-request-workflow.yml`, named `Pull Request Validation`.

The workflow runs on pull requests when they are opened, synchronized, reopened, or marked ready for review.

It executes the same validation chain as the main workflow before the protected-branch-only publication and deployment stages. The workflow also includes the `secure-coding-checklist` job, which enforces the SDR-001 PR description checks before merge.

The workflow also defines concurrency control so that older runs for the same workflow and reference are cancelled when newer commits arrive. This avoids reviewing stale results and keeps the latest commit as the authoritative validation target.

The pipeline uses GitHub-hosted Ubuntu runners, Java 17 through Temurin, Maven caching, and a shared Maven command baseline:

```text
MAVEN_CLI_OPTS: -B -ntp -f backend/pom.xml
```

## 5. Implemented Gates

| Gate | Workflow job | Objective | Evidence produced |
|---|---|---|---|
| Build | `build` | Compile and package the backend to confirm the codebase can produce a build artifact. | Backend classes and JAR artifact. |
| Unit tests | `unit-tests` | Execute unit-level verification through Maven/Surefire. | Surefire reports and JaCoCo coverage artifacts. |
| Integration tests | `integration-tests` | Execute integration-level verification through Maven/Failsafe. | Failsafe reports and integration coverage artifacts. |
| Secure coding checklist | `secure-coding-checklist` | Enforce the SDR-001 PR description requirements for backend changes. | GitHub Actions check result. |
| Secret scanning | `secret-scan` | Detect committed secrets using Gitleaks. | GitHub Actions check result. |
| SAST | `sast-spotbugs` | Detect static security issues using SpotBugs with FindSecBugs. | SpotBugs XML reports. |
| SCA | `sca-dependency-check` | Detect vulnerable third-party dependencies using OWASP Dependency-Check. | Dependency-Check XML and HTML reports. |
| DAST | `dast-zap-baseline` | Start the API in an isolated Docker Compose runtime and run an authenticated OWASP ZAP API baseline scan. | ZAP JSON, HTML, Markdown, and XML reports plus runtime logs. |
| Code quality gate | `sonarcloud` | Submit code, test, and coverage context to SonarQube/SonarCloud and block the pipeline when the quality gate fails. | SonarCloud quality gate result, quality information, and summary output. |
| Required aggregate gate | `required-devsecops-gates` | Fail the pipeline if any required upstream gate fails. | Test, security, and Sonar summaries plus final pass/fail status. |
| Container image build and publish | `publish-container-image` | Build the production image from the validated JAR artifact, tag it immutably, and publish it only after container scanning passes. | Published Docker Hub tags, image digest, and metadata artifact. |
| Container image vulnerability gate | `publish-container-image` | Run Trivy against the release image and block publication on High/Critical findings. | Trivy SARIF and text reports, plus GitHub code scanning upload. |
| Release orchestration | `github-release` | Create or update a GitHub Release for the immutable build. | GitHub Release with version tag, release notes, and image digest reference. |
| Production deployment | `deploy-production` | Deploy the immutable image to the Terraform-managed AWS Docker Swarm environment and validate readiness. | Deployment job logs, Swarm update status, and post-deploy health-check outcome. |

The final job is named `required-devsecops-gates`. This is the status check that should be configured as mandatory in branch protection. By aggregating all upstream gates, the branch protection rule can depend on one stable required check while still enforcing the full validation chain.

## 6. Security Tooling and Failure Rules

### 6.1 Secret Scanning

Secret detection is implemented in two layers:

- Local pre-commit scanning through Gitleaks.
- CI pull request and branch scanning through the Gitleaks GitHub Action.

This reduces the chance that credentials, API keys, tokens, or sensitive configuration values are committed and merged.

### 6.2 Static Application Security Testing

SAST is implemented with SpotBugs and FindSecBugs in `backend/pom.xml`. The configuration uses high-effort analysis, a medium threshold, XML output, and `failOnError=true`.

The pipeline first compiles the backend and then runs:

```text
mvn -B -ntp -f backend/pom.xml -Dmaven.test.skip=true -Djacoco.skip=true -Dspotbugs.failOnError=true spotbugs:check
```

Documented exclusions are centralized in `backend/config/spotbugs-exclude.xml`. This keeps accepted false positives explicit instead of hiding them in pipeline behavior.

### 6.3 Software Composition Analysis

Dependency risk is controlled through OWASP Dependency-Check. The Maven plugin is configured to generate all report formats and fail the build when a dependency finding reaches CVSS 9 or above.

The CI workflow requires the repository secret `NVD_API_KEY` before running this gate. This is necessary because the Dependency-Check analyzer relies on NVD data and unauthenticated or missing configuration can produce unreliable or failed scans.

### 6.4 SonarQube/SonarCloud Quality Gate

The SonarQube job depends on the build, unit test, and integration test jobs. It downloads compiled classes and test artifacts so that the analysis has context for Java bytecode, JUnit reports, and JaCoCo coverage data.

The job requires `SONAR_TOKEN` and uses `sonar-project.properties` to identify the project and organization. The scanner waits for the SonarQube/SonarCloud quality gate result and exits with a failing status when the gate is not passed, so `required-devsecops-gates` also fails. This supports centralized visibility over maintainability, reliability, coverage, and security-related quality indicators while keeping the gate enforceable in CI.

### 6.5 Dynamic Application Security Testing

`SDR-008` runtime security testing is implemented through the `dast-zap-baseline` job in both the pull request and protected-branch workflows.

The DAST job:

1. Downloads the validated backend JAR produced by the `build` job.
2. Builds the same production-style runtime image using `backend/Dockerfile.prod`.
3. Starts PostgreSQL, Redis, and the API through Docker Compose with generated CI-only runtime secrets.
4. Waits for `GET http://127.0.0.1:9090/actuator/health/readiness` (management port) to report `UP`.
5. Authenticates as the seeded `bootstrap_admin` user to obtain a real JWT.
6. Runs OWASP ZAP against the local API using the OpenAPI definition in `.github/dast/openapi.yml`.
7. Injects the JWT into scan requests through ZAP's request-header replacer so protected endpoints are exercised as authenticated traffic.
8. Archives ZAP reports and runtime logs.
9. Fails the gate when the ZAP JSON report contains High or Critical findings.

The job runs ZAP in safe baseline mode for pull request suitability. This avoids destructive active attack behavior while still validating deployed runtime headers, error responses, reachable endpoint behavior, and authenticated request handling. Deeper active DAST remains suitable for a scheduled/nightly workflow or a dedicated non-production environment.

### 6.6 Container Image Security Policy

`SR-019` is implemented in the protected-branch pipeline by scanning the production image before any registry publication occurs.

The `publish-container-image` job:

1. Downloads the already-built backend JAR artifact from the validated `build` job.
2. Builds the production image with `backend/Dockerfile.prod`, which uses a dedicated runtime-only image layout instead of rebuilding the application from source in the release stage.
3. Runs Trivy in SARIF mode for archival and GitHub security upload.
4. Runs Trivy again as a blocking gate for High/Critical findings.
5. Publishes the image to Docker Hub only if the blocking Trivy gate succeeds.
6. Records the immutable digest for release and deployment traceability.

This flow was chosen because it supports the specific requirement from `SR-019`: publication must be blocked when the container image contains High or Critical vulnerabilities.

### 6.7 Release and Deployment Promotion

`NFR-007`, `NFR-008`, `NFR-009`, and `SR-020` are implemented in sequence after the required verification gates pass.

#### Docker image publication

- The protected-branch workflow computes an immutable version tag from the Maven project version and GitHub run number.
- The image is published to Docker Hub using both the immutable version tag and a rolling `latest` tag.
- The immutable digest is captured with Docker tooling and saved as workflow evidence.

#### GitHub Release creation

- The `github-release` job creates or updates a release named with the immutable version tag.
- Release notes record the image reference, immutable digest, workflow run URL, and the fact that the blocking Trivy image gate passed before publication.

#### Terraform-managed AWS deployment

- The `deploy-production` job runs only after the protected-branch validation, image publication, and GitHub Release stages succeed.
- Terraform provisions the target EC2 host in `eu-west-3`, including networking, an Elastic IP, IMDSv2 enforcement, a least-privilege security group, and bootstrap installation of Docker, Docker Compose, Doppler, and Swarm.
- Deployment uses SSH with pinned host trust, a least-privilege deployment user, and a synchronized deployment bundle from the repository.
- The remote deployment script renders the Swarm compose file with Doppler-managed variables, applies it with `docker stack deploy`, waits for the API service to converge to `2/2` replicas, checks the public readiness endpoint, and triggers `docker service rollback` if readiness validation fails.
- The Swarm stack uses two API replicas, `start-first` rolling updates, update parallelism `1`, rollback-aware update policy, health checks, restart policies, and resource reservations/limits.

## 7. Evidence Collection

The pipeline preserves evidence in multiple forms:

- Completed pull request template sections, including secure coding, security impact, ASVS consultation, dependency review, and validation evidence.
- Feature Branch CI results for feature-branch pushes, including build, unit test, integration test, and Gitleaks outcomes.
- Maven test reports for unit and integration tests.
- JaCoCo coverage reports.
- SpotBugs XML reports.
- OWASP Dependency-Check XML and HTML reports.
- OWASP ZAP JSON, HTML, Markdown, and XML reports.
- DAST runtime logs from the Docker Compose test environment.
- Trivy SARIF and text reports for the release image.
- Docker Hub image references and immutable digests captured in the workflow metadata artifact.
- GitHub Release notes for protected-branch promotions.
- Deployment job logs and Swarm service update status for production promotions.
- GitHub Actions job results for Gitleaks, SonarQube, and the required aggregation gate.
- Summary output generated by `.github/scripts/test-summary.py`, `.github/scripts/security-summary.py`, and `.github/scripts/sonar-summary.py`.

Sprint 1 validation evidence is consolidated in [summary.md](summary.md) and in the specific SDR documentation. The CI/CD process now also depends on Docker Hub, SSH, and Doppler repository secrets to preserve the protected-branch release and deployment evidence chain.

## 8. Traceability to Sprint 1 Planning

The implemented CI/CD process directly supports the following Sprint 1 planning items:

| Planning item | CI/CD contribution |
|---|---|
| `NFR-004 Automated build and security gates` | Branch-level build/test/secret checks and mandatory pull request build, unit test, integration test, SAST, SCA, DAST, secret scan, SonarQube, and artifact scan gates execute automatically. |
| `SDR-001 Secure coding checklist` | Pull request template and embedded checklist gate require secure coding confirmations. |
| `SDR-003 Third-party review discipline` | Dependency changes are surfaced through the PR template and reinforced by dependency scanning. |
| `SDR-004 Secret leak prevention controls` | Gitleaks runs locally through pre-commit and centrally in CI. |
| `SDR-005 Automated static analysis and review gates` | SpotBugs and FindSecBugs run as mandatory SAST gates. |
| `SDR-006 Continuous dependency risk control` | Dependency-Check and Dependabot support recurring dependency visibility. |
| `SDR-007 Mandatory DevSecOps pipeline` | The full workflow set implements the required mandatory pre-merge gate chain. |
| `SDR-008 Security test expansion` | The DAST job runs an authenticated OWASP ZAP API baseline scan against a live Docker Compose runtime and blocks High/Critical runtime findings. |
| `SDR-010 Documentation and evidence upkeep` | Pipeline evidence and Sprint 1 documentation preserve reviewable proof of implementation. |
| `NFR-007 Docker image publishing` | Protected-branch automation publishes immutable Docker Hub images and records the image digest. |
| `NFR-008 GitHub Release creation` | Protected-branch automation creates or updates GitHub Releases for immutable builds. |
| `NFR-009 AWS deployment` | Protected-branch automation deploys the immutable image to the Terraform-managed AWS host through a controlled Swarm rollout. |
| `SR-019 Container image security policy` | Trivy blocks image publication on High/Critical findings and archives the scan reports. |
| `SR-020 Doppler-managed secrets` | Deployment renders the production stack with Doppler-managed variables instead of storing application secrets in the repository. |

## 9. Required Repository Configuration

The pipeline requires the following repository and branch settings:

- `SONAR_TOKEN` must be configured as a GitHub Actions secret.
- `NVD_API_KEY` must be configured as a GitHub Actions secret.
- `GITLEAKS_LICENSE` may be configured if required by the selected Gitleaks action usage.
- `DOCKERHUB_USERNAME` and `DOCKERHUB_TOKEN` must be configured for immutable image publication.
- `AWS_SSH_HOST`, `AWS_SSH_PORT`, `AWS_SSH_USER`, `AWS_SSH_PRIVATE_KEY`, and `AWS_SSH_KNOWN_HOSTS` must be configured for remote deployment with pinned host trust.
- `AWS_HEALTHCHECK_URL` must be configured for post-deployment readiness validation.
- `DOPPLER_PROJECT`, `DOPPLER_CONFIG`, and `DOPPLER_TOKEN` must be configured for deployment-time secret injection.
- Branch protection must require the `required-devsecops-gates` status check before merge.
- Branch protection must require pull requests, require review from Code Owners, and dismiss stale approvals to enforce SDR-009 with [.github/CODEOWNERS](.github/CODEOWNERS).
- The `production` GitHub environment should be used to scope or protect deployment-time secrets and approvals if the team wants an additional promotion boundary.
- Pull requests affecting backend code must use the project PR template and complete the secure coding checklist embedded in the pull-request workflow.

Without these settings, the workflow may run but the intended governance and promotion model is incomplete because image publication or deployment would fail or could bypass the expected evidence chain.

## 10. Limitations and Future Improvements

The current process is intentionally focused on Sprint 1 priorities. The main remaining limitations are:

- The deployment target is a single server, so the two API replicas provide rolling-update continuity but not multi-node fault tolerance.
- Uploaded document storage is still local to the deployment node, which is why the API service is pinned to the Swarm manager node.
- The implemented DAST stage is a safe OWASP ZAP baseline/API scan; destructive active scans and IAST instrumentation are not yet part of the mandatory pipeline.
- Release artifact signing, provenance attestations, and SBOM publication are not yet implemented.
- Dependency-Check and Trivy still depend on external vulnerability data availability.

Future iterations should consider network-attached or object-based document storage, multi-node Swarm or a different orchestrator, scheduled active DAST, IAST instrumentation, SBOM generation, artifact signing, provenance attestations, and stricter environment promotion policies.

## 11. Conclusion

The implemented CI/CD process converts Sprint 1 security planning into enforceable DevSecOps controls. It provides early branch-level feedback through `Feature Branch CI`, validates code quality, test behavior, dependency risk, secret hygiene, static security properties, and runtime API behavior before merge through `Pull Request Validation`, and uses `Production DevSecOps Pipeline` to promote validated artifacts into immutable image publication, GitHub Release creation, and controlled Terraform-backed AWS deployment. The final required gate still provides a single branch-protection control point while the protected-branch stages preserve the operational evidence needed for auditability, traceability, and rollback-friendly production promotion.
