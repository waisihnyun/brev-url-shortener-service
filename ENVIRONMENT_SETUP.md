# Environment Configuration Guide

## Setting up your local development environment

### Option 1: Using application-local.properties (Recommended for development)

1. The `application-local.properties` file has been created with your local database credentials
2. To use it, run your application with the `local` profile:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```
   Or set the environment variable:
   ```bash
   export SPRING_PROFILES_ACTIVE=local
   ```

### Option 2: Using environment variables

1. Copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   ```

2. Edit `.env` file with your actual database credentials:
   ```
   DB_PASSWORD=your_actual_password
   DB_USERNAME=your_actual_username
   ```

3. Load the environment variables before running the application

### Option 3: Using IDE environment variables

Set the following environment variables in your IDE run configuration:
- `DB_PASSWORD=your_password`
- `DB_USERNAME=your_username`
- `DB_URL=jdbc:mysql://localhost:3306/brev_db`

## Production Deployment

For production, set these environment variables in your deployment platform:
- `DB_URL`
- `DB_USERNAME` 
- `DB_PASSWORD`
- `JPA_DDL_AUTO=validate` (recommended for production)
- `JPA_SHOW_SQL=false` (recommended for production)

## Security Notes

- Never commit files containing actual passwords
- The main `application.properties` only contains environment variable placeholders
- Sensitive files are excluded in `.gitignore`
- Use your platform's secret management for production deployments
