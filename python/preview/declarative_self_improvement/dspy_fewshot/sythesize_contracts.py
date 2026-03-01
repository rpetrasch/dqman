from dateutil import parser as date_parser
import pandas as pd
import os
import random
import requests
from openai import OpenAI
from config import CONTRACTS_DIR, GROUND_TRUTH_FILE, OPENAI_API_KEY, SYN_OPENAI_MODEL, NUM_OF_CONTRACTS, \
    OLLAMA_ENDPOINT, SYN_OLLAMA_MODEL, USE_LOCAL_OLLAMA

# --- Configuration ---

# Create output directory
if not os.path.exists(CONTRACTS_DIR):
    os.makedirs(CONTRACTS_DIR)

# Date format variations to inject into the prompt
DATE_FORMATS = [
    "MM/DD/YYYY (e.g., 12/23/2004)",
    "Month DD, YYYY (e.g., December 23, 2004)",
    "DD.MM.YYYY (e.g., 23.12.2004)",
    "the [Day] day of [Month], [Year] (e.g., the 23rd day of December, 2004)",
    "Abbreviated Month (e.g., Dec 23, '04)"
]


def generate_prompt(row):
    """Creates a unique, randomized prompt for a specific contract row."""

    # 1. Select a random date format for this specific LLM call
    chosen_format = random.choice(DATE_FORMATS)

    # 2. Build instructions based on presence/absence of data
    content_instructions = []

    # Mapping CSV columns to prompt logic
    mapping = {
        'agreement_date': 'Agreement Date',
        'effective_date': 'Effective Date',
        'expiration_date': 'Expiration Date',
        'renewal_term': 'Renewal Term',
        'notice_period': 'Notice Period',
        'governing_law': 'Governing Law'
    }

    data_summary = ""
    for col, label in mapping.items():
        val = row[col]
        if pd.isna(val) or str(val).strip().lower() in ['nan', 'none', '']:
            content_instructions.append(f"- Do NOT mention any {label} or duration details related to it.")
        else:
            data_summary += f"- {label}: {val}\n"
            if label == 'Renewal Term':
                content_instructions.append(
                    f"- Describe the Renewal Term ('{val}') using natural legal language. Do NOT use the exact phrase from the data.")

    # 3. Construct the final prompt
    prompt = f"""
    TASK: Write a formal legal contract based on the metadata provided below.

    METADATA:
    - Document Name: {row['document_name']}
    - Parties involved: {row['parties']}
    {data_summary}
    - Non-Compete: {'Include a non-compete clause' if row['non_compete'] == 'Yes' else 'Do NOT include a non-compete clause'}
    - Exclusivity: {'Include an exclusivity clause' if row['exclusivity'] == 'Yes' else 'Do NOT include an exclusivity clause'}

    STYLISTIC CONSTRAINTS FOR THIS SPECIFIC DOCUMENT:
    1. FORMAT ALL DATES in this style: {chosen_format}.
    {chr(10).join(content_instructions)}
    2. Strictly use the dates given (agreement date, effective date, expiration_date) even if they contradict each other (e.g., effective date is before agreement date).
    3. The size limit of the document is 4000 characters (including spaces).
    4. Use professional, varied legalese. 
    5. Ensure the document looks like a real, standalone text file.
    6. Return ONLY the contract text. No introductions or chatter.
    """
    return prompt


def call_llm(prompt):
    """Sends the prompt to the local Ollama API."""
    payload = {
        "model": SYN_OLLAMA_MODEL,
        "prompt": prompt,
        "stream": False
    }
    try:
        response = requests.post(OLLAMA_ENDPOINT, json=payload, timeout=60)
        return response.json().get('response', '')
    except Exception as e:
        return f"Error calling LLM: {e}"

client = OpenAI(api_key=OPENAI_API_KEY)
def call_openai(prompt):
    response = client.responses.create(
        model=SYN_OPENAI_MODEL,
        instructions="You are a lawyer specialized on creating legal contracts.",
        input=prompt
    )
    return response.output_text


# Main function for contract generation
if __name__ == "__main__":
    print("Starting generation...")
    df = pd.read_csv(GROUND_TRUTH_FILE, delimiter='^', encoding='utf-8')
    for index, row in df.head(NUM_OF_CONTRACTS).iterrows():
        # parse the dates for correct formatting
        if not row['exclusivity']:
            print(f"Failed to get exclusivity date for contract {row['filename']}. Skipping...")
            exit(1)
        try:
            ad = date_parser.parse(row['agreement_date'], fuzzy=True)
        except ValueError:
            print(f"Failed to parse agreement date for contract {row['filename']}. Skipping...")
            exit(1)
        except TypeError:
            row['agreement_date'] = 'N/A'
        try:
            ed = date_parser.parse(row['effective_date'], fuzzy=True)
        except ValueError:
            print(f"Failed to parse eff. date for contract {row['filename']}. Skipping...")
            exit(1)
        except TypeError:
            row['effective_date'] = 'N/A'
        try:
            if row['expiration_date'] == "perpetual" or "successive" in row['expiration_date']:
                pass
            else:
                ed = date_parser.parse(row['expiration_date'], fuzzy=True)
        except ValueError:
            print(f"Failed to parse exp. date for contract {row['filename']}. Skipping...")
            exit(1)
        except TypeError:
            row['expiration_date'] = 'N/A'
        print(f"Contract {row['filename']} parsed successfully.")
        if index == 370:
            break

    print(f"Starting generation for {len(df)} contracts...")

    for index, row in df.head(NUM_OF_CONTRACTS).iterrows():
        filename = f"{row['filename']}.txt"
        filepath = os.path.join(CONTRACTS_DIR, filename)

        # Generate unique prompt
        prompt = generate_prompt(row)

        # Get response from local LLM
        print(f"Generating {filename}...")
        if USE_LOCAL_OLLAMA:
            contract_text = call_llm(prompt)  # local ollama LLM
        else:
            contract_text = call_openai(prompt) # OpenAI LLM

        # Save to file
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(contract_text)

    print("Done! All contracts generated in the 'contracts_syn' folder.")
