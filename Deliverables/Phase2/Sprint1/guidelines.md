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

## Testing and quality gates
- Add or update tests for every security-relevant change. Include negative-path tests (401/403, forbidden object access, invalid input).
- Use existing test conventions (JUnit 5, Mockito, Spring MockMvc).
- Run locally when possible:
  ```bash
  cd backend && mvn test
  ```
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