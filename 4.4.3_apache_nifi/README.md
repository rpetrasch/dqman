# Introduction
Example for Apache Nifi (using auto trails / revisions).

# Installation / test
- Make sure docker service is up and running
- Create the jar file: mvn clean package
- Create docker network: docker network create nifi-network
- Run docker compose: docker-compose up -d
- Copy to NiFi's lib folder: docker cp target/4.4.3_apache_nifi-1.0.jar nifi:/opt/nifi/nifi-current/lib/
- Restart nifi so that the processor is available: docker restart nifi
- Open UI in browser: https://localhost:8443/nifi and login with credentials: admin / NiFi123NiFi123!

# Create a flow
- 

# ToDo
- Write tests
- Example for problems


