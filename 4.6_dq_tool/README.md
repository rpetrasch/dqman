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

## Running the Application

### Backend
Start the Spring Boot application:
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
The H2 Console is enabled and can be accessed at `http://localhost:8080/h2-console`.
- **JDBC URL**: `jdbc:h2:mem:testdb`
- **User Name**: `sa`
- **Password**: `password`
