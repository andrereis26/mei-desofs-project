import os
from pathlib import Path
import xml.etree.ElementTree as ET


def find_first(root_dir, pattern):
    root = Path(root_dir)
    if not root.exists():
        return None
    for path in root.rglob(pattern):
        return path
    return None


def parse_int(value):
    try:
        return int(value)
    except (TypeError, ValueError):
        return 0


def parse_float(value):
    try:
        return float(value)
    except (TypeError, ValueError):
        return None


def detect_namespace(root):
    if root.tag.startswith("{"):
        return root.tag.split("}")[0] + "}"
    return ""


def extract_cvss(vulnerability, ns):
    paths = [
        f"{ns}cvssV4/{ns}baseScore",
        f"{ns}cvssV3/{ns}baseScore",
        f"{ns}cvssV2/{ns}score",
        f"{ns}cvssV2/{ns}baseScore",
    ]
    for path in paths:
        value = vulnerability.findtext(path)
        score = parse_float(value)
        if score is not None:
            return score
    return None


def normalize_cwe(value):
    if not value:
        return None
    text = value.strip().upper()
    if not text:
        return None
    if text.startswith("CWE-"):
        return text
    digits = "".join(ch for ch in text if ch.isdigit())
    if digits:
        return f"CWE-{digits}"
    return None


def extract_cwes(vulnerability, ns):
    results = []
    for cwe in vulnerability.findall(f"{ns}cwes/{ns}cwe"):
        value = normalize_cwe(cwe.text or "")
        if value:
            results.append(value)
    raw_value = vulnerability.findtext(f"{ns}cwe")
    if raw_value:
        for part in raw_value.split(","):
            value = normalize_cwe(part)
            if value:
                results.append(value)
    return results


def parse_dependency_check(report_path):
    if report_path is None or not report_path.exists():
        return None
    try:
        tree = ET.parse(report_path)
    except ET.ParseError:
        return None
    root = tree.getroot()
    ns = detect_namespace(root)
    deps_with_vulns = 0
    vuln_total = 0
    max_cvss = None
    severity_counts = {
        "CRITICAL": 0,
        "HIGH": 0,
        "MEDIUM": 0,
        "LOW": 0,
        "UNKNOWN": 0,
    }
    cwe_counts = {}

    for dependency in root.findall(f".//{ns}dependency"):
        vulns = dependency.findall(f"{ns}vulnerabilities/{ns}vulnerability")
        if not vulns:
            continue
        deps_with_vulns += 1
        for vulnerability in vulns:
            vuln_total += 1
            severity = (vulnerability.findtext(f"{ns}severity") or "").strip().upper()
            if severity not in severity_counts:
                severity = "UNKNOWN"
            severity_counts[severity] += 1
            score = extract_cvss(vulnerability, ns)
            if score is not None:
                max_cvss = score if max_cvss is None else max(max_cvss, score)
            for cwe in extract_cwes(vulnerability, ns):
                cwe_counts[cwe] = cwe_counts.get(cwe, 0) + 1

    return {
        "deps_with_vulns": deps_with_vulns,
        "vuln_total": vuln_total,
        "max_cvss": max_cvss,
        "severity_counts": severity_counts,
        "cwe_counts": cwe_counts,
    }


def parse_spotbugs(report_path):
    if report_path is None or not report_path.exists():
        return None
    try:
        tree = ET.parse(report_path)
    except ET.ParseError:
        return None
    root = tree.getroot()
    ns = detect_namespace(root)
    bugs = root.findall(f".//{ns}BugInstance")
    counts = {"HIGH": 0, "MEDIUM": 0, "LOW": 0, "UNKNOWN": 0}
    categories = {}

    for bug in bugs:
        priority = (bug.attrib.get("priority") or "").strip()
        if priority == "1":
            level = "HIGH"
        elif priority == "2":
            level = "MEDIUM"
        elif priority == "3":
            level = "LOW"
        else:
            level = "UNKNOWN"
        counts[level] += 1
        category = (bug.attrib.get("category") or "").strip()
        if category:
            categories[category] = categories.get(category, 0) + 1

    top_categories = sorted(categories.items(), key=lambda item: item[1], reverse=True)[:3]

    return {
        "total": len(bugs),
        "counts": counts,
        "top_categories": top_categories,
    }


def iter_zap_alerts(payload):
    for site in payload.get("site", []):
        for alert in site.get("alerts", []):
            yield alert

    for alert in payload.get("alerts", []):
        yield alert


def zap_risk_code(alert):
    raw_code = alert.get("riskcode") or alert.get("riskCode")
    if raw_code is not None:
        return str(raw_code)

    risk = (alert.get("riskdesc") or alert.get("risk") or "").strip().lower()
    if risk.startswith("critical"):
        return "4"
    if risk.startswith("high"):
        return "3"
    if risk.startswith("medium"):
        return "2"
    if risk.startswith("low"):
        return "1"
    return "0"


