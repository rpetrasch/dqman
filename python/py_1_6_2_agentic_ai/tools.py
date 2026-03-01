import datetime

from langchain_core.tools import tool
from langchain_community.tools import WikipediaQueryRun
from langchain_community.utilities import WikipediaAPIWrapper
import requests

# 1. Wikipedia Tools
# We use the built-in wrapper but you can also make a custom one.
wikipedia = WikipediaQueryRun(api_wrapper=WikipediaAPIWrapper(top_k_results=1, doc_content_chars_max=4000))

@tool
def search_wikipedia(name_location: str) -> str:
    """Useful to search for company information on Wikipedia."""
    return wikipedia.run(name_location)


@tool
def search_wikipedia_full_content(name: str) -> str:
    """Search for full company information on Wikipedia using the MediaWiki API"""
    url = "https://en.wikipedia.org/w/api.php"
    params = {
        "action": "query",
        "format": "json",
        "prop": "extracts",
        "explaintext": True,
        "titles": name,
    }
    headers = {
        "User-Agent": "DQ_Book_Bot/1.0"
    }

    r = requests.get(url, params=params, headers=headers)
    r.raise_for_status()

    data = r.json()
    pages = data["query"]["pages"]
    return next(iter(pages.values())).get("extract", "")


# 2. Age calculation Tool
@tool
def calc_age(founded: int) -> int:
    """Determines the age of a company based on its founding year."""
    current_year = datetime.datetime.now().year
    return current_year - founded

# 3. Currency Converter Tool
@tool
def convert_usd_to_eur(amount_usd: float) -> float:
    """Converts an amount from USD to EUR using a fixed rate of 0.85."""
    try:
        # ensuring input is a float
        if isinstance(amount_usd, str):
            amount_usd = float(amount_usd.replace(',', '').replace('$', ''))
        return amount_usd * 0.85
    except ValueError:
        return 0.0
