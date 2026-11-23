# Porter

Spring Boot backend with a Vite React frontend.

User login and signup data is stored in the MySQL-compatible database configured through environment variables. Local Docker MySQL is optional.

## Environment Configuration

The application supports multiple environments through Spring profiles and environment variables:

- **dev**: Local development with Docker Compose MySQL
- **prod**: Production deployment
- **aws**: AWS RDS or any cloud MySQL database

The database connection contract is always the same:

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://host:3306/porter?allowPublicKeyRetrieval=true&useSSL=true&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=your_db_username
SPRING_DATASOURCE_PASSWORD=your_db_password
SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
```

For first-time local development, use `SPRING_JPA_HIBERNATE_DDL_AUTO=update`. For managed environments, prefer `validate` after the schema exists.

Document uploads are handled through a pluggable storage provider:

```bash
# local for development, gcs for Google Cloud Storage
PORTER_DOCUMENT_STORAGE_PROVIDER=gcs
PORTER_GCS_BUCKET_NAME=your-gcs-bucket
PORTER_GCS_PROJECT_ID=your-gcp-project-id
PORTER_GCS_OBJECT_PREFIX=documents
PORTER_GCS_CREDENTIALS_PATH=/path/to/service-account.json
```

For Google Cloud Storage, the application uses the configured `PORTER_GCS_CREDENTIALS_PATH` when present. If it is empty, it falls back to Google Application Default Credentials. On Google Cloud runtimes, prefer an attached service account with permission to create objects in the bucket. Outside Google Cloud, either set `PORTER_GCS_CREDENTIALS_PATH` or set:

```bash
GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json
```

If you run with Docker and a local service-account JSON file, mount that file into the backend container and pass `GOOGLE_APPLICATION_CREDENTIALS` with the in-container path. Do not set `GOOGLE_APPLICATION_CREDENTIALS` when the runtime already has an attached Google service account.

### Running Without Docker

The application can run directly against any MySQL-compatible database:

1. Copy `.env.example` to `.env`
2. Set your database credentials:
   ```bash
   SPRING_DATASOURCE_URL=jdbc:mysql://your-db-endpoint:3306/porter?allowPublicKeyRetrieval=true&useSSL=true&serverTimezone=UTC
   SPRING_DATASOURCE_USERNAME=your_db_username
   SPRING_DATASOURCE_PASSWORD=your_db_password
   SPRING_PROFILES_ACTIVE=prod
   ```
3. Run the application:
   ```bash
   # Load environment variables from .env file
   export $(cat .env | xargs)
   mvn spring-boot:run
   ```

### Docker With Local MySQL

```bash
docker compose -f docker-compose.yml -f docker-compose.local.yml up --build
```

This starts MySQL, the backend, and the frontend. The backend connects to the compose MySQL service through the same `SPRING_DATASOURCE_*` variables used for remote databases.

### Docker With A Managed Database

For AWS RDS, Google Cloud SQL, or any cloud MySQL-compatible database:

1. Copy `.env.example` to `.env`
2. Set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`
3. Run:

```bash
docker compose up --build
```

`docker-compose.aws.yml` is kept as a deployment-specific alias if you want AWS defaults:

```bash
docker compose -f docker-compose.aws.yml up --build
```

### Manual Configuration

Set environment variables directly or use `.env` file:

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:mysql://your-host:3306/porter?allowPublicKeyRetrieval=true&useSSL=true&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=your_username
SPRING_DATASOURCE_PASSWORD=your_password
SPRING_PROFILES_ACTIVE=prod
```

## Backend Commands

```bash
mvn test
mvn package
```

## Seed Logins

```text
admin / admin123
operations / ops123
```

Login endpoint:

```text
POST /api/auth/login
```

After login, the frontend stores the returned username, role, and display name in browser storage. It does not store the raw password.

Signup endpoints:

```text
POST /api/auth/signup/admin
POST /api/auth/signup/operations
```

Request body:

```json
{
  "username": "newuser",
  "password": "secret",
  "displayName": "New User"
}
```

## Run With Docker Locally

From the repository root:

```bash
docker compose -f docker-compose.yml -f docker-compose.local.yml up --build
```

Open the frontend at:

```text
http://localhost:5174
```

The backend is exposed at:

```text
http://localhost:8081/api/health
```

When using `docker-compose.local.yml`, MySQL is exposed locally at:

```text
localhost:3307
database: porter
username: porter
password: porter
```

The frontend container serves the built React app with nginx and proxies `/api/*` requests to the backend service. With the local compose file, the backend waits for MySQL, creates the auth table, and seeds the default users if they are missing.

To remove local database data and start fresh:

```bash
docker compose down -v
```
