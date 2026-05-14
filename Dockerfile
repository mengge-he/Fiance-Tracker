# Build stage
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src/
RUN mvn -B -q -DskipTests package

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=builder /build/target/finance-tracker-*.jar app.jar
USER spring:spring
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["sh", "-c", "if [ -n \"$DATABASE_URL\" ] && [ -z \"$SPRING_DATASOURCE_URL\" ]; then export SPRING_DATASOURCE_URL=\"jdbc:${DATABASE_URL}\"; fi; exec java -jar /app/app.jar"]
