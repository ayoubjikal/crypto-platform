FROM maven:3.8.5-openjdk-11-slim AS build

WORKDIR /app

# Copy pom.xml first for dependency resolution and caching
COPY pom.xml .
RUN mvn clean package -DskipTests && ls -l target

# Copy source code
COPY src ./src

# Build the application
RUN mvn package -DskipTests

# Runtime stage
FROM openjdk:11-jre-slim

WORKDIR /app

# Copy built JAR file from build stage
COPY --from=build /app/target/*.jar app.jar

# Add a volume for the data directory
VOLUME /data

# Expose the port
EXPOSE 8080

# Startup command
ENTRYPOINT ["java", "-jar", "/app/app.jar"]