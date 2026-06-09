import ssl
import json
import requests
import certifi
from geopy.geocoders import Nominatim
from geopy.exc import GeocoderServiceError

# --- Configuration & Lookups ---
OLLAMA_URL = "http://localhost:11434/api/generate"
LOCAL_MODEL = "gpt-oss"  # Your local Ollama model identifier

STREET_ABBREVIATIONS = {
    "ave": "Avenue", "pkwy": "Parkway", "st": "Street", "rd": "Road", "blvd": "Boulevard"
}

CITY_CLEANING = {
    "new york city": "New York",
    "newyorkcity": "New York",
    "nyc": "New York"
}


# -------------------------------------------------------------------------
# STEP 1: Data Cleansing Module
# -------------------------------------------------------------------------
def cleanse_data(raw_record: dict) -> dict:
    """Performs local deterministic normalization on State, City, and Street suffixes."""
    clean_record = raw_record.copy()

    # 1. State uppercase normalization
    if clean_record.get("State"):
        clean_record["State"] = clean_record["State"].upper().strip()

    # 2. City entity standardization
    if clean_record.get("City"):
        normalized_city = clean_record["City"].lower().replace(" ", "")
        if normalized_city in CITY_CLEANING:
            clean_record["City"] = CITY_CLEANING[normalized_city]

    # 3. Street abbreviation correction (checks the suffix)
    if clean_record.get("Street"):
        words = clean_record["Street"].split()
        if words:
            suffix = words[-1].lower().strip(".")
            if suffix in STREET_ABBREVIATIONS:
                words[-1] = STREET_ABBREVIATIONS[suffix]
                clean_record["Street"] = " ".join(words)

    return clean_record


# -------------------------------------------------------------------------
# STEP 2: Data Enrichment Level 1 (OpenStreetMap via GeoPy)
# -------------------------------------------------------------------------
def enrich_level_1_osm(record: dict) -> (dict, bool):
    """Attempts authoritative validation via OpenStreetMap."""
    enriched_record = record.copy()

    ssl_context = ssl.create_default_context(cafile=certifi.where())
    geolocator = Nominatim(user_agent="dq_pipeline_processor", ssl_context=ssl_context)

    query = f"{enriched_record['Street']}, {enriched_record['City']}, {enriched_record['State']}"

    try:
        location = geolocator.geocode(query, addressdetails=True, timeout=5)
        if location and 'address' in location.raw:
            address_details = location.raw['address']

            # Reconstruct clean structural fields from API payload
            if 'road' in address_details:
                house_number = address_details.get('house_number', enriched_record["Street"].split()[0])
                enriched_record["Street"] = f"{house_number} {address_details['road']}"
            if 'city' in address_details:
                enriched_record["City"] = address_details['city']

            # Append missing ZIP code
            api_zip = address_details.get('postcode')
            if api_zip:
                enriched_record["Zip"] = api_zip.split('-')[0]
                return enriched_record, True  # Success

    except GeocoderServiceError:
        pass  # Allow pipeline to naturally fall back to Level 2 on timeout or server glitch

    return enriched_record, False  # Failed to resolve or match


# -------------------------------------------------------------------------
# STEP 3: Data Enrichment Level 2 (Local LLM Fallback)
# -------------------------------------------------------------------------
def enrich_level_2_ollama(record: dict) -> dict:
    """Asks local Ollama model to fix typos/variants contextually."""
    # Construct an explicit prompt ensuring standard text output
    prompt = (
        f"You are a Data Quality engineer pipeline component.\n"
        f"Fix spelling typos in this address component string. Output ONLY a clean, valid street address "
        f"with digital ordinals if applicable (e.g., 'Fifth' or 'fifh' becomes '5th').\n"
        f"Input street: '{record['Street']}', City: '{record['City']}', State: '{record['State']}'.\n"
        f"Output format: Just the corrected street string, nothing else. Do not write an explanation."
    )

    payload = {
        "model": LOCAL_MODEL,
        "prompt": prompt,
        "stream": False,
        "options": {"temperature": 0.0}  # Absolute deterministic extraction
    }

    try:
        response = requests.post(OLLAMA_URL, json=payload, timeout=15)
        if response.status_code == 200:
            llm_suggestion = response.json().get("response", "").strip()
            if llm_suggestion and len(llm_suggestion) > 3:
                # Update the record with the LLM's structural guess
                corrected_record = record.copy()
                corrected_record["Street"] = llm_suggestion
                return corrected_record
    except Exception as e:
        print(f"[Level 2 Note]: Local Ollama call was unable to complete: {e}")

    return record


# -------------------------------------------------------------------------
# STEP 4: Execution Pipeline Engine
# -------------------------------------------------------------------------
def run_address_pipeline(raw_record: dict) -> (dict, list):
    logs = []
    current_record = raw_record.copy()

    # Executing Cleansing Phase
    current_record = cleanse_data(current_record)
    logs.append(f"Cleansing completed: State standardized, checking suffix maps.")

    # Executing Level 1 Match
    enriched_record, success = enrich_level_1_osm(current_record)
    if success:
        logs.append("Level 1 (OSM Directory) match successful.")
        return enriched_record, logs

    # Level 1 Failed -> Activating Level 2 Fallback
    logs.append("Level 1 strict lookup failed. Engaging Level 2 Ollama agent context mapping...")
    llm_corrected_record = enrich_level_2_ollama(current_record)
    logs.append(f"Ollama suggested correction: '{llm_corrected_record['Street']}'")

    # Re-running Level 1 with LLM output payload
    final_record, success_lvl2 = enrich_level_1_osm(llm_corrected_record)
    if success_lvl2:
        logs.append("Level 1 retry with Ollama structural mapping input succeeded.")
        return final_record, logs

    # Terminal Failure State
    logs.append("CRITICAL: Pipeline failed to resolve address verification at all tiers. Signalled manual review.")
    final_record["Zip"] = "MANUAL_REVIEW"
    return final_record, logs


# -------------------------------------------------------------------------
# Main Controller Engine Execution
# -------------------------------------------------------------------------
if __name__ == "__main__":

    # Target low-quality record: 350 fifh ave, New YorkCity, Ny  (correct record is: 350 5th Avenue, New York, NY 10118)
    raw_customer_record = {
        "Customer ID": 10421,
        "Name": "Alice Smith",
        "Street": "350 fifh ave",
        "City": "New YorkCity",
        "State": "Ny",
        "Zip": ""
    }

    # Execute complete process pipeline loop
    processed_record, pipeline_history = run_address_pipeline(raw_customer_record)

    # Print Final Verification & Quality Correction Report
    print("=" * 60)
    print("             DATA QUALITY CORRECTION REPORT")
    print("=" * 60)
    print(f"Customer Name:   {processed_record['Name']} (ID: {processed_record['Customer ID']})")
    print("-" * 60)
    print(
        f"INITIAL INPUT:   {raw_customer_record['Street']}, {raw_customer_record['City']}, {raw_customer_record['State']}")
    print(
        f"ENRICHED OUTPUT: {processed_record['Street']}, {processed_record['City']}, {processed_record['State']} {processed_record['Zip']}")
    print("-" * 60)
    print("PIPELINE PROCESSING LOGS:")
    for step_num, log in enumerate(pipeline_history, start=1):
        print(f"  {step_num}. {log}")
    print("=" * 60)