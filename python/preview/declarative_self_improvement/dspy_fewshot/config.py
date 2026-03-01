# File Configuration
import os
from dotenv import load_dotenv
load_dotenv()

GROUND_TRUTH_FILE = '../data/ground_truth_syn.csv'
CONTRACTS_DIR = '../data/contracts_syn'
MODEL_STATE_FILE = "optimized_contract_extractor.json"

# LLM for synthesize data
USE_LOCAL_OLLAMA = False
OLLAMA_ENDPOINT = "http://localhost:11434/api/generate"
SYN_OLLAMA_MODEL = "llama3"  # Change to your local model name
SYN_MODEL = "openai/gpt-4-turbo" # 'openai/gpt-4.1-mini' # or gpt-4o' - Set the API key in the environment variable: export OPENAI_API_KEY="sk-..." or use .env
SYN_OPENAI_MODEL="gpt-4o"
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
NUM_OF_CONTRACTS=60

# LLM and Prompt template for prediction
LLM_MODEL = "ollama/gpt-oss:20b"
TRUNCATE_TEXT=5000
PROMPT_TEMPLATE = """You are a legal contract analyzer. Extract the following information from the contract text below.

Contract Text:
{contract_text}

Please extract the following data from the contract text:
- Parties: The parties involved in the contract with their nickname, e.g. Company A ("Seller"); COMPANY B Inc. ("Company B")
- Agreement Date: The date when the agreement was made (M/D/YY)
- Effective Date: The date when the contract becomes effective (M/D/YY)
- Expiration Date: The date when the contract expires (M/D/YY or perpetual) 
- Renewal Term: Information about contract renewal terms (M/D/YY or number of years or perpetual or successive), e.g. 'successive 1 year', '2 years', 'perpetual', '11/15/14'
- Notice Period To Terminate Renewal: Notice period required to terminate renewal (number of days or months), e.g. '90 days', '6 months'
- Governing Law: The governing law or jurisdiction, i.e., contract/agreement is governed by a state/country ('state' when in the USA or 'state, country' when not in the USA or N/A if unknown), e.g. 'New York', 'Ontario, Canada'

Return your answer as a JSON object with keys exactly matching the clause names above. If a clause is not found or not applicable, use "N/A" as the value.

Example format:
{{
    "parties": "Company A ("Seller"); COMPANY B Inc. ("Company B")",
    "agreement_date": "1/15/20",
    "effective_date": "3/22/20",
    "expiration_date": "6/30/20",,
    "renewal_term": "successive 1 year",
    "notice_period": "90 days",
    "governing_law": "New York"
}}

Now extract the clauses from the contract above:"""