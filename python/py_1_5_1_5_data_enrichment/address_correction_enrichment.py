import ssl
import time
import certifi
from geopy.geocoders import Nominatim
from rapidfuzz import process, utils

# Local mappings
STREET_ABBREVIATIONS = {
    "ave": "Avenue", "pkwy": "Parkway", "st": "Street", "rd": "Road"
}


def enrich_address_pipeline(raw_data: dict) -> dict:
    enriched = raw_data.copy()
    print(f"Original: {enriched}\n" + "-" * 50)

    # -------------------------------------------------------------------------
    # STEP 1: State Correction (Ny -> NY)
    # -------------------------------------------------------------------------
    if enriched.get("State"):
        enriched["State"] = enriched["State"].upper()

    # -------------------------------------------------------------------------
    # STEP 2: Street Cleaning & Typo Fallback
    # -------------------------------------------------------------------------
    if enriched.get("Street"):
        words = enriched["Street"].split()
        house_number = words[0]  # Save "350"

        # Suffix handling
        suffix = words[-1].lower().strip(".")
        if suffix in STREET_ABBREVIATIONS:
            words[-1] = STREET_ABBREVIATIONS[suffix]

        # Reconstruct without number for specific street lookup if needed
        raw_street_name = " ".join(words[1:])
        enriched["Street"] = f"{house_number} {raw_street_name}"

    # Setup Geocoder with SSL fix
    ssl_context = ssl.create_default_context(cafile=certifi.where())
    geolocator = Nominatim(user_agent="data_quality_pipeline", ssl_context=ssl_context)

    # --- Try 1: Strict Query ---
    query_string = f"{enriched['Street']}, {enriched['City']}, {enriched['State']}"
    print(f"[Step 2b - Querying Strict]: '{query_string}'")

    location = geolocator.geocode(query_string, addressdetails=True, timeout=10)

    # --- Try 2: Fallback Logic if Strict Fails (Handling the 'fifh' typo) ---
    if not location:
        print("[Notice]: Strict match failed. Activating Fuzzy/Fallback lookup...")
        # Search just for the raw street in that city to let Nominatim guess the typo
        fallback_query = f"{raw_street_name}, {enriched['City']}, {enriched['State']}"
        fallback_results = geolocator.geocode(fallback_query, exactly_one=False, addressdetails=True, limit=5)

        if fallback_results:
            # Gather candidate street names returned by the directory
            candidates = []
            for res in fallback_results:
                if 'road' in res.raw['address']:
                    candidates.append(res.raw['address']['road'])

            if candidates:
                # Use rapidfuzz to find the closest match to "fifh Avenue"
                # "fifh Avenue" will score exceptionally high against "Fifth Avenue"
                best_match, score, _ = process.extractOne(raw_street_name, list(set(candidates)),
                                                          processor=utils.default_process)
                print(f"[Fuzzy Match]: Found '{best_match}' with a confidence score of {score}%")

                # Re-query now with the perfectly corrected street name
                enriched["Street"] = f"{house_number} {best_match}"
                final_query = f"{enriched['Street']}, {enriched['City']}, {enriched['State']}"
                location = geolocator.geocode(final_query, addressdetails=True, timeout=10)

    # -------------------------------------------------------------------------
    # STEP 3: Map Final Results & Append ZIP
    # -------------------------------------------------------------------------
    if location and 'address' in location.raw:
        address_details = location.raw['address']

        # Standardize City Name from metadata (e.g., "New York City" -> "New York")
        if 'city' in address_details:
            enriched["City"] = address_details['city']

        # Standardize Street
        if 'road' in address_details:
            enriched["Street"] = f"{address_details.get('house_number', house_number)} {address_details['road']}"

        # Extract Zip Code
        api_zip = address_details.get('postcode')
        if api_zip:
            # Nominatim yields '10118-0110', we can slice to get standard 5-digit if desired
            enriched["Zip"] = api_zip.split('-')[0]
            print(f"[Step 3 - ZIP Appended]: {enriched['Zip']}")
    else:
        print("[Error]: Could not resolve address even with fallback logic.")

    return enriched


# --- Execution ---
if __name__ == "__main__":

    # Target low-quality record (correct record is: 350 Fifth Avenue, New York, NY 10118)
    raw_customer_record = {
        "Customer ID": 10421,
        "Name": "Alice Smith",
        "Street": "350 fifh ave",
        "City": "New York City",
        "State": "Ny",
        "Zip": ""
    }

    final_record = enrich_address_pipeline(raw_customer_record)
    print("\nEnriched Final Record:")
    print(final_record)