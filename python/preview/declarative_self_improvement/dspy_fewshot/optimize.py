"""
DSPy WITH PYDANTIC (DSPy 3.1.2)
================================================

Uses:
1. Pydantic for structured output + validation
2. ChainOfThought for reasoning
3. Soft comparison (similarity, containment)
4. BootstrapFewShot for optimization
"""
import os
import csv
from datetime import datetime
import dspy
from dspy.teleprompt import BootstrapFewShot
from pydantic import BaseModel, Field, field_validator
from config import TRUNCATE_TEXT, CONTRACTS_DIR, GROUND_TRUTH_FILE
from utils import parse_json_output, date_compare, soft_compare


# Pydantic Models (Output structure + validation)
class ContractClauses(BaseModel):
    """Extracted contract clauses with validation."""

    parties: str = Field(
        description="Names of parties (e.g., 'Company A and Company B' or 'N/A')"
    )
    agreement_date: str = Field(
        description="Date signed in format M/D/YY (or 'N/A')"
    )
    effective_date: str = Field(
        description="Effective date in format M/D/YY (or 'N/A')"
    )
    expiration_date: str = Field(
        description="Expiration date in format M/D/YY or 'perpetual' (or 'N/A')"
    )
    renewal_term: str = Field(
        description="Renewal terms like '1 year', 'successive 1 year', 'perpetual' (or 'N/A')"
    )
    notice_period: str = Field(
        description="Notice to terminate like '30 days', '90 days', '12 months' (or 'N/A')"
    )
    governing_law: str = Field(
        description="Jurisdiction like 'California', 'New York', 'Nevada' (or 'N/A')"
    )

    @field_validator('parties', 'agreement_date', 'effective_date', 'expiration_date',
                     'renewal_term', 'notice_period', 'governing_law')
    @classmethod
    def not_empty(cls, v):
        """Ensure field is not empty (but can be 'N/A')."""
        if not v or len(str(v).strip()) == 0:
            raise ValueError("Field cannot be empty, use 'N/A' if not found")
        return v.strip()

    @field_validator('agreement_date', 'effective_date', 'expiration_date')
    @classmethod
    def validate_date_format(cls, v):
        if v == "N/A" or v.lower() == "perpetual" or "successive" in v.lower():
            return v

        # Try to catch DD/MM/YY or DD.MM.YYYY and flip them
        for fmt in ["%d.%m.%Y", "%d/%m/%y", "%d/%m/%Y"]:
            try:
                dt = datetime.strptime(v, fmt)
                # If day > 12, it's definitely European; flip it to M/D/YY
                return dt.strftime("%-m/%-d/%y")
            except ValueError:
                continue
        return v


# DSPy Signature with JSON output (instead of Pydantic)
class ContractExtractor_minimal(dspy.Signature):
    """ Extract key clauses from a contract.

    STRICT DATE RULE: All dates MUST be in US format (Month/Day/Year).
    Example: '23.12.2017' or '23 December 2017' MUST become '12/23/17' (M/D/YY).
    Do NOT use Day/Month/Year format.

    Return 'N/A' for missing fields.
    """
    contract_text = dspy.InputField(desc="The full contract text")
    answer = dspy.OutputField(
        desc="JSON with fields: parties, agreement_date, effective_date, expiration_date, renewal_term, notice_period, governing_law")


class ContractExtractor(dspy.Signature):
    """You are a legal contract analyzer. Extract key clauses from the contract text.
    
    STRICT DATE RULE: All output dates MUST be in US format (Month/Day/Year). 
    If you see Day.Month.Year (e.g. 13.01.2019), convert it to 01/13/19.
    """

    contract_text = dspy.InputField(desc="The full contract text")
    answer = dspy.OutputField(
        desc="""JSON object with the following fields:
        - parties: Names of parties (e.g. "Company A" and "Company B").
        - agreement_date: Date agreement was made (M/D/YY).
        - effective_date: Date contract becomes effective (M/D/YY).
        - expiration_date: Date contract expires (M/D/YY) or "perpetual".
        - renewal_term: Renewal terms (e.g. "3 years", "successive 1 year", "perpetual", "2 years").
        - notice_period: Notice period required to terminate renewal (number of days or months) (e.g. "90 days", "6 months").
        - governing_law: Jurisdiction (e.g. "New York", "Ontario, Canada").

        Use "N/A" if a field is not found.
        """)


# Load Data
def load_contracts(contracts_dir=CONTRACTS_DIR) -> list:
    """Load all .txt contract files."""
    contracts = []
    for file_name in sorted(os.listdir(contracts_dir)):
        if file_name.endswith(".txt"):
            path = os.path.join(contracts_dir, file_name)
            try:
                with open(path, "r", encoding="utf-8") as f:
                    text = f.read()
                clean_name = file_name.replace(".txt", "").strip()
                contracts.append({"filename": clean_name, "text": text})
            except Exception as e:
                print(f"Warning: Could not load {file_name}: {e}")
    return contracts


