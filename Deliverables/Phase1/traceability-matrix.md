# Phase 1 - Traceability Matrix

## 1. Matrix Objective
This matrix links each requirement to:
- Threat(s)
- Planned control(s)
- Planned test(s)
- OWASP ASVS reference(s)

## 2. Planned Future Controls
| Planned Control ID | Description |
|---|---|
| PC-001 | Anti-automation and rate-limiting controls for public authentication endpoints |
| PC-002 | Token revocation/session invalidation model with pluggable storage (Redis default) |
| PC-003 | Malicious upload/content security controls |
| PC-004 | Secure privileged-bootstrap policy |
| PC-005 | Secret lifecycle policy (strength + rotation + non-default enforcement) |
| PC-006 | Audit coverage gate in code review/tests/CI |
| PC-007 | Container image publication and versioning policy (Docker Hub) |
| PC-008 | Automated GitHub Release creation with artifact linkage |
| PC-009 | Automated deployment pipeline to Terraform-managed AWS infrastructure with environment gating |
| PC-010 | Container image vulnerability scanning gate before publish/release |
| PC-011 | Doppler-based secret management and CI/CD secret injection policy |

## 3. Requirement -> Threat -> Control -> Test -> ASVS

| Trace ID | Requirement ID | Threat ID(s) | Control ID(s) | Test ID(s) | ASVS Ref(s) | Coverage State |
|---|---|---|---|---|---|---|
| TM-001 | FR-001 | T-003, T-005, T-004 | C-001, C-004, C-005, C-012 | ST-001, ST-002, ST-114 | V2.2.1, V6.3.8 | Planned |
| TM-002 | FR-002 | T-001, T-002, T-005, T-011, T-019 | C-001, C-002, C-003, C-013, PC-001, PC-002 | ST-003, ST-101, ST-110, ST-115, ST-117 | V6.3.1, V9.1.1, V9.2.1 | Planned |
| TM-003 | FR-003 | T-006, T-009, T-007 | C-006 | ST-103 | V8.2.2, V8.3.1 | Planned |
| TM-004 | FR-004 | T-006, T-007 | C-006 | ST-104 | V8.2.2, V8.2.3 | Planned |
| TM-005 | FR-005 | T-009, T-008, T-007 | C-001, C-006 | ST-102 | V8.2.1, V8.4.2 | Planned |
| TM-006 | FR-006 | T-009, T-013, T-014, T-007 | C-007, C-012, C-014 | ST-004, ST-005, ST-006, ST-126 | V8.2.1, V8.2.2, V2.3.2 | Planned |
| TM-007 | FR-007 | T-016, T-017, T-009, T-018, T-007 | C-008, C-010, C-011, C-012, PC-003 | ST-007, ST-105, ST-106, ST-108, ST-109, ST-116 | V5.1.1, V5.2.1, V5.2.2, V5.3.2 | Planned |
| TM-008 | FR-008 | T-005, T-009, T-008, T-013, T-014, T-007 | C-009 | ST-121 | V8.2.2, V8.3.1 | Planned |
| TM-009 | FR-009 | T-001, T-006, T-009, T-013, T-007 | C-009, C-010, C-011 | ST-107, ST-108, ST-111 | V8.2.1, V8.2.2, V5.3.2 | Planned |
| TM-010 | FR-010 | T-010 | C-014, PC-006 | ST-113 | V16.2.1, V16.3.3 | Planned |
| TM-011 | FR-011 | T-004, T-002 | PC-004, PC-005 | ST-118 | V6.3.2, V6.4.1 | Planned |
| TM-012 | NFR-001 | T-001, T-019 | C-001, C-002, PC-002 | ST-101, ST-110, ST-117 | V7.2.2, V9.1.1 | Planned |
| TM-013 | NFR-002 | T-018, T-016 | C-011 | ST-109 | V5.1.1, V5.2.1 | Planned |
| TM-014 | NFR-003 | T-015, T-014 | C-015, C-016 | ST-123 | V2.3.3 | Planned |
| TM-015 | NFR-004 | T-002, T-016, T-017 | C-017 | ST-120 | V15.1.1, V15.1.2 | Planned |
| TM-016 | NFR-005 | T-017, T-013 | C-018 | ST-119 | V13.2.2, V15.2.3 | Planned |
| TM-017 | NFR-006 | T-008, T-009 | C-019 | ST-122 | V14.2.6, V15.3.1 | Planned |
| TM-018 | SR-001 | T-001, T-012 | C-004 | ST-124 | V11.4.2 | Planned |
| TM-019 | SR-002 | T-002, T-001 | C-003, PC-005 | ST-125 | V11.1.1, V13.3.1 | Planned |
| TM-020 | SR-003 | T-001, T-019 | C-003, PC-002 | ST-110, ST-117 | V9.2.1, V7.4.1 | Planned |
| TM-021 | SR-004 | T-005, T-003, T-011 | C-001, C-002 | ST-101 | V8.2.1, V8.3.1 | Planned |
| TM-022 | SR-005 | T-006, T-007, T-009, T-005 | C-006, C-007, C-008, C-009 | ST-102, ST-103, ST-104, ST-105, ST-106, ST-107, ST-121 | V8.2.1, V8.2.2, V8.3.1 | Planned |
| TM-023 | SR-006 | T-017 | C-010 | ST-108 | V5.3.2, V5.3.3 | Planned |
| TM-024 | SR-007 | T-016, T-017 | C-011, PC-003 | ST-111, ST-116 | V5.4.1, V5.4.2 | Planned |
| TM-025 | SR-008 | T-005, T-006 | C-012 | ST-001, ST-112 | V2.2.1, V2.2.2 | Planned |
| TM-026 | SR-009 | T-008, T-009 | C-013 | ST-112 | V16.5.1, V16.5.4 | Planned |
| TM-027 | SR-010 | T-010 | C-014, PC-006 | ST-113 | V16.2.1, V16.3.3, V16.4.2 | Planned |
| TM-028 | SR-011 | T-004, T-002 | C-005 | ST-114 | V8.2.3, V15.3.3 | Planned |
| TM-029 | SR-012 | T-006, T-009, T-008 | C-015 | ST-121 | V8.2.2, V8.3.3 | Planned |
| TM-030 | SR-013 | T-003, T-008, T-015, T-014 | C-015 | ST-002, ST-126 | V2.3.2, V2.3.3 | Planned |
| TM-031 | SR-014 | T-005, T-006 | C-020 | ST-127 | V5.3.4 | Planned |
| TM-032 | SR-015 | T-011 | PC-001 | ST-115 | V2.1.7 | Implemented in Sprint 1 |
| TM-033 | SR-016 | T-001, T-019 | PC-002 | ST-117 | V7.4.1 | Implemented in Sprint 1 |
| TM-034 | SR-017 | T-016 | PC-003 | ST-116 | V5.1.1, V5.2.1 | Planned |
| TM-035 | SR-018 | T-004, T-002 | PC-004 | ST-118 | V6.3.2, V6.4.1 | Planned |
| TM-036 | NFR-007 | T-009, T-013 | PC-007 | ST-128 | V15.1.2, V15.2.1 | Implemented in Sprint 1 |
| TM-037 | NFR-008 | T-009 | PC-008 | ST-129 | V15.1.1 | Implemented in Sprint 1 |
| TM-038 | NFR-009 | T-013 | PC-009 | ST-130 | V13.1.1 | Implemented in Sprint 1 |
| TM-039 | SR-019 | T-009, T-013 | PC-010 | ST-131 | V15.2.1 | Implemented in Sprint 1 |
| TM-040 | SR-020 | T-001, T-002, T-009 | PC-011 | ST-132 | V13.3.1, V13.3.2 | Implemented in Sprint 1 |
