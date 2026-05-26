"""
JMH JSON 결과를 SVG 차트로 변환.

입력: build/results/jmh/results.json
출력: docs/benchmarks/jmh-results.svg
"""

import json
from pathlib import Path

import matplotlib.pyplot as plt


RESULTS_JSON = Path("build/results/jmh/results.json")
OUTPUT_SVG = Path("docs/benchmarks/jmh-results.svg")

SCENARIO_LABELS = {
    "jdbc_batch_insert": "JDBC Batch (BIGINT + LAST_INSERT_ID)",
    "jpa_identity_insert": "JPA IDENTITY (single INSERT)",
    "jpa_uuidv7_batch_insert": "JPA Batch (UUIDv7)",
}

SCENARIO_COLORS = {
    "jdbc_batch_insert": "#1f77b4",      # blue
    "jpa_identity_insert": "#d62728",    # red
    "jpa_uuidv7_batch_insert": "#2ca02c",  # green
}


def short_name(full_benchmark: str) -> str:
    """e.g. 'com.ryuqq...JdbcVsJpaBenchmark.jdbc_batch_insert' -> 'jdbc_batch_insert'"""
    return full_benchmark.rsplit(".", 1)[-1]


def parse_results(data: list[dict]) -> dict[str, list[tuple[int, float, float]]]:
    """benchmark name -> [(rowCount, score, scoreError), ...]"""
    scenarios: dict[str, list[tuple[int, float, float]]] = {}
    for entry in data:
        name = short_name(entry["benchmark"])
        row_count = int(entry["params"]["rowCount"])
        score = entry["primaryMetric"]["score"]
        error = entry["primaryMetric"]["scoreError"]
        scenarios.setdefault(name, []).append((row_count, score, error))
    for k in scenarios:
        scenarios[k].sort(key=lambda x: x[0])
    return scenarios


def render(scenarios: dict[str, list[tuple[int, float, float]]]) -> None:
    fig, ax = plt.subplots(figsize=(10, 6))

    for name, points in scenarios.items():
        x = [p[0] for p in points]
        y = [p[1] for p in points]
        yerr = [p[2] for p in points]
        ax.errorbar(
            x, y, yerr=yerr,
            marker="o",
            label=SCENARIO_LABELS.get(name, name),
            color=SCENARIO_COLORS.get(name, "black"),
            capsize=5,
            linewidth=2,
            markersize=8,
        )

    ax.set_xscale("log")
    ax.set_yscale("log")
    ax.set_xlabel("Rows inserted (log scale)", fontsize=12)
    ax.set_ylabel("Average time per op (ms, log scale)", fontsize=12)
    ax.set_title(
        "Insert Performance by PK Strategy (JMH 5 iterations, avg +/- stddev)",
        fontsize=14,
        fontweight="bold",
    )
    ax.legend(loc="upper left", fontsize=10)
    ax.grid(True, which="both", linestyle="--", alpha=0.4)

    OUTPUT_SVG.parent.mkdir(parents=True, exist_ok=True)
    fig.savefig(OUTPUT_SVG, format="svg", bbox_inches="tight")
    print(f"Chart written: {OUTPUT_SVG}")


def main() -> None:
    if not RESULTS_JSON.exists():
        raise SystemExit(
            f"JMH results not found at {RESULTS_JSON}. "
            "Run `./gradlew jmh` first."
        )
    with RESULTS_JSON.open() as f:
        data = json.load(f)
    scenarios = parse_results(data)
    if not scenarios:
        raise SystemExit("No benchmark results parsed.")
    render(scenarios)


if __name__ == "__main__":
    main()
