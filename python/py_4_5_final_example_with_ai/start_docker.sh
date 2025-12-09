docker compose down
docker rmi dqman/4.5_detect_service:latest dqman/simulation_service:latest dqman/4.5_train_service:latest dqman/4.5_test_service:latest dqman/4.5_deploy_service:latest dqman/4.5_monitor_service:latest dqman/4.5_serve_service:latest dqman/4.5_serve_model_service:latest dqman/4.5_serve_model_with_ai_service:latest
docker compose up --build -d
docker compose logs -f --tail=100
