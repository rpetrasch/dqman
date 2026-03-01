"""
org.dqman.Main module for explainable AI confidence using an LMM iike llama3
"""

import ollama

if __name__ == "__main__":
    # Review text (user inpute) we want to analyze
    review_text = "The service was slow, but the food was absolutely amazing."
    model_to_use = "llama3"

    # 1: "Black Box" (No Explainability)
    print("--- 1. 'Black Box' Example ---")
    prompt_no_xai = f"""
    Classify the sentiment of this review (Positive, Negative, or Neutral). Give only the classification:
    '{review_text}'
    """
    response_no_xai = ollama.chat(
        model=model_to_use, messages=[{"role": "user", "content": prompt_no_xai}]
    )
    print(f"Prompt: {prompt_no_xai}")
    print(f"LLM Answer: {response_no_xai['message']['content']}\n")

    # 2: "Glass Box" (With Explainability)
    print("--- 2. 'Explainable' Example ---")
    prompt_with_xai = f"""
    You are a sentiment analysis expert. Your task is to classify a review.
    First, provide your step-by-step reasoning.
    Second, on a new line, state the final classification (Positive, Negative, or Neutral).
    
    Review: '{review_text}'
    """
    response_with_xai = ollama.chat(
        model=model_to_use, messages=[{"role": "user", "content": prompt_with_xai}]
    )
    print(f"Prompt: {prompt_with_xai}")
    print(f"LLM Answer:\n{response_with_xai['message']['content']}\n")

    # 3: "Post-Hoc Rationalization")
    print("--- 3. 'Post-Hoc Rationalization' Example ---")
    prompt_with_xai = f"""
    You are a sentiment analysis expert. Your task is to classify a review.
    First, State the final classification (Positive, Negative, or Neutral). Give only the classification.
    Second: Explain your classification.
    
    Review: '{review_text}'
    """
    response_with_xai = ollama.chat(
        model=model_to_use, messages=[{"role": "user", "content": prompt_with_xai}]
    )
    print(f"Prompt: {prompt_with_xai}")
    print(f"LLM Answer:\n{response_with_xai['message']['content']}")
