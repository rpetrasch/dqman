#!/usr/bin/env python3
"""
Compares structured fact extraction from a rental contract with and without a
semantic layer, using a small local model (Ollama).

Why structured/repeated extraction, not a single Q&A?
    gpt-oss:20b is a capable model: for a single, greedily-decoded (temperature=0)
    natural-language question, it usually reads this short contract correctly
    either way. But real extraction pipelines need STRUCTURED, SCHEMA-CONFORMANT,
    machine-consumable output, run repeatedly (many documents, retries, non-zero
    temperature). That is exactly where a semantic layer earns its keep: it fixes
    field names, types, synonym resolution, and disambiguation rules so output is
    consistent and correct run after run -- not just plausible-sounding once.

Usage:
    python3 extract.py

Requires a running Ollama instance (default http://localhost:11434) with the
model below pulled, e.g.:
    ollama pull gpt-oss:20b
"""

import json
import re
import urllib.request

OLLAMA_URL = "http://localhost:11434/api/generate"
MODEL = "gpt-oss:20b"
RUNS_PER_CASE = 5
TEMPERATURE = 0.7

with open("contract.txt", encoding="utf-8") as f:
    CONTRACT = f.read()

with open("semantic_layer.json", encoding="utf-8") as f:
    SEMANTIC_LAYER = f.read()

BASELINE_SYSTEM = (
    "You are a contract fact-extraction assistant. Read the contract and extract "
    "the requested facts into JSON. Output ONLY valid JSON, no explanation, no "
    "markdown code fences."
)

SEMANTIC_SYSTEM = (
    "You are a contract fact-extraction assistant. You are given a SEMANTIC LAYER "
    "that defines canonical field names, synonyms, formulas for derived fields, and "
    "disambiguation rules for this contract domain, followed by the CONTRACT text. "
    "Use the semantic layer to resolve synonyms/foreign terms to canonical fields, "
    "compute derived fields, and apply disambiguation rules before answering. "
    "Output ONLY valid JSON that strictly conforms to the requested schema -- exact "
    "key names, plain numbers (no currency symbols or strings) for numeric fields. "
    "No explanation, no markdown code fences."
)


def call_ollama(system: str, user: str, seed: int) -> str:
    payload = {
        "model": MODEL,
        "prompt": f"{system}\n\n{user}",
        "stream": False,
        "options": {"temperature": TEMPERATURE, "seed": seed},
    }
    req = urllib.request.Request(
        OLLAMA_URL,
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(req, timeout=300) as resp:
        body = json.loads(resp.read().decode("utf-8"))
    return body["response"].strip()


def parse_json(text: str):
    text = re.sub(r"^```(json)?|```$", "", text.strip(), flags=re.MULTILINE).strip()
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        return None


# ---------------------------------------------------------------------------
# Test Case 1: derived total + synonym-resolved deposit
# ---------------------------------------------------------------------------

CASE1 = {
    "name": "Test 1: Derived total payment + synonym-resolved deposit (structured JSON)",
    "baseline_task": (
        "Extract into JSON: the tenant's total monthly payment amount, and the "
        "security deposit amount. Use whatever JSON keys you think are appropriate."
    ),
    "semantic_task": (
        "Extract into JSON with EXACTLY these two keys: total_monthly_payment, "
        "security_deposit. Both must be plain numbers (the dollar amount, no symbols)."
    ),
    "check": lambda d: (
        d is not None
        and isinstance(d, dict)
        and any(_is_number_close(v, 1625) for v in _flat_values(d))
        and any(_is_number_close(v, 2700) for v in _flat_values(d))
        and set(d.keys()) == {"total_monthly_payment", "security_deposit"}
    ),
    "schema_check": lambda d: (
        d is not None and set(d.keys()) == {"total_monthly_payment", "security_deposit"}
    ),
}

# ---------------------------------------------------------------------------
# Test Case 2: notice-period disambiguation (standard vs. breach clause)
# ---------------------------------------------------------------------------

CASE2 = {
    "name": "Test 2: Notice-period disambiguation (standard vs. breach clause)",
    "baseline_task": (
        "Extract into JSON: the notice period the tenant must give to end the "
        "lease at the end of its term. Use whatever JSON key you think is appropriate."
    ),
    "semantic_task": (
        "Extract into JSON with EXACTLY this one key: termination_notice_period. "
        "The value must be a string describing only the standard end-of-term notice "
        "period (not the breach/default notice period)."
    ),
    "check": lambda d: (
        d is not None
        and isinstance(d, dict)
        and any(_mentions_three_months(v) and not _mentions_fourteen_days_only(v) for v in _flat_values(d))
        and set(d.keys()) == {"termination_notice_period"}
    ),
    "schema_check": lambda d: (
        d is not None and set(d.keys()) == {"termination_notice_period"}
    ),
}


def _flat_values(d):
    for v in d.values():
        if isinstance(v, dict):
            yield from _flat_values(v)
        else:
            yield v


def _is_number_close(v, target, tol=0.01):
    try:
        return abs(float(v) - target) < tol
    except (TypeError, ValueError):
        return False


def _mentions_three_months(v):
    s = str(v).lower()
    return "3" in s or "three" in s


def _mentions_fourteen_days_only(v):
    s = str(v).lower()
    return ("14" in s or "fourteen" in s) and not ("3" in s or "three" in s)


def run_case(case, semantic: bool):
    system = SEMANTIC_SYSTEM if semantic else BASELINE_SYSTEM
    task = case["semantic_task"] if semantic else case["baseline_task"]
    if semantic:
        user = f"SEMANTIC LAYER:\n{SEMANTIC_LAYER}\n\nCONTRACT:\n{CONTRACT}\n\nTASK: {task}"
    else:
        user = f"CONTRACT:\n{CONTRACT}\n\nTASK: {task}"

    results = []
    for seed in range(RUNS_PER_CASE):
        raw = call_ollama(system, user, seed)
        parsed = parse_json(raw)
        results.append(
            {
                "raw": raw,
                "parsed": parsed,
                "schema_ok": bool(case["schema_check"](parsed)),
                "correct": bool(case["check"](parsed)),
            }
        )
    return results


def summarize(label, results):
    n = len(results)
    schema_ok = sum(r["schema_ok"] for r in results)
    correct = sum(r["correct"] for r in results)
    print(f"  {label}: schema-conformant {schema_ok}/{n}, correct {correct}/{n}")
    for i, r in enumerate(results):
        flag = "OK " if r["correct"] else "FAIL"
        print(f"    run {i} [{flag}]: {r['raw']!r}")


def main():
    for case in (CASE1, CASE2):
        print("=" * 100)
        print(case["name"])
        print("-" * 100)

        print("[WITHOUT semantic layer]")
        baseline_results = run_case(case, semantic=False)
        summarize("baseline", baseline_results)
        print()

        print("[WITH semantic layer]")
        semantic_results = run_case(case, semantic=True)
        summarize("semantic layer", semantic_results)
        print()


if __name__ == "__main__":
    main()
