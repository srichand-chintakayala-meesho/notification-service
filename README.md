# Notification Service

A comprehensive SMS notification service built with Spring Boot, featuring Kafka message queuing, Redis caching, MySQL persistence, and Elasticsearch search capabilities.

## Features

- **SMS Sending**: Asynchronous SMS delivery via third-party API
- **Message Queuing**: Kafka-based message processing
- **Caching**: Redis-based blacklist management
- **Persistence**: MySQL database for SMS request storage
- **Search**: Elasticsearch for advanced SMS search capabilities
- **Authentication**: Basic authentication filter
- **Monitoring**: Spring Boot Actuator for health checks

## Technology Stack

- **Java 8**
- **Spring Boot 2.7.18**
- **Spring Data JPA**
- **Spring Kafka**
- **Spring Data Redis**
- **MySQL 8.0**
- **Redis**
- **Apache Kafka**
- **Elasticsearch 7.17.14**
- **Maven**

## Prerequisites

Before running the application, ensure you have the following installed:

1. **Java 8** or higher
2. **Maven 3.6** or higher
3. **MySQL 8.0**
4. **Redis**
5. **Apache Kafka** (with Zookeeper)
6. **Elasticsearch 7.17.14**

## Setup Instructions

### 1. Database Setup

Create a MySQL database:

```sql
CREATE DATABASE notification_db;
CREATE USER 'notification_user'@'localhost' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON notification_db.* TO 'notification_user'@'localhost';
FLUSH PRIVILEGES;
```

### 2. Redis Setup

Start Redis server:

```bash
redis-server
```

### 3. Kafka Setup

Start Zookeeper and Kafka:

```bash
# Start Zookeeper
bin/zookeeper-server-start.sh config/zookeeper.properties

# Start Kafka
bin/kafka-server-start.sh config/server.properties

# Create the required topic
bin/kafka-topics.sh --create --topic notification.send_sms --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
```

### 4. Elasticsearch Setup

Start Elasticsearch:

```bash
bin/elasticsearch
```

### 5. Application Configuration

Update the `application.yml` file with your configuration:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/notification_db
    username: notification_user
    password: password
  
  redis:
    host: localhost
    port: 6379
  
  kafka:
    bootstrap-servers: localhost:9092

elasticsearch:
  host: localhost
  port: 9200

sms:
  api:
    key: your-actual-api-key-here
```

### 6. Build and Run

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## API Endpoints

### Authentication

All API endpoints require an `Authorization` header. For testing, you can use any non-empty value.

### 1. Send SMS

**POST** `/v1/sms/send`

**Request Body:**
```json
{
  "phone_number": "+919940630272",
  "message": "Welcome to Meesho!"
}
```

**Success Response:**
```json
{
  "data": {
    "request_id": "uuid-here",
    "comments": "Successfully Sent"
  }
}
```

### 2. Get SMS Details

**GET** `/v1/sms/{request_id}` or `/v1/sms/id/{id}`

**Success Response:**
```json
{
  "data": {
    "id": 1,
    "phoneNumber": "+919940630272",
    "message": "Welcome to Meesho!",
    "status": "SENT",
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T10:00:05"
  }
}
```

### 3. Add to Blacklist

**POST** `/v1/blacklist`

**Request Body:**
```json
{
  "phone_numbers": ["+91904353454534", "+91980998787678"]
}
```

**Success Response:**
```json
{
  "data": "Successfully blacklisted"
}
```

### 4. Remove from Blacklist

**DELETE** `/v1/blacklist`

**Request Body:**
```json
{
  "phone_numbers": ["+91904353454534"]
}
```

**Success Response:**
```json
{
  "data": "Successfully removed from blacklist"
}
```

### 5. Get Blacklisted Numbers

**GET** `/v1/blacklist`

**Success Response:**
```json
{
  "data": ["+91904353454534", "+91980998787678"]
}
```

### 6. Search SMS by Phone Number and Time Range

**GET** `/v1/search/sms/phone?phone_number=+919940630272&start_time=2024-01-01T00:00:00&end_time=2024-01-02T00:00:00&page=0&size=10`

### 7. Search SMS by Text

**GET** `/v1/search/sms/text?text=Welcome&page=0&size=10`

## Architecture

### Flow Diagram

```
1. Client → POST /v1/sms/send
2. Service → Save to MySQL + Publish to Kafka
3. Consumer → Process from Kafka
4. Consumer → Check Redis blacklist
5. Consumer → Call third-party SMS API
6. Consumer → Update MySQL status
7. Consumer → Index in Elasticsearch
```

### Components

- **Controllers**: Handle HTTP requests
- **Services**: Business logic implementation
- **Repositories**: Data access layer
- **Kafka Consumer**: Asynchronous message processing
- **Redis Service**: Blacklist management
- **Elasticsearch Service**: Search functionality
- **Third-party API Service**: SMS delivery

## Monitoring

### Health Checks

Access health check endpoints:

- `GET /v1/actuator/health` - Application health
- `GET /v1/actuator/info` - Application information
- `GET /v1/actuator/metrics` - Application metrics

### Logging

The application uses SLF4J with Logback. Logs are configured to show:
- Application logs at DEBUG level
- Kafka logs at INFO level
- Elasticsearch logs at INFO level

## Error Handling

The application includes comprehensive error handling:

- **Validation Errors**: Automatic validation of request parameters
- **Business Logic Errors**: Proper error codes and messages
- **System Errors**: Graceful handling of external service failures
- **Global Exception Handler**: Consistent error response format

## Scaling Considerations

### Horizontal Scaling

1. **Multiple Application Instances**: Run multiple instances behind a load balancer
2. **Kafka Partitions**: Increase partitions for better throughput
3. **Database Connection Pool**: Configure appropriate connection pool sizes
4. **Redis Cluster**: Use Redis cluster for high availability

### Performance Optimization

1. **Database Indexing**: Ensure proper indexes on frequently queried columns
2. **Caching Strategy**: Implement appropriate caching for frequently accessed data
3. **Connection Pooling**: Optimize connection pool configurations
4. **Batch Processing**: Implement batch processing for bulk operations

## Security Considerations

1. **API Key Management**: Store API keys securely using environment variables
2. **Input Validation**: Validate all input parameters
3. **Rate Limiting**: Implement rate limiting for API endpoints
4. **HTTPS**: Use HTTPS in production
5. **Authentication**: Implement proper authentication mechanism

## Deployment

### Docker Deployment

Create a `Dockerfile`:

```dockerfile
FROM openjdk:8-jre-alpine
COPY target/notification-service-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Environment Variables

Set the following environment variables:

```bash
export SMS_API_KEY=your-api-key
export SPRING_PROFILES_ACTIVE=prod
export DB_HOST=your-db-host
export REDIS_HOST=your-redis-host
export KAFKA_BOOTSTRAP_SERVERS=your-kafka-servers
export ELASTICSEARCH_HOST=your-elasticsearch-host
```

## Testing

### Unit Tests

Run unit tests:

```bash
mvn test
```

### Integration Tests

Run integration tests:

```bash
mvn verify
```

## Troubleshooting

### Common Issues

1. **Database Connection**: Ensure MySQL is running and accessible
2. **Redis Connection**: Verify Redis server is running
3. **Kafka Connection**: Check Kafka and Zookeeper status
4. **Elasticsearch Connection**: Ensure Elasticsearch is running
5. **API Key**: Verify third-party SMS API key is correct

### Logs

Check application logs for detailed error information:

```bash
tail -f logs/application.log
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License. 