# Docker Environment Setup for Brev Application

This document explains how to set up and run the Brev URL shortener application using Docker Compose.

## Prerequisites

- Docker Desktop installed and running
- Docker Compose (included with Docker Desktop)
- At least 2GB of available RAM
- Ports 3306, 6379, 8080, 8081, 8082 available on your machine

## Quick Start

1. **Start the entire environment:**
   ```bash
   docker-compose up -d
   ```

2. **Access the application:**
   - Application: http://localhost:8080
   - Health Check: http://localhost:8080/actuator/health
   - API Documentation: http://localhost:8080/api/v1/urls

3. **Stop the environment:**
   ```bash
   docker-compose down
   ```

## Services Overview

### 🚀 Brev Application (Port 8080)
- Spring Boot application with URL shortening functionality
- Automatically connects to MySQL and Redis
- Health checks enabled
- Logs stored in `./logs` directory

### 🗄️ MySQL Database (Port 3306)
- MySQL 8.0 with persistent storage
- Database: `brev`
- User: `brevuser` / Password: `brevpassword`
- Root Password: `rootpassword`
- Initialization script: `./docker/mysql/init.sql`

### ⚡ Redis Cache (Port 6379)
- Redis 7 Alpine with persistent storage
- Custom configuration: `./docker/redis/redis.conf`
- No password (development setup)
- Memory limit: 256MB

## Management Tools (Optional)

Start with management tools:
```bash
docker-compose --profile tools up -d
```

### 🔧 Redis Commander (Port 8081)
- Web-based Redis management interface
- URL: http://localhost:8081

### 🔧 phpMyAdmin (Port 8082)
- Web-based MySQL management interface
- URL: http://localhost:8082
- Server: mysql
- Username: brevuser / Password: brevpassword

## Common Commands

### Development Workflow
```bash
# Start only the databases (for local development)
docker-compose up -d mysql redis

# Rebuild and restart the application
docker-compose up --build brev-app

# View logs
docker-compose logs -f brev-app
docker-compose logs -f mysql
docker-compose logs -f redis

# Check service status
docker-compose ps

# Execute commands in containers
docker-compose exec mysql mysql -u brevuser -pbrevpassword brev
docker-compose exec redis redis-cli
docker-compose exec brev-app sh
```

### Data Management
```bash
# Backup database
docker-compose exec mysql mysqldump -u brevuser -pbrevpassword brev > backup.sql

# Restore database
docker-compose exec -T mysql mysql -u brevuser -pbrevpassword brev < backup.sql

# Clear all data and restart
docker-compose down -v
docker-compose up -d
```

## Environment Variables

You can override default settings by creating a `.env` file:

```env
# Database settings
MYSQL_ROOT_PASSWORD=your-root-password
MYSQL_PASSWORD=your-app-password

# Application settings
APP_BASE_URL=http://localhost:8080
APP_CACHE_TTL=7200

# JVM settings
JAVA_OPTS=-Xmx1024m -Xms512m
```

## Troubleshooting

### Application won't start
1. Check if all services are healthy: `docker-compose ps`
2. View application logs: `docker-compose logs brev-app`
3. Verify database connection: `docker-compose exec mysql mysql -u brevuser -pbrevpassword -e "SHOW DATABASES;"`

### Performance issues
1. Increase memory limits in docker-compose.yml
2. Check available system resources: `docker stats`
3. Review application logs for errors

### Port conflicts
If you get port conflicts, modify the ports in docker-compose.yml:
```yaml
ports:
  - "8081:8080"  # Change host port from 8080 to 8081
```

## File Structure
```
project-root/
├── docker-compose.yml
├── Dockerfile
├── docker/
│   ├── mysql/
│   │   └── init.sql
│   └── redis/
│       └── redis.conf
└── logs/
    └── (application logs)
```

## Production Considerations

For production deployment:
1. Use environment-specific configuration files
2. Enable Redis authentication
3. Use secrets management for passwords
4. Configure proper resource limits
5. Set up monitoring and alerting
6. Use external volumes for data persistence
