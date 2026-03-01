import json
from langchain_core.messages import SystemMessage, HumanMessage
from langchain_core.prompts import ChatPromptTemplate
from langchain.agents import create_agent

from config import get_llm
from state import AgentState
from tools import search_wikipedia, convert_usd_to_eur, search_wikipedia_full_content, calc_age

llm = get_llm()

# Node 1: Lookup Agent
def lookup_node(state: AgentState):
    """
    Search Wikipedia for the company.
    """
    company = state["company_name"]
    location = state.get("location", "")
    query_short = f"{company} {location}".strip()
    query_full = f"{company}".strip()

    print(f"--- Searching Wikipedia for: {query_short} ---")
    try:
        content_short = search_wikipedia.invoke({    # Only summary
            "name_location": query_short
        })
        content_full = search_wikipedia_full_content.invoke({  # Full content
            "name": query_full
        })
        content = f"{content_short}\n\n{content_full}"
    except Exception as e:
        content = f"Error searching wikipedia: {str(e)}"
        
    return {"wikipedia_content": content}

# Node 2: Validation Agent
def validation_node(state: AgentState):
    """
    Compare user input with Wikipedia content.
    """
    print("--- Validating Company Name ---")
    
    content = state["wikipedia_content"]
    user_company = state["company_name"]
    
    # Simple prompt for the LLM
    system_prompt = (
        "You are a Data Validation Agent. "
        "Compare the USER PROVIDED company name with the text retrieved from Wikipedia. "
        "Determine if they refer to the same entity.\n"
        "Possible statuses:\n"
        "- 'correct': Names match or are very close variations.\n"
        "- 'minor_issue': Names are slightly different but refer to the same entity (e.g. Inc vs Ltd, or typo).\n"
        "- 'not_found': The retrieved text does not seem to be about the user provided company or the difference is too big.\n\n"
        "Return ONLY a JSON object with keys: 'status' and 'reason'."
    )
    
    user_msg = f"User Company: {user_company}\n\nWikipedia Text snippet:\n{content[:2000]}"
    
    messages = [
        SystemMessage(content=system_prompt),
        HumanMessage(content=user_msg)
    ]
    
    response = llm.invoke(messages)
    
    # Attempt to parse JSON
    try:
        content_str = response.content.strip()
        # Clean up markdown code blocks if present
        if "```json" in content_str:
            content_str = content_str.split("```json")[1].split("```")[0]
        elif "```" in content_str:
             content_str = content_str.split("```")[1].split("```")[0]
             
        result = json.loads(content_str)
        status = result.get("status", "not_found")
        reason = result.get("reason", "Parsing failed")
    except Exception as e:
        print(f"JSON Parsing failed: {e}")
        status = "not_found"
        reason = "Failed to parse validation response."

    return {
        "validation_status": status, 
        "validation_reason": reason
    }

# Node 3: Extraction Agent (ReAct)
def extraction_node(state: AgentState):
    """
    Extracts data and converts currency using tools.
    """
    print("--- Extracting Data ---")
    
    content = state["wikipedia_content"]
    
    # We create a specialized agent for this task that has access to the conversion tool
    tools = [calc_age]
    
    # We don't need search here, just the content
    prompt_template = ChatPromptTemplate.from_messages([
        ("system", "You are an Extraction Agent. "
                   "Extract the following fields from the text: Name, Location, Founding Year, Employees, Revenue (latest). "
                   "If you found a founding year for the company, you MUST use the 'calc_age' tool to get the age in years. "
                   "Return the final output as a JSON object with keys: "
                   "name, location, employees, founded, age, revenue. "
                   "Do not output markdown, just the JSON string."),
        ("user", "Text: {text}")
    ])
    
    # Create the agent executor
    # We use a ReAct style agent or just bind tools if model supports it robustly. 
    # create_react_agent is good for forcing tool usage.
    agent_executor = create_agent(llm, tools)
    
    # Invoke
    # We format the prompt first or just pass input
    # create_react_agent expects 'messages' in input
    messages = prompt_template.invoke({"text": content[:4000]}).to_messages()
    
    result = agent_executor.invoke({"messages": messages})
    
    # The result['messages'][-1].content should constitute the final answer
    final_response = result["messages"][-1].content
    
    # Parse the extraction
    try:
        # Cleanup
        content_str = final_response.strip()
        if "```json" in content_str:
            content_str = content_str.split("```json")[1].split("```")[0]
        elif "```" in content_str:
             content_str = content_str.split("```")[1].split("```")[0]
        
        extracted_data = json.loads(content_str)
    except:
        extracted_data = {"raw_output": final_response}
        
    return {
        "extracted_data": extracted_data,
        "final_output": final_response
    }
