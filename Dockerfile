# Use a lightweight JRE for the runtime
FROM eclipse-temurin:21-jre-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the pre-built JAR from your local build folder into the container
# Adjust "build/libs/*.jar" to the actual path where your JAR is generated
COPY build/libs/*.jar app.jar

# Add a non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Expose the port (usually 8080 for Ktor/Spring)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]