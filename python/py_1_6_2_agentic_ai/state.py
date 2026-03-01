from typing import TypedDict, Optional, Dict, Any

class AgentState(TypedDict):
    company_name: str
    location: Optional[str]
    wikipedia_content: Optional[str]
    validation_status: Optional[str]  # "correct", "minor_issue", "not_found"
    validation_reason: Optional[str]
    extracted_data: Optional[Dict[str, Any]]
    final_output: Optional[str]
