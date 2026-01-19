
"""
Graph definition for the forecasting agent workflow.
"""
from langgraph.graph import StateGraph, END
from .state import AgentState
from .nodes import (
    dq_check_node,
    dq_error_node,
    router_node,
    data_analysis_node,
    coder_node,
    tool_builder_node,
    executor_node,
    response_node
)


def dq_decision(state: AgentState):
    """
    Conditional edge logic for DQ check.
    :param state: Agent state dictionary.
    """
    if state.get("dq_status") == "error":
        return "error"
    else:
        return "valid"


def route_decision(state: AgentState):
    """
    Conditional edge logic for execution reuse.
    :param state: Agent state dictionary.
    """
    if state["analysis"] == "execute_existing":
        return "executor"
    else:
        return "coder"


def create_graph():
    """
    Builds the workflow graph.
    :return: compiled graph
    """
    workflow = StateGraph(AgentState)

    # Add Nodes
    workflow.add_node("dq_check", dq_check_node)
    workflow.add_node("dq_error", dq_error_node)
    
    workflow.add_node("data_analysis", data_analysis_node)
    workflow.add_node("router", router_node)
    workflow.add_node("coder", coder_node)
    workflow.add_node("builder", tool_builder_node)
    workflow.add_node("executor", executor_node)
    workflow.add_node("responder", response_node)

    # 1. Start with DQ Check
    workflow.set_entry_point("dq_check")

    # 2. Branch: Error vs Valid
    workflow.add_conditional_edges(
        "dq_check",
        dq_decision,
        {
            "error": "dq_error",
            "valid": "data_analysis"
        }
    )
    
    # Error ends the flow
    workflow.add_edge("dq_error", END)

    workflow.add_edge("data_analysis", "router")

    # 3. Router logic (Creating vs Existing)
    workflow.add_conditional_edges(
        "router",
        route_decision,
        {
            "executor": "executor",
            "coder": "coder"
        }
    )

    # Creation Chain
    workflow.add_edge("coder", "builder")
    workflow.add_edge("builder", "executor")

    # Execution & Response
    workflow.add_edge("executor", "responder")
    workflow.add_edge("responder", END)

    return workflow.compile()
