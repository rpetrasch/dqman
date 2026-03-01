import sys
from langgraph.graph import StateGraph, END
from state import AgentState
from nodes import lookup_node, validation_node, extraction_node

def check_validation(state: AgentState):
    """
    Conditional logic to determine the next step.
    """
    status = state.get("validation_status")
    if status in ["correct", "minor_issue"]:
        return "continue"
    else:
        return "abort"

def create_graph():
    workflow = StateGraph(AgentState)

    # Add nodes
    workflow.add_node("lookup", lookup_node)
    workflow.add_node("validate", validation_node)
    workflow.add_node("extract", extraction_node)

    # Add edges
    workflow.set_entry_point("lookup")
    workflow.add_edge("lookup", "validate")

    # Conditional edge
    workflow.add_conditional_edges(
        "validate",
        check_validation,
        {
            "continue": "extract",
            "abort": END
        }
    )

    workflow.add_edge("extract", END)

    return workflow.compile()

def main():
    print("Agentic AI - Company Name Validator")
    print("------------------------------------------")
    
    if len(sys.argv) > 1:
        company_name = sys.argv[1]
        location = sys.argv[2] if len(sys.argv) > 2 else ""
    else:
        company_name = input("Enter Company Name: ")
        location = input("Enter Location (optional): ")

    inputs = {"company_name": company_name, "location": location}
    
    app = create_graph()
    
    print(f"\nProcessing '{company_name}'...\n")
    
    # Run the graph
    result = app.invoke(inputs)
    
    print("\n--- Final Result ---\n")
    status = result.get("validation_status")
    print(f"Validation Status: {status}")
    print(f"Reason: {result.get('validation_reason')}")
    
    if status != "not_found":
        print("\nExtracted Data:")
        data = result.get("extracted_data")
        if data:
            print(json.dumps(data, indent=2) if isinstance(data, dict) else data)
        else:
            print("No data extracted.")
            
        print(f"\nOutput Message: {result.get('final_output')}")

if __name__ == "__main__":
    import json
    main()
