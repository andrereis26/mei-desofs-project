import json
import os
import sys
import urllib.error
import urllib.request


def parse_float(value, default):
    try:
        return float(value)
    except (TypeError, ValueError):
        return default


def parse_headers(raw_value):
    if not raw_value:
        return []
    return [value.strip() for value in raw_value.split(",") if value.strip()]


def build_url(base_url, path):
    if path.startswith("http://") or path.startswith("https://"):
        return path
    if not base_url:
        return path
    if base_url.endswith("/"):
        base_url = base_url[:-1]
    if not path.startswith("/"):
        path = f"/{path}"
    return f"{base_url}{path}"


def request(method, url, timeout_seconds, headers=None, body=None):
    request_headers = headers or {}
    data = None
    if body is not None:
        data = json.dumps(body).encode("utf-8")
        request_headers = {**request_headers, "Content-Type": "application/json"}
    req = urllib.request.Request(url, method=method, headers=request_headers, data=data)
    try:
        with urllib.request.urlopen(req, timeout=timeout_seconds) as response:
            return response.getcode(), dict(response.headers)
    except urllib.error.HTTPError as exc:
        return exc.code, dict(exc.headers)
    except Exception as exc:
        raise RuntimeError(f"Request failed for {url}: {exc}") from exc


def safe_request(method, url, timeout_seconds, headers=None, body=None):
    try:
        status, response_headers = request(
            method,
            url,
            timeout_seconds,
            headers=headers,
            body=body,
        )
        return status, response_headers, None
    except RuntimeError as exc:
        return None, {}, str(exc)


def append_summary(lines):
    summary_path = os.getenv("GITHUB_STEP_SUMMARY")
    if not summary_path:
        return
    with open(summary_path, "a", encoding="utf-8") as handle:
        handle.write("\n".join(lines))
        handle.write("\n")


def normalize_header_map(headers):
    return {key.lower(): value for key, value in headers.items()}


def main():
    base_url = os.getenv("SECURITY_BASE_URL")
    if not base_url:
        print("SECURITY_BASE_URL is required.")
        sys.exit(1)

    protected_path = os.getenv("SECURITY_PROTECTED_PATH", "/api/users")
    login_path = os.getenv("SECURITY_LOGIN_PATH", "/api/auth/login")
    timeout_seconds = parse_float(os.getenv("SECURITY_TIMEOUT_SECONDS"), 10.0)
    required_headers = parse_headers(os.getenv("SECURITY_REQUIRED_HEADERS"))

    protected_url = build_url(base_url, protected_path)
    login_url = build_url(base_url, login_path)

    failures = []
    warnings = []

    if base_url.startswith("http://"):
        warnings.append("Base URL uses HTTP; HTTPS is recommended for production.")

    status, protected_headers, error = safe_request("GET", protected_url, timeout_seconds)
    if error:
        failures.append(error)
    elif status not in (401, 403):
        failures.append(f"Unauthenticated access to {protected_path} returned {status}.")

    status, protected_auth_headers, error = safe_request(
        "GET",
        protected_url,
        timeout_seconds,
        headers={"Authorization": "Bearer invalid"},
    )
    if error:
        failures.append(error)
    elif status not in (401, 403):
        failures.append(f"Invalid token access to {protected_path} returned {status}.")

    status, login_headers, error = safe_request(
        "POST",
        login_url,
        timeout_seconds,
        body={"username": "invalid", "password": "invalid"},
    )
    if error:
        failures.append(error)
    elif status == 200:
        failures.append("Login with invalid credentials unexpectedly succeeded.")
    elif status not in (400, 401, 403):
        failures.append(f"Login failure returned unexpected status {status}.")

    if required_headers:
        header_map = normalize_header_map({**protected_headers, **login_headers})
        missing = [name for name in required_headers if name.lower() not in header_map]
        if missing:
            failures.append(f"Missing security headers: {', '.join(missing)}")

    summary_lines = [
        "## Post-deploy security checks",
        "",
        f"- Protected path: {protected_path}",
        f"- Login path: {login_path}",
    ]
    if warnings:
        summary_lines.append(f"- Warnings: {', '.join(warnings)}")
    if failures:
        summary_lines.append(f"- Result: failed ({len(failures)} issues)")
    else:
        summary_lines.append("- Result: passed")

    append_summary(summary_lines)

    if warnings:
        print("Security check warnings:")
        for warning in warnings:
            print(f"- {warning}")

    if failures:
        print("Security checks failed:")
        for failure in failures:
            print(f"- {failure}")
        sys.exit(1)

    print("Security checks passed.")


if __name__ == "__main__":
    main()
