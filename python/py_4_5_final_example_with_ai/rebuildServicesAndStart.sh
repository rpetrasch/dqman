#!/bin/bash
# This script is used to rebuild the services and start them with Docker Compose.
docker compose down
docker rmi dqman/4.5_detect_service:v1
docker rmi dqman/4.5_train_service:v1
docker rmi dqman/4.5_simulation_service:v1
docker compose up --build -d


