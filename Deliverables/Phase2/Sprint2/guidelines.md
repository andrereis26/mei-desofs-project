# Guidelines
These guidelines keep contributions secure, consistent, and traceable. Follow them for every change.

## Setup
DO NOT IGNORE THE PRE-COMMIT HOOKS! They prevent secret leaks and enforce basic security checks. Follow these steps:
1. Clone the repository:
   ```bash
   git clone git@github.com:Departamento-de-Engenharia-Informatica/desofs2026_wed_ffs_2.git
   ```
2. Install pre-commit:
    ```bash
    # linux
    sudo apt install pre-commit
    # or
    pip install pre-commit

    # macOS
    brew install pre-commit

    # Windows
    pip install pre-commit
    ```
3. Install pre-commit hooks:
   ```bash
   cd to-this-repo

   # linux/macOS/windows (Git Bash)
   pre-commit install

   # Windows if the above command does not work (PowerShell)
   python -m pre_commit install
   ```

We are using pre-commit hooks for the following checks:
- **Secret detection**: prevents committing sensitive information (API keys, passwords, tokens, etc) with Gitleaks.

## Core developer principles
- **Security-first**: treat security requirements as functional requirements. Review Phase 1 docs before changing auth, authorization, file handling, or audit logic.
- **Small, focused changes**: avoid unrelated refactors. Keep diffs minimal and reviewable.
- **DDD boundaries**: keep domain logic in `domain/*`, HTTP concerns in `interfaces/*`, and IO in `infrastructure/*`. Services and controllers return DTOs only.
- **Fail fast, validate early**: reject invalid inputs at the API boundary and enforce domain constraints explicitly.

## Security expectations (mandatory)
- **Deny-by-default authorization**: enforce RBAC plus object-level checks in service/domain policies (roles: ADMIN, MANAGER, USER).
- **No client-side role control**: never trust role or privilege fields from requests. Role assignment must be server-side only.
- **Safe error contracts**: avoid leaking internal details, storage paths, or access decisions in API responses.
- **File handling hardening**: sanitize filenames, enforce canonical path checks, and respect upload size limits. Never build filesystem paths from raw user input.
- **Audit coverage**: user/department/document create, update, delete operations must generate audit entries with actor, action, target, and timestamp.
- **Secrets handling**: store secrets in Doppler or CI secrets only. Keep `.env` files and credentials out of git. If a secret leaks, rotate it immediately.

## Threat modeling and traceability
- For changes touching auth, departments, documents, file IO, or audits, review:
    - `../../Phase1/threat-models-overview.md`
    - `../../Phase1/abuse-and-misuse-case-catalog.md`
    - `../../Phase1/requirements.md`
- Reference requirement IDs (FR/NFR/SR) and threat IDs (T-*) in PR notes or test names when relevant.

## Dependencies and third-party code
- Review any new dependency or external snippet for maintenance, CVEs, and license fit.
- Pin versions in `backend/pom.xml` and document why a new dependency is required.
- Run the local dependency scan before opening a pull request that adds, removes, or upgrades backend dependencies.

## Local secure development tooling

Use the same security tools locally that are enforced in CI. This gives faster feedback and supports `SDR-005`, `SDR-006`, `SDR-007`, and `SDR-013`.

### SonarLint setup

SonarLint provides local static analysis feedback before code is pushed to GitHub and before the SonarCloud quality gate runs.

1. Install Java 17 and open the repository root in your IDE.
2. Install the SonarLint extension:
   - Visual Studio Code: install **SonarQube for IDE** from the Extensions view.
   - IntelliJ IDEA: install **SonarQube for IDE** from `Settings > Plugins > Marketplace`.
3. Enable connected mode so local rules match the project quality profile:
   - Select SonarCloud/SonarQube connected mode in the extension.
   - Authenticate with a SonarCloud token from your SonarCloud account.
   - Use organization `desofs2026-wed-ffs-2`.
   - Bind the project key `desofs2026-wed-ffs-2_desofs2026-wed-ffs-2`.
4. Confirm the binding matches [sonar-project.properties](../../../../sonar-project.properties):
   ```properties
   sonar.organization=desofs2026-wed-ffs-2
   sonar.projectKey=desofs2026-wed-ffs-2_desofs2026-wed-ffs-2
   ```
5. Build the backend once so Java bytecode exists for rules that need compiled classes:
   ```bash
   cd backend
   mvn -DskipTests package
   ```
6. Open or edit files under `backend/src/main/java`. SonarLint findings should appear in the IDE Problems/SonarLint panel.
7. Fix all new SonarLint issues before committing. If a finding is a false positive, explain the reason in the pull request and wait for reviewer approval before suppressing it in code.

### Local OWASP Dependency-Check

The backend uses `org.owasp:dependency-check-maven` in [backend/pom.xml](../../../../backend/pom.xml). The local command below uses the same Maven plugin configuration as CI: it generates reports in all configured formats, fails on CVSS 9 or higher, disables OSS Index, and stores the vulnerability database cache in `${user.home}/.owasp/dependency-check-data`.

1. Get an NVD API key from the NVD website and store it only in your local shell environment or secret manager. Do not commit it.
2. Set the key for the current terminal session:
   ```bash
   # Linux/macOS
   export NVD_API_KEY=your-local-nvd-api-key

   # Windows PowerShell
   $env:NVD_API_KEY="your-local-nvd-api-key"
   ```
3. Run the scan from the repository root:
   ```bash
   mvn -B -ntp -f backend/pom.xml org.owasp:dependency-check-maven:check
   ```
   Or run it from the backend directory:
   ```bash
   cd backend
   mvn -B -ntp org.owasp:dependency-check-maven:check
   ```
4. Review the generated reports:
   - HTML report: `backend/target/dependency-check-report.html`
   - XML report: `backend/target/dependency-check-report.xml`
   - JSON report: `backend/target/dependency-check-report.json`
5. If the first run is slow, let it finish. Dependency-Check downloads and caches NVD data locally under `${user.home}/.owasp/dependency-check-data`; later runs reuse that cache.
6. Treat any High or Critical dependency finding as blocking unless the team documents and approves an exception. Prefer upgrading or replacing the dependency. If no fixed version exists, document the affected component, CVE, exposure in this application, compensating controls, and planned follow-up in the pull request.
7. If the scan fails because `NVD_API_KEY` is missing, invalid, or rate-limited, refresh the key or rerun later. Do not bypass the CI SCA gate.

## Testing and quality gates
- Add or update tests for every security-relevant change. Include negative-path tests (401/403, forbidden object access, invalid input).
- Use existing test conventions (JUnit 5, Mockito, Spring MockMvc).
- Run locally when possible:
  ```bash
  cd backend && mvn test
  ```
- Before opening a pull request for backend code, also run SonarLint in the IDE and run Dependency-Check when dependencies changed.
- CI gates are mandatory (secret scanning, SAST, SCA, container scanning). Do not merge when any High/Critical finding is open.

## Documentation and deliverables
- If you change security behavior or controls, update the related deliverables.
- Respect the dependency chain in Deliverables:
    1. Threat models
    2. Abuse and misuse case catalog
    3. Requirements
    4. Security test strategy and plan
    5. Traceability matrix

## Deployment and infrastructure changes
- Follow the instructions in `deployment/README.md` and `infrastructure/terraform/README.md` when touching release or infrastructure files.
