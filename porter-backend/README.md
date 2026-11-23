# Porter Backend

Spring Boot backend for the Porter application.

## Local Development

### Prerequisites
- Java 17
- Maven 3.9+
- Docker and Docker Compose

### Setup

1. Start the local MySQL database:
```bash
docker-compose -f docker-compose.dev.yml up -d
```

2. Build the project:
```bash
mvn clean install
```

3. Run the application:
```bash
mvn spring-boot:run
```

The backend will be available at `http://localhost:8080`

### Stop Local Database
```bash
docker-compose -f docker-compose.dev.yml down
```

To remove the database volume (delete all data):
```bash
docker-compose -f docker-compose.dev.yml down -v
```

## Production Deployment

The application uses `application-prod.properties` profile in production.

Set the following environment variables in Google Cloud Run:
- `SPRING_PROFILES_ACTIVE=prod`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `PORTER_GCS_BUCKET_NAME`
- `PORTER_GCS_PROJECT_ID`
