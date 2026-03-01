import dspy
from config import MODEL_STATE_FILE, TRUNCATE_TEXT
from utils import parse_json_output, date_compare, soft_compare, load_data

# Setup the LM (Must match the one used during optimization)
lm = dspy.LM("ollama/gpt-oss:20b", api_base="http://localhost:11434")
# cache is set to false to prevent using the DSPy cache (force to call LLM for predictions)
dspy.settings.configure(lm=lm, cache=False)

# Define the contract extraction structure
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
        """
    )


# Reconstruct and load the optimized program
optimized_extractor = dspy.ChainOfThought(ContractExtractor)
optimized_extractor.load(MODEL_STATE_FILE)


# Main Evaluation Loop
if __name__ == "__main__":
    contracts, ground_truth = load_data()
    results = []
    fields = ["parties", "agreement_date", "effective_date", "expiration_date",
              "renewal_term", "notice_period", "governing_law"]

    print(f"Starting evaluation on {len(contracts)} contracts...\n")

    for item in contracts:
        file_name = item['filename']
        if file_name not in ground_truth: continue

        # Run Extraction
        prediction = optimized_extractor(contract_text=item['text'][:TRUNCATE_TEXT])

        pred_data = parse_json_output(prediction.answer)
        truth_data = ground_truth[file_name]

        # Score each field
        scores = {}
        for field in fields:
            p_val = pred_data.get(field, "N/A")
            t_val = str(truth_data.get(field, "N/A"))

            if "date" in field:
                scores[field] = date_compare(p_val, t_val)
            else:
                scores[field] = soft_compare(p_val, t_val)

        avg_score = sum(scores.values()) / len(fields)
        results.append({"filename": file_name, "score": avg_score, "details": scores})
        print(f"Processed {file_name}.txt | Accuracy: {avg_score:.1%}")

    # Summary Report
    print("\n" + "=" * 40)
    print("FINAL SUMMARY REPORT (DSPy)")
    print("=" * 40)

    overall_avg = sum(r['score'] for r in results) / len(results)
    print(f"Overall Accuracy: {overall_avg:.2%}")

    print("\nAccuracy by Field:")
    for field in fields:
        field_avg = sum(r['details'][field] for r in results) / len(results)
        print(f"- {field:18}: {field_avg:.2%}")
    print("=" * 40)