# ==========================================
# Stage 1: Build Stage
# ==========================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy Maven descriptor and fetch dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy project source code and build production artifact
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# Stage 2: Runtime Stage
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root system user for security best practices
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy compiled JAR from build stage
COPY --from=build /app/target/shopsphere-0.0.1-SNAPSHOT.jar app.jar

# Set ownership to non-root user
RUN chown appuser:appgroup app.jar
USER appuser

# Expose Spring Boot server port
EXPOSE 8081

# Launch application
ENTRYPOINT ["java", "-jar", "app.jar"]
