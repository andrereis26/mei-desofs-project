import os
from pathlib import Path
import xml.etree.ElementTree as ET


def parse_int(value):
    try:
        return int(value)
    except (TypeError, ValueError):
        return 0


def parse_float(value):
    try:
        return float(value)
    except (TypeError, ValueError):
        return 0.0


def junit_files(root_dir):
    files = []
    root_path = Path(root_dir)
    if not root_path.exists():
        return files
    for path in root_path.rglob("*.xml"):
        try:
            tree = ET.parse(path)
        except ET.ParseError:
            continue
        tag = tree.getroot().tag
        if tag in ("testsuite", "testsuites"):
            files.append(path)
    return files


def collect_suite_stats(root):
    stats = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0, "time": 0.0}
    if root.tag == "testsuites":
        suites = root.findall("testsuite")
        for suite in suites:
            stats["tests"] += parse_int(suite.attrib.get("tests"))
            stats["failures"] += parse_int(suite.attrib.get("failures"))
            stats["errors"] += parse_int(suite.attrib.get("errors"))
            stats["skipped"] += parse_int(suite.attrib.get("skipped"))
            stats["time"] += parse_float(suite.attrib.get("time"))
        return stats
    if root.tag == "testsuite":
        stats["tests"] += parse_int(root.attrib.get("tests"))
        stats["failures"] += parse_int(root.attrib.get("failures"))
        stats["errors"] += parse_int(root.attrib.get("errors"))
        stats["skipped"] += parse_int(root.attrib.get("skipped"))
        stats["time"] += parse_float(root.attrib.get("time"))
    return stats


def junit_stats(root_dir):
    totals = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0, "time": 0.0}
    for path in junit_files(root_dir):
        try:
            tree = ET.parse(path)
        except ET.ParseError:
            continue
        stats = collect_suite_stats(tree.getroot())
        for key in totals:
            totals[key] += stats[key]
    return totals


def line_coverage_percent(report_path):
    if not Path(report_path).exists():
        return None
    try:
        tree = ET.parse(report_path)
    except ET.ParseError:
        return None
    root = tree.getroot()
    for counter in root.findall("counter"):
        if counter.attrib.get("type") == "LINE":
            missed = parse_int(counter.attrib.get("missed"))
            covered = parse_int(counter.attrib.get("covered"))
            total = missed + covered
            if total == 0:
                return None
            return {
                "missed": missed,
                "covered": covered,
                "total": total,
                "percent": (covered / total) * 100.0,
            }
    return None


def summarize(name, root_dir, coverage_path):
    totals = junit_stats(root_dir)
    tests = totals["tests"]
    failures = totals["failures"]
    errors = totals["errors"]
    skipped = totals["skipped"]
    passed = max(tests - failures - errors - skipped, 0)
    success_rate = (passed / tests) * 100.0 if tests else 0.0
    failure_rate = ((failures + errors) / tests) * 100.0 if tests else 0.0
    skipped_rate = (skipped / tests) * 100.0 if tests else 0.0
    coverage = line_coverage_percent(coverage_path)
    return {
        "name": name,
        "tests": tests,
        "passed": passed,
        "failures": failures,
        "errors": errors,
        "skipped": skipped,
        "success_rate": success_rate,
        "failure_rate": failure_rate,
        "skipped_rate": skipped_rate,
        "time": totals["time"],
        "coverage": coverage,
    }


def summarize_overall(suites):
    totals = {
        "name": "Overall",
        "tests": 0,
        "passed": 0,
        "failures": 0,
        "errors": 0,
        "skipped": 0,
        "time": 0.0,
    }
    coverage_totals = {"missed": 0, "covered": 0, "total": 0}
    has_coverage = False
    for suite in suites:
        totals["tests"] += suite["tests"]
        totals["passed"] += suite["passed"]
        totals["failures"] += suite["failures"]
        totals["errors"] += suite["errors"]
        totals["skipped"] += suite["skipped"]
        totals["time"] += suite["time"]
        if suite["coverage"] is not None:
            coverage_totals["missed"] += suite["coverage"]["missed"]
            coverage_totals["covered"] += suite["coverage"]["covered"]
            coverage_totals["total"] += suite["coverage"]["total"]
            has_coverage = True

    tests = totals["tests"]
    totals["success_rate"] = (totals["passed"] / tests) * 100.0 if tests else 0.0
    totals["failure_rate"] = ((totals["failures"] + totals["errors"]) / tests) * 100.0 if tests else 0.0
    totals["skipped_rate"] = (totals["skipped"] / tests) * 100.0 if tests else 0.0

    if has_coverage and coverage_totals["total"] > 0:
        totals["coverage"] = {
            "missed": coverage_totals["missed"],
            "covered": coverage_totals["covered"],
            "total": coverage_totals["total"],
            "percent": (coverage_totals["covered"] / coverage_totals["total"]) * 100.0,
        }
    else:
        totals["coverage"] = None

    return totals


def write_summary(suites, summary_path):
    unit, integration, overall = suites
    with open(summary_path, "a", encoding="utf-8") as handle:
        handle.write("## Test Summary\n\n")
        handle.write("| Suite | Tests | Passed | Failures | Errors | Skipped | Success % | Failure % | Skipped % | Time (s) |\n")
        handle.write("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |\n")
        for suite in (unit, integration, overall):
            handle.write(
                f"| {suite['name']} | {suite['tests']} | {suite['passed']} | {suite['failures']} | "
                f"{suite['errors']} | {suite['skipped']} | {suite['success_rate']:.2f}% | "
                f"{suite['failure_rate']:.2f}% | {suite['skipped_rate']:.2f}% | {suite['time']:.2f} |\n"
            )

        handle.write("\n## Coverage Summary\n\n")
        handle.write("| Suite | Lines Covered | Lines Missed | Lines Total | Line Coverage % |\n")
        handle.write("| --- | --- | --- | --- | --- |\n")
        for suite in (unit, integration, overall):
            if suite["coverage"] is None:
                coverage_text = "n/a"
                covered_text = "n/a"
                missed_text = "n/a"
                total_text = "n/a"
            else:
                coverage_text = f"{suite['coverage']['percent']:.2f}%"
                covered_text = str(suite["coverage"]["covered"])
                missed_text = str(suite["coverage"]["missed"])
                total_text = str(suite["coverage"]["total"])
            handle.write(
                f"| {suite['name']} | {covered_text} | {missed_text} | {total_text} | {coverage_text} |\n"
            )


def main():
    unit = summarize(
        "Unit",
        "test-reports/unit",
        "test-reports/unit/backend/target/site/jacoco/jacoco-unit.xml",
    )
    integration = summarize(
        "Integration",
        "test-reports/integration",
        "test-reports/integration/backend/target/site/jacoco/jacoco-integration.xml",
    )
    overall = summarize_overall([unit, integration])

    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if not summary_path:
        print("GITHUB_STEP_SUMMARY not set; skipping summary output.")
        return
    write_summary([unit, integration, overall], summary_path)


if __name__ == "__main__":
    main()
