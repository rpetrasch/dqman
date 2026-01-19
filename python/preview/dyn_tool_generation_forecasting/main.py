"""
Main program to run the forecasting agentic AI system.
"""

import logging
from sources.file_util import generate_sales_data
from sources.graph import create_graph

# Configure logging
logging.basicConfig(level=logging.WARN, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')

def in_range(actual_val, expected_val, tolerance=1):
    """
    Check if actual value is within tolerance of expected value.
    :param actual_val: Actual value.
    :param expected_val: Expected value.
    :param tolerance: Allowed deviation from expected value.
    :returns: True if within tolerance, False otherwise
    """
    return expected_val - tolerance <= actual_val <= expected_val + tolerance

def evaluate(output):
    """
    Evaluate the output of the forecasting model against expected values.
    ToDo: The expected values are hardcoded for day 21. Evaluate for other days will fail.
    :param output: Forecasting model output.
    """
    print(f"\n4. Evaluation:")
    expected = {"a": 26, "b": 2, "c": 25}
    result_lines = output.split("\n")
    for result in result_lines:
        result = result.replace(":", "").replace("*", "").replace("- ", "").replace("–", "").replace(",", "").replace("  ", " ")
        if not "group" in result.lower(): continue
        try:
            result_parts = result.split(" ")
            group_name = result_parts[1].strip()
            group_value = result_parts[2].strip()
            actual_val = float(result_parts[4].strip())
            expected_val = expected.get(group_value)
            ok = in_range(actual_val, expected_val)
            print(f"   - Prediction for {group_name} {group_value} with value {actual_val}: {'OK' if ok else 'FAIL'}, expected ~{expected_val}.")
        except Exception as e:
            print(f"   - Error evaluating result for result '{result}': {e}")


def main(prediction_day=21):
    """
    Main function to run the forecasting agentic AI system.
    :param prediction_day: Day for which to generate forecasts.
    """
    print("--- Agentic forecasting system -- ")

    print("0. Generate sales data ...", end="")
    sales_df = generate_sales_data(plot=True)
    print(f"done, lines={sales_df.size}")

    # 1. Build the graph
    print("1. Build graph ...", end="")
    app = create_graph()
    print("done.")

    # 2. Mock User Input
    user_input = f"""
    Predict sales for the the day {prediction_day} using linear and non.linear regression on sales_data (CSV file content). 
    The sales data consists of 20 days of historical sales data and has the columns 
    - product: a, b, c,
    - day_num: day number (starting at 1),
    - sales_amount: actual sales amount for that day and product.
    I need the predicted sales amount for the day {prediction_day} for each product, 
    but I will need predictions for other days later as well."""

    # 3. Run the graph
    print(f"2. Run the graph with user prompt: {user_input}\n")
    inputs = {"user_request": user_input, "messages": []}
    
    # Accumulate final state
    final_output = None
    for output in app.stream(inputs):
        for key, value in output.items():
            # Capture final output from either responder OR dq_error. Both nodes set 'final_response' in the state update
            if key in ("responder", "dq_error"):
                final_output = value.get("final_response")

    # 4. Final Output
    print("3. Final agent response: ")
    if final_output:
        print(final_output)
        evaluate(final_output)
    else:
        print("No final response generated.")
    
if __name__ == "__main__":
    """Main entry point: Delegate to main function."""
    main(prediction_day=21)