def parse_zap(report_path):
    if report_path is None or not report_path.exists():
        return None
    try:
        import json

        payload = json.loads(report_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return None

    counts = {
        "CRITICAL": 0,
        "HIGH": 0,
        "MEDIUM": 0,
        "LOW": 0,
        "INFORMATIONAL": 0,
        "UNKNOWN": 0,
    }

    for alert in iter_zap_alerts(payload):
        code = zap_risk_code(alert)
        if code == "4":
            level = "CRITICAL"
        elif code == "3":
            level = "HIGH"
        elif code == "2":
            level = "MEDIUM"
        elif code == "1":
            level = "LOW"
        elif code == "0":
            level = "INFORMATIONAL"
        else:
            level = "UNKNOWN"

        instances = alert.get("instances")
        count = max(1, len(instances)) if isinstance(instances, list) else 1
        counts[level] += count

    return counts


def format_number(value):
    if value is None:
        return "n/a"
    if isinstance(value, float):
        return f"{value:.2f}"
    return str(value)


def write_summary(text):
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if not summary_path:
        print("GITHUB_STEP_SUMMARY not set; skipping summary output.")
        return
    with open(summary_path, "a", encoding="utf-8") as handle:
        handle.write(text)


def main():
    sca_report = find_first("security-reports/sca", "dependency-check-report*.xml")
    sast_report = find_first("security-reports/sast", "spotbugs*.xml")
    dast_report = find_first("security-reports/dast", "zap-api-scan.json")

    sca = parse_dependency_check(sca_report)
    sast = parse_spotbugs(sast_report)
    dast = parse_zap(dast_report)

    lines = ["## Security Summary\n\n"]

    server_url = os.environ.get("GITHUB_SERVER_URL", "https://github.com")
    repository = os.environ.get("GITHUB_REPOSITORY")
    run_id = os.environ.get("GITHUB_RUN_ID")
    if repository and run_id:
        run_url = f"{server_url}/{repository}/actions/runs/{run_id}"
        lines.append(f"Artifacts: {run_url}\n\n")

    lines.append("### SCA (OWASP Dependency-Check)\n\n")
    if sca is None:
        lines.append("SCA report not found or invalid.\n\n")
    else:
        lines.append("| Metric | Value |\n")
        lines.append("| --- | --- |\n")
        lines.append(f"| Dependencies with vulnerabilities | {sca['deps_with_vulns']} |\n")
        lines.append(f"| Total vulnerabilities | {sca['vuln_total']} |\n")
        lines.append(f"| Critical | {sca['severity_counts']['CRITICAL']} |\n")
        lines.append(f"| High | {sca['severity_counts']['HIGH']} |\n")
        lines.append(f"| Medium | {sca['severity_counts']['MEDIUM']} |\n")
        lines.append(f"| Low | {sca['severity_counts']['LOW']} |\n")
        lines.append(f"| Unknown | {sca['severity_counts']['UNKNOWN']} |\n")
        lines.append(f"| Max CVSS | {format_number(sca['max_cvss'])} |\n\n")

        if sca["cwe_counts"]:
            lines.append("**Top CWE**\n\n")
            lines.append("| CWE | Count |\n")
            lines.append("| --- | --- |\n")
            for cwe, count in sorted(sca["cwe_counts"].items(), key=lambda item: item[1], reverse=True)[:10]:
                lines.append(f"| {cwe} | {count} |\n")
            lines.append("\n")

    lines.append("### SAST (SpotBugs)\n\n")
    if sast is None:
        lines.append("SAST report not found or invalid.\n")
    else:
        lines.append("| Metric | Value |\n")
        lines.append("| --- | --- |\n")
        lines.append(f"| Total bugs | {sast['total']} |\n")
        lines.append(f"| High | {sast['counts']['HIGH']} |\n")
        lines.append(f"| Medium | {sast['counts']['MEDIUM']} |\n")
        lines.append(f"| Low | {sast['counts']['LOW']} |\n")
        lines.append(f"| Unknown | {sast['counts']['UNKNOWN']} |\n")

        if sast["top_categories"]:
            categories = ", ".join(f"{name} ({count})" for name, count in sast["top_categories"])
            lines.append(f"\nTop categories: {categories}\n")

    lines.append("\n### DAST (OWASP ZAP)\n\n")
    if dast is None:
        lines.append("DAST report not found or invalid.\n")
    else:
        lines.append("| Risk | Count |\n")
        lines.append("| --- | --- |\n")
        lines.append(f"| Critical | {dast['CRITICAL']} |\n")
        lines.append(f"| High | {dast['HIGH']} |\n")
        lines.append(f"| Medium | {dast['MEDIUM']} |\n")
        lines.append(f"| Low | {dast['LOW']} |\n")
        lines.append(f"| Informational | {dast['INFORMATIONAL']} |\n")
        lines.append(f"| Unknown | {dast['UNKNOWN']} |\n")

    write_summary("".join(lines))


if __name__ == "__main__":
    main()
