"""
Helper Functions (Comparison Logic)
"""
import json
import os
from difflib import SequenceMatcher

import pandas as pd
from dateutil import parser as date_parser
from config import GROUND_TRUTH_FILE, CONTRACTS_DIR


# Data Loading and Preparation
def load_data():
    # Load Ground Truth
    df = pd.read_csv(GROUND_TRUTH_FILE, delimiter='^', encoding='utf-8', header="infer")
    df = df.dropna(subset=['filename'])
    df['filename'] = df['filename'].astype(str).str.strip()
    duplicates = df[df.duplicated('filename', keep=False)]
    if not duplicates.empty:
        print("\n[DEBUG] Found duplicates:")
        print(duplicates.sort_values('filename')[['filename', 'document_name', 'parties']])
    # Convert empty values to "N/A" (prevent float nan)
    df = df.fillna("N/A")
    truth_dict = df.set_index('filename').to_dict('index')

    # Load Contracts
    contracts = []
    for f in sorted(os.listdir(CONTRACTS_DIR)):
        if f.endswith(".txt"):
            with open(os.path.join(CONTRACTS_DIR, f), 'r', encoding='utf-8') as file:
                contracts.append({"filename": f.replace(".txt", ""), "text": file.read()})
    return contracts, truth_dict

def soft_compare(pred: str, truth: str, threshold: float = 0.7) -> float:
    """
    Soft comparison: returns similarity score 0.0-1.0

    Rules:
    - Exact match (case-insensitive) = 1.0
    - N/A match = 1.0 if both N/A, else 0.0
    - Substring: if truth is substring of pred = 0.9
    - Similarity: use SequenceMatcher for word-level similarity
    """
    pred_norm = str(pred).lower().strip()
    truth_norm = str(truth).lower().strip()

    # Exact match
    if pred_norm == truth_norm:
        return 1.0

    # N/A handling
    if truth_norm == "n/a":
        return 1.0 if pred_norm == "n/a" else 0.0
    if pred_norm == "n/a":
        return 0.0

    # Substring match (truth in pred)
    if truth_norm in pred_norm:
        return 0.9

    # Word overlap
    truth_words = set(truth_norm.split())
    pred_words = set(pred_norm.split())

    if len(truth_words) > 0 and len(pred_words) > 0:
        overlap = len(truth_words & pred_words) / max(len(truth_words), len(pred_words))
        if overlap >= 0.5:
            return 0.5 + (overlap * 0.5)  # 0.5 to 1.0

    # Sequence similarity as fallback
    ratio = SequenceMatcher(None, pred_norm, truth_norm).ratio()
    return ratio


def date_compare(pred: str, truth: str, info="---") -> float:
    pred_norm = str(pred).lower().strip()
    truth_norm = str(truth).lower().strip()

    print(f"DEBUG: date_compare for {info} (pred vs truth): {pred_norm} vs {truth_norm}  --> ", end="")
    if truth_norm == "n/a":
        result = 1.0 if pred_norm == "n/a" else 0.0
        print(f"{result:.1%}")
        return result
    if pred_norm == "n/a":
        print(f"0.0")
        return 0.0

    # fuzzy=True allows it to ignore extra text like "the 5th of..."
    if pred_norm == truth_norm:
        print(f"DEBUG: date_compare exact match: 100.0%")
        return 1.0
        
    try:
        d1 = date_parser.parse(pred_norm, fuzzy=True)
        d2 = date_parser.parse(truth_norm, fuzzy=True)
        result = 1.0 if d1 == d2 else 0.0
        print(f"DEBUG: date_compare result (pred vs truth): {d1} vs {d2} --> {result:.1%}")
        return result
    except:
        # Fallback to string overlap if parsing fails
        result = 0.5 if truth_norm in pred_norm or pred_norm in truth_norm else 0.0
        print(f"ERROR (date_parser): string overlap result (pred vs truth): {truth_norm} in {pred_norm} --> {result:.1%}")
        return result


def parse_json_output(output_str: str) -> dict:
    """Parse JSON string from LLM output."""
    import json
    try:
        # Try direct JSON parse
        return json.loads(output_str)
    except:
        # Try to extract JSON from text
        try:
            start = output_str.find('{')
            end = output_str.rfind('}') + 1
            if start >= 0 and end > start:
                return json.loads(output_str[start:end])
        except:
            pass

        # Return default if parsing fails
        return {
            "parties": "N/A",
            "agreement_date": "N/A",
            "effective_date": "N/A",
            "expiration_date": "N/A",
            "renewal_term": "N/A",
            "notice_period": "N/A",
            "governing_law": "N/A",
        }



def soft_compare_OLD(pred, truth):
    pred_norm, truth_norm = str(pred).lower().strip(), str(truth).lower().strip()
    if pred_norm == truth_norm or truth_norm in pred_norm: return 1.0
    if truth_norm == "n/a" or pred_norm == "n/a": return 0.0
    return SequenceMatcher(None, pred_norm, truth_norm).ratio()


def date_compare_OLD(pred, truth):
    p, t = str(pred).strip(), str(truth).strip()
    if p.lower() == t.lower(): return 1.0
    # Add your date parsing logic here if strict date matching is needed
    return soft_compare(p, t)


def parse_json_output_OLD(output_str):
    try:
        start = output_str.find('{')
        end = output_str.rfind('}') + 1
        if start != -1 and end != -1:
            return json.loads(output_str[start:end])
        return {}
    except:
        return {}