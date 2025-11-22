# Introduction
This example consists of several parts:
- Simple motor vibration simulation with FFT
- Use autoencoder (training and evaluation) with test driver
- Use N8 as for vibration analysis (also using autoencoder)

# 1. Simple motor vibration simulation with FFT
- Create virtual environment: python3 -m venv venv
- Install dependencies (requirements.txt): pip install -r requirements.txt
- Run simulation in motor_simulation-service:
  - Run simulation: python motor_simulation_service.py
  - Check the plots (close to see next plot)

# 2. Use autoencoder (training and evaluation) with test driver
- Start 3 services with docker compose:
  - Start services: docker-compose uptrain-service  detect-service simulate-service
- Use test/test_services.py (do_train = True): python3 test_services.py
- Model file will be saved in models folder
- Check the plot

# 3. Use N8 as for vibration analysis (also using autoencoder)
- Start all services with docker compose:
  - Start services: docker-compose up -d --build --force-recreate
- Open http://localhost:5678/
- Fill out the setup form
- Activate the workflow
- Click on Motor Vibration Control Workflow to see the details
- Click on the button "Execute workflow from TriggerWH" (WH stands for webhook)
- Paste the webhook URL in another browser tab and make sure to replace the :mode (variable) with "train", e.g. http://localhost:5678/webhook-test/.../motor-vibration/train (then press ENTER to trigger the workflow)
- You can observe the execution of the workflow in the n8n browser tab
- In the browser tab for the webhook request you should see the response {"data":[{"samples":5000,"status":"Training completed"}]}
- Start the workflow again and trigger it again with the webhook, but use detect, e.g. http://localhost:5678/webhook-test/.../motor-vibration/detect
- You should see "data":[{"anomalies":[2591,2592,2593,2594,2595,2830,2906,290 as the response


docker run --rm -it -v shared-models:/models alpine sh


