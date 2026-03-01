import ollama
from config import LLM_MODEL, PROMPT_TEMPLATE, TRUNCATE_TEXT
from utils import parse_json_output, date_compare, soft_compare, \
    load_data

# Clean up model name for ollama library (remove "ollama/" prefix if present)
MODEL_NAME = LLM_MODEL.replace("ollama/", "")
if ":" not in MODEL_NAME:
    MODEL_NAME = f"{MODEL_NAME}:20b" # Assuming 20b based on context if not specified


def get_llm_prediction(text):
    prompt = PROMPT_TEMPLATE.replace("{contract_text}", text[:4000])
    try:
        response = ollama.chat(model=MODEL_NAME, messages=[
            {'role': 'user', 'content': prompt}
        ], options={"temperature": 0.0}) # Set temperature to 0 for deterministic results
        return response['message']['content']
    except Exception as e:
        print(f"Error calling Ollama: {e}")
        return "{}"


# Main Evaluation Loop
if __name__ == "__main__":
    contracts, ground_truth = load_data()
    results = []
    fields = ["parties", "agreement_date", "effective_date", "expiration_date",
              "renewal_term", "notice_period", "governing_law"]

    print(f"Starting evaluation on {len(contracts)} contracts using direct LLM call ({MODEL_NAME})...\n")

    for item in contracts:
        file_name = item['filename']
        if file_name not in ground_truth: continue

        # 1. Run LLM Prediction
        raw_output = get_llm_prediction(item['text'][:TRUNCATE_TEXT])
        pred_data = parse_json_output(raw_output)
        truth_data = ground_truth[file_name]

        # 2. Score each field
        scores = {}
        for field in fields:
            # Note: config_llm.py prompt now uses field names that match ground truth keys
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
    if results:
        print("\n" + "=" * 40)
        print("FINAL SUMMARY REPORT (LLM)")
        print("=" * 40)

        overall_avg = sum(r['score'] for r in results) / len(results)
        print(f"Overall Accuracy: {overall_avg:.2%}")

        print("\nAccuracy by Field:")
        for field in fields:
            field_avg = sum(r['details'][field] for r in results) / len(results)
            print(f"- {field:18}: {field_avg:.2%}")
        print("=" * 40)
    else:
        print("No results to report.")
