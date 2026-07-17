import json
import os
import sys
from pathlib import Path


BLOCKING_RISK_CODES = {"3", "4"}
RISK_NAMES = {
    "0": "Informational",
    "1": "Low",
    "2": "Medium",
    "3": "High",
    "4": "Critical",
}


def iter_alerts(payload):
    for site in payload.get("site", []):
        for alert in site.get("alerts", []):
            yield alert

    for alert in payload.get("alerts", []):
        yield alert


def risk_code(alert):
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


def alert_name(alert):
    return alert.get("alert") or alert.get("name") or "Unnamed alert"


def count_alert_instances(alert):
    instances = alert.get("instances")
    if isinstance(instances, list):
        return max(1, len(instances))
    return 1


def write_step_summary(lines):
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if not summary_path:
        return
    with open(summary_path, "a", encoding="utf-8") as handle:
        handle.write("\n".join(lines))
        handle.write("\n")


def main():
    if len(sys.argv) != 2:
        print("Usage: zap-gate.py <zap-json-report>", file=sys.stderr)
        return 2

    report_path = Path(sys.argv[1])
    if not report_path.exists():
        print(f"ZAP JSON report not found: {report_path}", file=sys.stderr)
        return 2

    payload = json.loads(report_path.read_text(encoding="utf-8"))
    alerts = list(iter_alerts(payload))

    counts = {name: 0 for name in RISK_NAMES.values()}
    blocking = []

    for alert in alerts:
        code = risk_code(alert)
        risk = RISK_NAMES.get(code, "Unknown")
        counts[risk] = counts.get(risk, 0) + count_alert_instances(alert)
        if code in BLOCKING_RISK_CODES:
            blocking.append(alert)

    lines = [
        "## DAST Summary",
        "",
        "| Risk | Count |",
        "| --- | ---: |",
        f"| Critical | {counts.get('Critical', 0)} |",
        f"| High | {counts.get('High', 0)} |",
        f"| Medium | {counts.get('Medium', 0)} |",
        f"| Low | {counts.get('Low', 0)} |",
        f"| Informational | {counts.get('Informational', 0)} |",
        "",
    ]

    if blocking:
        lines.append("Blocking ZAP findings:")
        lines.append("")
        for alert in blocking[:20]:
            lines.append(f"- {RISK_NAMES.get(risk_code(alert), 'Unknown')}: {alert_name(alert)}")
        lines.append("")

    write_step_summary(lines)

    if blocking:
        print(f"ZAP found {len(blocking)} High/Critical alert type(s).")
        return 1

    print("ZAP found no High/Critical alert types.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