def load_ground_truth(csv_file=GROUND_TRUTH_FILE) -> dict:
    """Load ground truth from CSV."""
    truth = {}
    try:
        with open(csv_file, "r", encoding="utf-8") as f:
            reader = csv.DictReader(f, delimiter="^")
            for row in reader:
                # Normalize file_name: remove .txt and clean spaces
                file_name = row.get("filename", "").strip()
                file_name = file_name.replace(".txt", "")  # Remove .txt extension
                file_name = " ".join(file_name.split())    # Normalize spaces

                def sanitize(val):
                    # Check if val is None, an empty string, or the literal "None"
                    if val is None or str(val).strip().lower() in ["", "none", "nan", "null"]:
                        return "N/A"
                    return str(val).strip()

                truth[file_name] = {
                    "parties": sanitize(row.get("parties", "N/A")),
                    "agreement_date": sanitize(row.get("agreement_date", "N/A")),
                    "effective_date": sanitize(row.get("effective_date", "N/A")),
                    "expiration_date": sanitize(row.get("expiration_date", "N/A")),
                    "renewal_term": sanitize(row.get("renewal_term", "N/A")),
                    "notice_period": sanitize(row.get("notice_period", "N/A")),
                    "governing_law": sanitize(row.get("governing_law", "N/A")),
                }
    except Exception as e:
        print(f"Warning: Could not load ground truth: {e}")
    return truth


# Create Training Examples
def create_examples(contracts: list, ground_truth: dict, limit=50) -> list:
    """Convert contracts + ground truth into DSPy Examples."""
    import json
    examples = []
    for contract in contracts[:limit]:
        file_name = contract["filename"]
        text = contract["text"][:TRUNCATE_TEXT]  # Limit text length

        if file_name not in ground_truth:
            print(f"Warning: No ground truth for {file_name}")
            continue

        truth = ground_truth[file_name]

        # Format answer as JSON string
        answer_json = json.dumps({
            "parties": truth["parties"],
            "agreement_date": truth["agreement_date"],
            "effective_date": truth["effective_date"],
            "expiration_date": truth["expiration_date"],
            "renewal_term": truth["renewal_term"],
            "notice_period": truth["notice_period"],
            "governing_law": truth["governing_law"],
        })

        example = dspy.Example(
            contract_text=text,
            # Adding a rationale helps the BootstrapFewShot understand the transformation
            rationale="I will extract the clause values from the text. For dates, I will identify the format used in the document and convert it to standard US format (M/D/YY).",
            answer=answer_json
        ).with_inputs("contract_text")

        examples.append(example)

    return examples


# Evaluation Metric
def evaluate_extraction(example, pred, trace=None) -> float:
    """
    Evaluate using SOFT comparison.
    Returns: 0.0 to 1.0 (average similarity across all fields)
    """
    if pred is None or not hasattr(pred, 'answer'):
        return 0.0

    # Parse ground truth
    # If example.answer is already a Pydantic object (ContractClauses), use .model_dump()
    if hasattr(example.answer, 'model_dump'):
        truth_json = example.answer.model_dump()
    else:
        truth_json = parse_json_output(example.answer)

    # Parse prediction
    # If pred.answer is a Pydantic object, use .model_dump(), else parse
    if hasattr(pred.answer, 'model_dump'):
        pred_json = pred.answer.model_dump()
    else:
        pred_json = parse_json_output(pred.answer)

    fields = ["parties", "agreement_date", "effective_date", "expiration_date",
              "renewal_term", "notice_period", "governing_law"]

    total_score = 0.0
    for field in fields:
        pred_val = pred_json.get(field, "N/A")
        truth_val = truth_json.get(field, "N/A")

        # Use date comparison for date fields
        if field in ["agreement_date", "effective_date", "expiration_date"]:
            score = date_compare(str(pred_val), str(truth_val))
        else:
            score = soft_compare(str(pred_val), str(truth_val))

        total_score += score

    return total_score / len(fields)


# Optimize
def optimize_extraction(examples: list) -> dspy.Module:
    """Optimize using BootstrapFewShot."""

    # Create base ChainOfThought module
    extractor = dspy.ChainOfThought(ContractExtractor)  # Reasoning, but no man. validation
    # TypedPredictor is deprecated in DSPy 3.x
    # extractor = dspy.TypedPredictor(ContractExtractor)  # No reasoning, but auto validation
    # extractor = dspy.TypedPredictor(dspy.ChainOfThought(ContractExtractor))  # Both

    optimizer = BootstrapFewShot(
        metric=evaluate_extraction,
        max_bootstrapped_demos=4,
        max_labeled_demos=4
    )

    print("\n" + "="*80)
    print("Optimizing: Finding best few-shot examples...")
    print("="*80)

    optimized = optimizer.compile(extractor, trainset=examples)

    return optimized


