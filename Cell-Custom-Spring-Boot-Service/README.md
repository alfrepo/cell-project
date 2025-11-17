# Cell-Custom-Spring-Boot-Service

## Development

Before starting the application, the following command is required:

```bash
podman-compose -f docker-compose.yml -f docker-compose.mongo.dev.yml up -d
```

## MongoDB Management (Development)

In the development environment, you can manage the MongoDB instance using Mongo-Express, a web-based administration interface.

*   **Access Mongo-Express:** Open your web browser and navigate to `http://localhost:8081/db/myappdb/`.
*   **Connecting to the Database:** Since authentication is disabled in the development setup, Mongo-Express should automatically connect to the `myappdb` database running in the `mongodb` service. You should see your `employees` collection and its data.
