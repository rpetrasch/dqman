
"""
Nodes for the forecasting agent workflow.
"""
import logging
import os
import pandas as pd
import importlib.util
import sys
from langchain_openai import ChatOpenAI
from langchain_core.messages import SystemMessage, HumanMessage
from .config_loader import default_config
from .file_util import read_csv_file
from .state import AgentState
from langchain_core.tools import BaseTool
from langchain.agents import create_agent

# Setup Interfaces
llm_config = default_config.llm

# Initialize models
user_llm = ChatOpenAI(
    base_url=llm_config['base_url'],
    api_key=llm_config['api_key'],
    model=llm_config['user_model'],
    temperature=0.7
)

coder_llm = ChatOpenAI(
    base_url=llm_config['base_url'],
    api_key=llm_config['api_key'],
    model=llm_config['coder_model'],
    temperature=0.2 
)

agent_llm = ChatOpenAI(
    base_url=llm_config['base_url'],
    api_key=llm_config['api_key'],
    model=llm_config['agent_model'],
    temperature=0.2
)

# --- DQ NODES ---

def dq_check_node(state: AgentState):
    """
    Validates if the user prompt's column assumptions match the actual CSV file.
    :param state: Agent state dictionary.
    """
    
    # 1. Load CSV Header
    try:
        csv_columns = read_csv_file(header_only=True) # Read only header
    except Exception as e:
         return {"dq_status": "error", "dq_error_message": f"Failed to read CSV header: {e}"}

    # 2. Ask LLM to validate
    prompt = f"""
    The user request entails: "{state['user_request']}"
    
    The actual CSV columns are: {csv_columns}
    
    Task: Check if the user's request mentions columns or data structures that DO NOT exist in the CSV.
    - If the user implies columns (like 'sales_amount' vs 'amount') that mismatch, return ERROR.
    - If the user request is compatible or generic enough, return VALID.
    
    Return EXACTLY one word: 'VALID' or 'ERROR: [Reason]'
    """
    
    response = user_llm.invoke([HumanMessage(content=prompt)]).content.strip()
    
    if response.upper().startswith("ERROR"):
        return {"dq_status": "error", "dq_error_message": response}
    else:
        return {"dq_status": "valid"}


def dq_error_node(state: AgentState):
    """
    Reports the DQ error to the user.
    :param state: Agent state dictionary.
    """
    msg = f"Data Quality Check Failed. {state['dq_error_message']}"
    logging.warning(msg)
    return {"final_response": msg}


# --- DATA ANALYSIS AND ROUTER NODES ---

def data_analysis_node(state: AgentState):
    """
    Node for data analysis: Determines the groups and trends of the sales data.
    :param state: Agent state dictionary.
    :return: Updated state dictionary (tools and groups_trends).
    """

    print(f"   * Data Analysis")
    print(f"     a) Read sales data")
    sales_df = read_csv_file()
    sales_csv = sales_df.to_csv(index=False)

    # 1. Ask LLM to determine the needed tool name FIRST
    print(f"     b) Analyzing user request and sales data")
    prompt = f"""
    User Request: {state['user_request']}

    You are a data analyst. Determine the statistical trends of sales data used for a forecasting model, there can be more than one trend model, e.g. for each product (group).
    Focus on one model per type/group of data (linear, non-linear, periodic, asymptotic, parabolic, etc.).

    Return exactly two lines:
    - 1st line: the string 'Group(s): ' followed by the group name(s) as a comma separated list, e.g. product
    - 2nd line: a comma separated list with the data group and the trend (separated by ':'), e.g., 'a: linear, b:nonlinear, c:asymptotic'.
    
    The sales data for the analysis:
    {sales_csv}
        """
    response = user_llm.invoke([HumanMessage(content=prompt)])

    print(f"     c) Determining the tools: ", end="")
    tools = set()
    lines = response.content.split("\n")
    groups_trends = lines[1].split(",")
    for group_trend in groups_trends:
        tool = group_trend.split(":")[1]
        tools.add(tool)
    print(f"{','.join(tools)}")
    return {"tools": tools, "groups_trends": lines}


def router_node(state: AgentState):
    """
    Analyzes the user request and decides if we have a tool or need to create one.
    :param state: Agent state dictionary.
    :return: Updated state dictionary (analysis, tools_needed).
    """
    print(f"   * Router")
    tools = state['tools']
    tools_needed = []
    for tool in tools:
        tool_name = tool.strip()

        # 2. Check if file exists
        tool_path = f"sources/dynamic_tools/{tool_name}.py"

        if os.path.exists(tool_path):
            print(f"     - Tool '{tool_name}' already exists at {tool_path}, skipping generation.")
        else:
            print(f"     - Tool '{tool_name}' not found, generation needed.")
            tools_needed.append(tool_name)

    if len(tools_needed) == 0:
        return {"analysis": "execute_existing", "tools_needed": ""}
    else:
        return {"analysis": "create_new", "tools_needed": tools_needed}