# Evaluate on Test Set
def evaluate_on_test_set(optimized_model, contracts: list, ground_truth: dict,
                        train_count=15) -> dict:
    """Evaluate on remaining contracts."""

    results = {
        "total": 0,
        "scores_by_field": {f: [] for f in ["parties", "agreement_date", "effective_date",
                                              "expiration_date", "renewal_term",
                                              "notice_period", "governing_law"]},
        "predictions": []
    }

    test_contracts = contracts[train_count:]

    print("\n" + "="*80)
    print(f"Evaluating: Testing on {len(test_contracts)} contracts...")
    print("="*80 + "\n")

    for contract in test_contracts:
        file_name = contract["filename"]
        text = contract["text"][:4000]

        if file_name not in ground_truth:
            continue

        truth = ground_truth[file_name]

        try:
            pred = optimized_model(contract_text=text)
        except Exception as e:
            print(f"ERROR on {file_name}: {e}")
            continue

        if not hasattr(pred, 'answer'):
            print(f"ERROR on {file_name}: No answer field")
            continue

        results["total"] += 1

        # Parse predictions
        if hasattr(pred.answer, 'model_dump'):
            pred_json = pred.answer.model_dump()
        else:
            pred_json = parse_json_output(pred.answer)

        fields = ["parties", "agreement_date", "effective_date", "expiration_date",
                  "renewal_term", "notice_period", "governing_law"]

        scores = {}
        for field in fields:
            pred_val = pred_json.get(field, "N/A")
            truth_val = truth.get(field, "N/A")

            if field in ["agreement_date", "effective_date", "expiration_date"]:
                score = date_compare(str(pred_val), str(truth_val), info=f"{file_name} - {field}")
            else:
                score = soft_compare(str(pred_val), str(truth_val))

            scores[field] = score
            results["scores_by_field"][field].append(score)

        avg_score = sum(scores.values()) / len(scores)
        results["predictions"].append({
            "filename": file_name,
            "score": avg_score,
            "scores": scores
        })

        print(f"{file_name}: {avg_score:.1%}")

    return results


# Print Results
def print_results(results: dict):
    """Print evaluation results."""
    print("\n" + "="*80)
    print("RESULTS")
    print("="*80)

    if results["total"] == 0:
        print("No test contracts evaluated")
        return

    # Overall accuracy
    all_scores = []
    for scores in results["scores_by_field"].values():
        all_scores.extend(scores)

    overall_accuracy = sum(all_scores) / len(all_scores) if all_scores else 0

    print(f"\nOverall Accuracy: {overall_accuracy:.1%}")
    print(f"Test Contracts: {results['total']}")

    print(f"\nPer-Field Average Score:")
    print("-" * 80)

    for field, scores in results["scores_by_field"].items():
        avg = sum(scores) / len(scores) if scores else 0
        print(f"  {field:30s}: {avg:.1%}")

    print("\n" + "="*80)


# Main
def main():
    print("DSPy + Pydantic Contract Extraction (DSPy 3.1.2)")
    print("="*80)

    # Setup LM
    lm = dspy.LM("ollama/gpt-oss:20b", api_base="http://localhost:11434")
    dspy.settings.configure(lm=lm, cache=False)

    # Load data
    print("\n[1/4] Loading contracts and ground truth...")
    contracts = load_contracts(CONTRACTS_DIR)
    ground_truth = load_ground_truth(GROUND_TRUTH_FILE)
    print(f"  Loaded {len(contracts)} contracts")
    print(f"  Loaded {len(ground_truth)} ground truth entries")
    if len(contracts) == 0:
        print("ERROR: No contracts found! Check path:", os.path.abspath(CONTRACTS_DIR))
        return

    # Create examples
    print("\n[2/4] Creating training examples...")
    examples = create_examples(contracts, ground_truth)
    print(f"  Created {len(examples)} examples (from {len(contracts)} loaded)")
    if len(examples) == 0:
        print("ERROR: No examples created! Check if filenames match between contracts and ground truth.")
        return

    # Sanity Check
    print("\n[2.5/4] Sanity Check: Running model on one example...")
    import random
    sanity_ex = random.choice(examples)
    print(f"  Test File: {sanity_ex.contract_text[:50]}...")
    
    # Create a fresh extractor for the sanity check
    sanity_extractor = dspy.ChainOfThought(ContractExtractor)
    try:
        pred = sanity_extractor(contract_text=sanity_ex.contract_text)
        print(f"  Raw Answer: {pred.answer[:100]}...")
        score = evaluate_extraction(sanity_ex, pred)
        print(f"  Score: {score:.1%}")
        
        if score == 0.0:
            print("  WARNING: Score is 0.0. The optimizer might fail to find any good examples.")
            print(f"  Prediction JSON: {parse_json_output(pred.answer)}")
            
    except Exception as e:
        print(f"  FATAL ERROR during sanity check: {e}")
        return

    # Optimize
    print("\n[3/4] Optimizing...")
    optimized_model = optimize_extraction(examples)

    # Save the optimized settings to a file
    optimized_model.save("optimized_contract_extractor.json")
    print("Optimized model saved to optimized_contract_extractor.json")

    # Evaluate
    print("\n[4/4] Evaluating on test set...")
    results = evaluate_on_test_set(optimized_model, contracts, ground_truth, train_count=25)

    # Print
    print_results(results)


if __name__ == "__main__":
    main()