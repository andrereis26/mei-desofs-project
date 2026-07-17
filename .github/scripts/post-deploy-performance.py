import math
import os
import re
import sys
import time
import urllib.error
import urllib.request
from collections import Counter


def parse_float(value, default):
    try:
        return float(value)
    except (TypeError, ValueError):
        return default


def parse_int(value, default):
    try:
        return int(value)
    except (TypeError, ValueError):
        return default


def parse_urls(raw_value):
    if not raw_value:
        return []
    parts = re.split(r"[,\s]+", raw_value.strip())
    return [value for value in parts if value]


def parse_status_ranges(raw_value):
    if not raw_value:
        return [(200, 299)]

    ranges = []
    for part in re.split(r"[,\s]+", raw_value.strip()):
        if not part:
            continue
        if "-" in part:
            start_text, end_text = part.split("-", 1)
            start = int(start_text)
            end = int(end_text)
        else:
            start = int(part)
            end = start
        if start < 100 or end > 599 or start > end:
            raise ValueError(f"invalid HTTP status range: {part}")
        ranges.append((start, end))

    return ranges or [(200, 299)]


def status_is_success(status, status_ranges):
    return any(start <= status <= end for start, end in status_ranges)


def percentile(values, pct):
    if not values:
        return None
    ordered = sorted(values)
    index = int(math.ceil((pct / 100.0) * len(ordered))) - 1
    index = max(index, 0)
    return ordered[index]


def request_once(url, timeout_seconds):
    request = urllib.request.Request(url, headers={"User-Agent": "desofs-post-deploy-perf"})
    start = time.perf_counter()
    try:
        with urllib.request.urlopen(request, timeout=timeout_seconds) as response:
            status = response.getcode()
            response.read(256)
        elapsed_ms = (time.perf_counter() - start) * 1000.0
        return status, elapsed_ms, None
    except urllib.error.HTTPError as exc:
        elapsed_ms = (time.perf_counter() - start) * 1000.0
        return exc.code, elapsed_ms, str(exc)
    except Exception as exc:
        return None, None, str(exc)


def append_summary(lines):
    summary_path = os.getenv("GITHUB_STEP_SUMMARY")
    if not summary_path:
        return
    with open(summary_path, "a", encoding="utf-8") as handle:
        handle.write("\n".join(lines))
        handle.write("\n")


def main():
    urls = parse_urls(os.getenv("PERF_TARGET_URLS"))
    if not urls:
        print("PERF_TARGET_URLS is required and must contain at least one URL.")
        sys.exit(1)

    try:
        success_status_ranges = parse_status_ranges(os.getenv("PERF_SUCCESS_STATUSES"))
    except ValueError as exc:
        print(f"PERF_SUCCESS_STATUSES is invalid: {exc}")
        sys.exit(1)

    request_count = parse_int(os.getenv("PERF_REQUESTS"), 20)
    warmup_count = parse_int(os.getenv("PERF_WARMUP"), 1)
    avg_limit_ms = parse_float(os.getenv("PERF_AVG_MS"), 1200.0)
    p95_limit_ms = parse_float(os.getenv("PERF_P95_MS"), 2000.0)
    timeout_seconds = parse_float(os.getenv("PERF_TIMEOUT_SECONDS"), 12.0)
    pause_ms = parse_float(os.getenv("PERF_PAUSE_MS"), 50.0)

    if request_count <= 0:
        print("PERF_REQUESTS must be greater than zero.")
        sys.exit(1)
    if timeout_seconds <= 0:
        print("PERF_TIMEOUT_SECONDS must be greater than zero.")
        sys.exit(1)

    failure_counts = Counter()
    summary_lines = [
        "## Post-deploy performance checks",
        "",
        "| URL | Attempts | Success | Failed | Avg (ms) | P95 (ms) | Max (ms) | Status |",
        "| --- | --- | --- | --- | --- | --- | --- | --- |",
    ]

    for url in urls:
        for _ in range(max(warmup_count, 0)):
            request_once(url, timeout_seconds)

        durations = []
        request_failures = 0

        for _ in range(request_count):
            status, elapsed_ms, error = request_once(url, timeout_seconds)
            if status is None:
                request_failures += 1
                failure_counts[f"{url}: request failed ({error})"] += 1
            elif not status_is_success(status, success_status_ranges):
                request_failures += 1
                failure_counts[f"{url}: unexpected status {status}"] += 1
            else:
                durations.append(elapsed_ms)

            if pause_ms > 0:
                time.sleep(pause_ms / 1000.0)

        if not durations:
            summary_lines.append(
                f"| {url} | {request_count} | 0 | {request_failures} | n/a | n/a | n/a | failed |"
            )
            continue

        avg_ms = sum(durations) / len(durations)
        p95_ms = percentile(durations, 95)
        max_ms = max(durations)

        status_text = "passed"
        if request_failures:
            status_text = "failed"
        if avg_ms > avg_limit_ms:
            failure_counts[
                f"{url}: average latency {avg_ms:.1f}ms exceeds {avg_limit_ms:.1f}ms"
            ] += 1
            status_text = "failed"
        if p95_ms is not None and p95_ms > p95_limit_ms:
            failure_counts[
                f"{url}: p95 latency {p95_ms:.1f}ms exceeds {p95_limit_ms:.1f}ms"
            ] += 1
            status_text = "failed"

        summary_lines.append(
            f"| {url} | {request_count} | {len(durations)} | {request_failures} | {avg_ms:.1f} | {p95_ms:.1f} | {max_ms:.1f} | {status_text} |"
        )

    append_summary(summary_lines)

    if failure_counts:
        print("Performance checks failed:")
        for failure, count in failure_counts.most_common():
            suffix = f" (x{count})" if count > 1 else ""
            print(f"- {failure}{suffix}")
        sys.exit(1)

    print("Performance checks passed.")


if __name__ == "__main__":
    main()
