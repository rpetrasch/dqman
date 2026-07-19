# Fact Extraction With vs. Without a Semantic Layer

A minimal, runnable demo comparing structured fact extraction from a rental
contract, with and without a **semantic layer** in the prompt.

## What's here

| File | Purpose |
|---|---|
| `contract.txt` | Synthetic residential lease agreement. Deliberately contains: a rent figure split across two clauses (Base Rent + Additional Costs) with no single "total" stated anywhere; a security deposit called "Bond" and expressed as "2x Base Rent" rather than a dollar figure; and two different notice periods (a 3-month standard one and a 14-day breach one) that a naive reader can conflate. |
| `semantic_layer.json` | The semantic layer: canonical field names, synonyms/foreign-term aliases (e.g. `Bond` → `security_deposit`, `Kaltmiete` → `base_rent`), plain-language explanations, formulas for derived fields (e.g. `total_monthly_payment = base_rent + additional_costs`), and disambiguation rules (e.g. "prefer the standard notice clause over the breach clause unless asked otherwise"). |
| `extract.py` | Runs both approaches against a local Ollama model and scores the results. |

## Why the test isn't "does the model get the right number"

With a single, greedily-decoded question, `gpt-oss:20b` is smart enough to
read this short contract correctly either way — it doesn't need a semantic
layer to notice that Base Rent + Additional Costs should be summed. That's
not a useful comparison; it would just show that a capable model is capable.

The comparison that actually matters for a real extraction pipeline is
**structured, repeated extraction**: many documents, retries, non-zero
temperature, and a downstream system that expects a fixed schema. That's
exactly where a semantic layer earns its keep — it doesn't just help the
model find the right number once, it makes the *shape* of the output
consistent and machine-consumable every time.

## The two test cases

**Test 1 — derived total + synonym-resolved deposit.**
Ask for `total_monthly_payment` (must be *computed* from two separate
clauses) and `security_deposit` (the contract never uses that phrase — it
says "Bond" and expresses it as "2× Base Rent", not a dollar amount) as JSON,
5 times at temperature 0.7.

**Test 2 — notice-period disambiguation.**
Ask for the tenant's lease-termination notice period as JSON, 5 times at
temperature 0.7. The contract states *two* notice periods (3 months standard,
14 days for breach); the correct answer requires picking the right one.

Each run is scored on:
- **schema-conformant** — does the JSON use exactly the expected key(s)?
- **correct** — is the value numerically/factually right?

## Typical result

```
Test 1: Derived total payment + synonym-resolved deposit (structured JSON)
[WITHOUT semantic layer]  schema-conformant 1/5, correct 1/5
    -> keys drift: totalMonthlyPayment / monthlyPayment / total_monthly_payment
    -> values sometimes strings ("$1,625.00") instead of numbers
[WITH semantic layer]     schema-conformant 5/5, correct 5/5

Test 2: Notice-period disambiguation (standard vs. breach clause)
[WITHOUT semantic layer]  schema-conformant 0/5, correct 0/5
    -> key never matches the requested "termination_notice_period"
       (noticePeriodMonths, tenantTerminationNoticePeriod, ...)
[WITH semantic layer]     schema-conformant 5/5, correct 5/5
```

Without the semantic layer, the underlying *value* is often right, but the
key names, nesting, and value types vary from run to run — which is exactly
what breaks a downstream parser, database write, or API contract in a real
pipeline. The semantic layer fixes the schema, resolves synonyms/jargon, and
encodes the disambiguation rule the contract itself doesn't spell out.

## Running it

```bash
# one-time setup
ollama pull gpt-oss:20b

cd semantic-layer-demo
python3 extract.py
```

No external Python packages are required (uses `urllib` from the standard
library). Ollama must be running locally on `http://localhost:11434`.

## Extending it

- Swap in a weaker/smaller model (e.g. `llama3.1:8b`) — the gap should widen
  further, since smaller models are more prone to inventing keys/ignoring
  formulas without explicit guidance.
- Add more fields to `semantic_layer.json` (e.g. late-fee interest rate,
  CPI adjustment timing) and extend `extract.py` with more test cases.
- Try `contract.txt` variants using different jargon/synonyms to see how far
  synonym resolution generalizes.
