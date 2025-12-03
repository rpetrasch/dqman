# Installation
- pip install mlflow boto3
- docker compose up -d
- When postgreSQL data error: 
  - docker-compose down --volumes --remove-orphans
  - restart

# Test
## A. Check the MLflow UI
- Open http://localhost:5001 in  browser
- Click on "Experiments" and then on "Docker_MinIO_Test" in the list.
- In the "Run Name" column, you see an entry in blue. Click on it.
- Seelct the "Artifacts" tab. You should see test_artifact.txt. Click it to preview the text.

## B. Check the MinIO Console (Optional but Recommended) 
To prove the file actually lives in the object store and not just on the server:
- Open http://localhost:9001 (The MinIO Console).
- Login with minio_user / minio_password.
- Go to the Object Browser.
- Click the mlflow bucket.
- Navigate through the numbered folders (Experiment ID -> Run ID -> Artifacts) until you find your text file.

## C. Run examples
### 1. dq_pipeline_simple.py
This is the simplest version of the pipeline: Ingest -> Clean & Validate -> Decide.
- python dq_pipeline_simple.py
- Check MLflow UI and MinIO Console.

### 2. dq_pipeline_llm_enrich.py
This version of the pipeline uses an LLM to standardize text data (e.g., capitalization, inferring context).
Ingest -> Enrich (LLM) -> Validate -> Decide
It has an LLM simulation mode to simulate what an LLM would do but without an actual API call.
Setup OpenAI API key in the LLM_API_KEY variable in the script and set USE_REAL_LLM = True to use the real API.
- python dq_pipeline_llm_enrich.py
- Check MLflow UI and MinIO Console.

