import base64
import json
import os
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen


def read_properties(path):
    data = {}
    for raw_line in Path(path).read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        data[key.strip()] = value.strip()
    return data


def basic_auth_header(token):
    payload = f"{token}:".encode("utf-8")
    return "Basic " + base64.b64encode(payload).decode("utf-8")


def sonar_get(base_url, token, endpoint, params):
    url = base_url.rstrip("/") + endpoint + "?" + urlencode(params)
    request = Request(url)
    request.add_header("Authorization", basic_auth_header(token))
    request.add_header("Accept", "application/json")
    with urlopen(request, timeout=20) as response:
        return json.load(response)


def rating_to_letter(value):
    try:
        rating = int(float(value))
    except (TypeError, ValueError):
        return "n/a"
    return {1: "A", 2: "B", 3: "C", 4: "D", 5: "E"}.get(rating, "n/a")


def format_percent(value):
    try:
        return f"{float(value):.2f}%"
    except (TypeError, ValueError):
        return "n/a"


def format_number(value):
    try:
        number = float(value)
    except (TypeError, ValueError):
        return "n/a"
    if number.is_integer():
        return str(int(number))
    return f"{number:.2f}"


def resolve_pr_or_branch():
    event_name = os.environ.get("GITHUB_EVENT_NAME", "")
    if event_name.startswith("pull_request"):
        event_path = os.environ.get("GITHUB_EVENT_PATH")
        if event_path and Path(event_path).exists():
            payload = json.loads(Path(event_path).read_text(encoding="utf-8"))
            pr = payload.get("pull_request")
            if pr and pr.get("number"):
                return {"pullRequest": str(pr["number"])}, "pullRequest"
    branch = os.environ.get("GITHUB_REF_NAME")
    if branch:
        return {"branch": branch}, "branch"
    return {}, None


def write_summary(text):
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if not summary_path:
        print("GITHUB_STEP_SUMMARY not set; skipping summary output.")
        return
    with open(summary_path, "a", encoding="utf-8") as handle:
        handle.write(text)


def main():
    token = os.environ.get("SONAR_TOKEN")
    if not token:
        write_summary("## SonarCloud Summary\n\nSONAR_TOKEN not set; skipping summary.\n")
        return

    base_url = os.environ.get("SONAR_HOST_URL", "https://sonarcloud.io")
    props_path = Path("sonar-project.properties")
    if not props_path.exists():
        write_summary("## SonarCloud Summary\n\nsonar-project.properties not found; skipping summary.\n")
        return

    props = read_properties(props_path)
    project_key = os.environ.get("SONAR_PROJECT_KEY", props.get("sonar.projectKey"))
    organization = props.get("sonar.organization")
    if not project_key:
        write_summary("## SonarCloud Summary\n\nsonar.projectKey not found; skipping summary.\n")
        return

    context_params, context_label = resolve_pr_or_branch()

    summary_lines = ["## SonarCloud Summary\n\n"]
    overview_url = f"{base_url.rstrip('/')}/project/overview?id={project_key}"
    if context_label == "pullRequest":
        overview_url += f"&pullRequest={context_params['pullRequest']}"
    elif context_label == "branch":
        overview_url += f"&branch={context_params['branch']}"
    summary_lines.append(f"Dashboard: {overview_url}\n\n")

    try:
        quality = sonar_get(
            base_url,
            token,
            "/api/qualitygates/project_status",
            {"projectKey": project_key, **context_params},
        )
        status = quality.get("projectStatus", {}).get("status", "n/a")
        summary_lines.append(f"Quality Gate: {status}\n\n")
    except (HTTPError, URLError, json.JSONDecodeError) as exc:
        summary_lines.append(f"Quality Gate: n/a ({exc.__class__.__name__})\n\n")

    metrics = [
        "bugs",
        "vulnerabilities",
        "security_hotspots",
        "security_hotspots_reviewed",
        "security_rating",
        "reliability_rating",
        "sqale_rating",
        "code_smells",
        "coverage",
        "duplicated_lines_density",
        "ncloc",
    ]

    measures = {}
    try:
        measures_data = sonar_get(
            base_url,
            token,
            "/api/measures/component",
            {
                "component": project_key,
                "metricKeys": ",".join(metrics),
                **context_params,
            },
        )
        for metric in measures_data.get("component", {}).get("measures", []):
            measures[metric.get("metric")] = metric.get("value")
    except (HTTPError, URLError, json.JSONDecodeError):
        measures = {}

    def metric_value(name):
        value = measures.get(name)
        if value is None:
            return "n/a"
        if name in ("coverage", "duplicated_lines_density", "security_hotspots_reviewed"):
            return format_percent(value)
        if name in ("security_rating", "reliability_rating", "sqale_rating"):
            return rating_to_letter(value)
        return format_number(value)

    summary_lines.append("| Metric | Value |\n")
    summary_lines.append("| --- | --- |\n")
    summary_lines.append(f"| Bugs | {metric_value('bugs')} |\n")
    summary_lines.append(f"| Vulnerabilities | {metric_value('vulnerabilities')} |\n")
    summary_lines.append(f"| Security Hotspots | {metric_value('security_hotspots')} |\n")
    summary_lines.append(f"| Hotspots Reviewed | {metric_value('security_hotspots_reviewed')} |\n")
    summary_lines.append(f"| Security Rating | {metric_value('security_rating')} |\n")
    summary_lines.append(f"| Reliability Rating | {metric_value('reliability_rating')} |\n")
    summary_lines.append(f"| Maintainability Rating | {metric_value('sqale_rating')} |\n")
    summary_lines.append(f"| Code Smells | {metric_value('code_smells')} |\n")
    summary_lines.append(f"| Coverage | {metric_value('coverage')} |\n")
    summary_lines.append(f"| Duplicated Lines | {metric_value('duplicated_lines_density')} |\n")
    summary_lines.append(f"| Lines of Code | {metric_value('ncloc')} |\n")

    if organization:
        summary_lines.append("\n")

    write_summary("".join(summary_lines))


if __name__ == "__main__":
    main()
