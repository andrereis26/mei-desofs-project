FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN apk add --no-cache --upgrade 'libcrypto3>=3.5.7-r0' 'libssl3>=3.5.7-r0' 'openssl>=3.5.7-r0' \
    && addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/storage && chown appuser:appgroup /app/storage
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
