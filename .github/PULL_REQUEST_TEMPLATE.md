## Summary
Describe the change and why it is needed.

## Type of change
- [ ] Bug fix
- [ ] Feature
- [ ] Refactor / cleanup
- [ ] CI / tooling
- [ ] Documentation

## Secure coding checklist (SDR-001)
- [ ] I applied OWASP Secure Coding Practices and CERT Oracle Java rules to this change.
- [ ] I validated input handling, validation, and output/error behavior for affected endpoints.
- [ ] I validated authentication/authorization/session impact (or marked N/A in notes).
- [ ] I confirmed no secrets are hardcoded and no sensitive data is logged.
- [ ] I added or updated automated tests (unit/integration/security) or justified why not needed.

## Security impact (SDR-009)
Security impact level (select one):
- [ ] None
- [ ] Low
- [ ] Medium
- [ ] High

If Medium or High:
- [ ] ASVS consulted
- [ ] Added/updated security tests
- [ ] Requested security reviewer (required for High)

## OWASP ASVS consulted (SDR-002)
List consulted ASVS controls and explain how they influenced this change.

## Security review checklist
Authentication / session / JWT (if applicable)
- [ ] Token creation and validation remain correct (expiry, signature, algorithm, issuer/audience when used).
- [ ] No sensitive authentication data is logged.

Authorization (RBAC + object-level)
- [ ] Endpoints are protected by default and public routes are explicit.
- [ ] Object-level authorization checks exist for affected read/write flows.
- [ ] No privilege escalation path was introduced.

Input validation and safe output
- [ ] DTO/domain validation rejects invalid or adversarial input deterministically.
- [ ] Error responses do not leak internals (stack traces, SQL, filesystem paths, implementation hints).

File handling (if applicable)
- [ ] Filename/path sanitization and traversal protection remain enforced.
- [ ] Upload limits and file access controls remain enforced.

Secrets / crypto / configuration
- [ ] No secrets committed and configuration uses environment/secret management.
- [ ] No insecure cryptographic primitive or custom crypto introduced.

Logging / audit
- [ ] Security-relevant actions remain auditable (actor, action, target, timestamp).
- [ ] Logs avoid secrets and unnecessary sensitive data.

Dependencies (SDR-003)
- [ ] No new third-party items introduced.
- [ ] If third-party items were introduced/changed, SDR-003 review evidence and approvals were updated.

## Tests and evidence
List the validation evidence for this PR (tests, pipeline jobs, screenshots, reports, etc.).
