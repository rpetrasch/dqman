# Data Quality Tool

## Prerequisites
- Java 21+
- Maven
- Node.js & npm
- Angular CLI (`npm install -g @angular/cli`), use sudo if needed

## Installation

1.  **Backend**:
    ```bash
    cd _4_6_dq_tool
    mvn clean install
    ```

2.  **Frontend**:
    ```bash
    cd _4_6_dq_tool/frontend
    npm install
    ```

## Running the Application: manual (no docker)
### start postgresql
- use docker to start postgresql:
```bash
docker compose up crm-postgres
```

### Backend
- set the following workspace in the launch.json (in the .vscode folder):
```json
        {
            "type": "java",
            "name": "DQ Tool",
            "request": "launch",
            "mainClass": "org.dqman.Main",
            "projectName": "6_dq_tool",
            "cwd": "${workspaceFolder}/4.6_dq_tool/"
        }

- Start the Spring Boot application:
```bash
cd _4_6_dq_tool
mvn spring-boot:run
```
The backend API will be available at `http://localhost:8081`.

### Swagger UI
API documentation is available at `http://localhost:8081/swagger-ui.html`.

### Frontend
Start the Angular development server:
```bash
cd _4_6_dq_tool/frontend
ng serve
```
Navigate to `http://localhost:4200`. The application will automatically reload if you change any of the source files.

## Accessing the Database
The H2 Console is enabled and can be accessed at `http://localhost:8081/h2-console`.
- **JDBC URL**: `jdbc:h2:mem:testdb` (for in-memory database) or
 `jdbc:h2:file:./data/testdb` (for file-based database)
- **User Name**: `sa`
- **Password**: `password`

# Running the Application: docker
- use docker-compose to run the application:
```bash
docker-compose up
```
