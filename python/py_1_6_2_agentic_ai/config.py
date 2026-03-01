from langchain_ollama import ChatOllama

# You can change the model name here if needed
LLM_MODEL = "gpt-oss" 

def get_llm():
    return ChatOllama(model=LLM_MODEL, temperature=0)