def coder_node(state: AgentState):
    """
    Generates the Python code for the requested tool.
    :param state: Agent state dictionary.
    """
    tools = state['tools_needed']
    print(f"   * Coder, tools needed: {len(tools)}")

    tool_code = []
    for tool_name in tools:
        request = state['user_request']

        prompt = f"""
        Task: Write a Python function named '{tool_name}' that solves the following user request:
        "{request}"
        
        Important: Focus only on one single forecasting model. Do not implement different forecasting models like linear, non-linear, periodic, asymptote, etc. in one single function.
        Use ONE prediction model in ONE function.
        
        Requirements:
        1. Import `tool` from `langchain_core.tools`.
        2. Decorate the function with `@tool`.
        3. The function MUST accept a list of comma-separated values (header: day, amount) as an argument to get the data.
        4. The function MUST accept a numerical argument 'day' to get the day number or date/time for the prediction.        
        5. Provide a clear, descriptive docstring for the tool so an LLM knows when to use it.
        6. Return the prediction as a result (string) that the agent can interpret, e.g. 'prediction (linear): 39.12'.
        7. RETURN ONLY VALID PYTHON CODE. No markdown formatting, no explanation.
        8. Use a print('        - tool <tool name> called') statement for debugging purposes.
        """

        print(f"     - Generating code for tool '{tool_name}' ... ", end="")
        response = coder_llm.invoke([HumanMessage(content=prompt)])
        code = response.content.replace("```python", "").replace("```", "").strip()
        tool_code.append(code)
        print(f"done.")
    return {"generated_code": tool_code}


def tool_builder_node(state: AgentState):
    """
    Saves the generated code to a file.
    :param state: Agent state dictionary.
    """
    num_tools = len(state['tools_needed'])
    print(f"   * Tool Builder: {num_tools} tools")
    for tool_name, code in zip(state['tools_needed'], state['generated_code']):
        file_path = f"sources/dynamic_tools/{tool_name}.py"
        print(f"     - Saving tool code to {file_path} ... ", end="")
        try:
            with open(file_path, "w") as f:
                f.write(code)
            print(f"done.")
        except Exception as e:
            print(f"Error saving tool code: {e}")
    return {}


def executor_node(state: AgentState):
    """
    Loads tools dynamically and executes the agent.
    :param state: Agent state dictionary.
    """
    print(f"   * Executor: Setting up Agent with tools)")
    
    # 1. Load all tools from sources/dynamic_tools
    tools_dir = "sources/dynamic_tools"
    loaded_tools = []
    print(f"     a) Loading tools from {tools_dir}: ... ", end="")
    if os.path.exists(tools_dir):
        for filename in os.listdir(tools_dir):
            if filename.endswith(".py") and filename != "__init__.py":
                module_name = filename[:-3]
                file_path = os.path.join(tools_dir, filename)
                try:
                    spec = importlib.util.spec_from_file_location(module_name, file_path)
                    module = importlib.util.module_from_spec(spec)
                    sys.modules[module_name] = module
                    spec.loader.exec_module(module)
                    # Find @tool decorated functions (checking if they are BaseTool instances)
                    for attr_name in dir(module):
                        attr = getattr(module, attr_name)
                        if isinstance(attr, BaseTool):
                             loaded_tools.append(attr)
                             print(f"{attr.name} ", end="")
                except Exception as e:
                    print(f"Failed to load {module_name}: {e}")
    print(f"done.")
    if not loaded_tools:
        print("No tools found.")
        return {"execution_result": "No tools found to execute."}

    # 2. Load sales data
    print(f"     b) Loading sales data ... ", end="")
    sales_df = read_csv_file()
    sales_csv = sales_df.to_csv(index=False)
    print(f"done.")

    # 2. Create and invoke agent
    print(f"     c) Creating and invoking agent ...")
    agent = create_agent(agent_llm, tools=loaded_tools)
    groups_trends = state['groups_trends']

    prompt = f"""
    User Request: {state['user_request']}

    You are a forecasting specialist. The sales data has the following groups and trends:
    {groups_trends}
    
    Split the data into groups, e.g., for column 'product', and create forecasting (predictions). 
    For each data group you can use a different tool (depending on the trend).
    This is your plan:
    1. Split the data into different lists. For each group value a separate list of comma-separated values (header: day, amount) must be created
    2. Look at the groups and trends and decide what tools to use for each list separately.
    3. For each list: Call the tool with the data (list) and receive the result.
    4. Aggregate the results form the tool calls: Return a result line for each group value like this:
       'Group(s): <group name(s)>, <group value>:' <day> <prediction> (<trend>)    
       Example: 'Group(s): product, a: 21: 39.12 (linear)'
     
    This is the sales data for the analysis (content of the sales data CSV file):
    {sales_csv}
    """

    inputs = {"messages": [HumanMessage(content=prompt)]}

    try:
        result = agent.invoke(inputs)
        # Extract final response from the last message content
        last_message = result["messages"][-1]
        final_content = last_message.content
        print(f"     d) Agent finished. Result length: {len(final_content)}")
    except Exception as e:
        final_content = f"Error during agent execution: {e}"
        print(f"     d) {final_content}")

    return {"execution_result": final_content}


def response_node(state: AgentState):
    """
    Node for final response generation.
    :param state: Agent state dictionary.
    :return: Updated state dictionary (final_response).
    """
    res = f"\nResult:\n{state['execution_result']}"
    return {"final_response": res}
