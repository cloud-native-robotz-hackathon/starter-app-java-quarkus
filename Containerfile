## Stage 1: Build stage
FROM registry.access.redhat.com/ubi9/openjdk-17:1.20 AS build

# Install Maven
USER root
RUN microdnf install -y maven && microdnf clean all

USER default

# Set working directory
WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

## Stage 2: Runtime stage
FROM registry.access.redhat.com/ubi9/openjdk-17-runtime:1.20

ENV LANGUAGE='en_US:en'

# Create app directory
WORKDIR /app

# Copy the JAR file from build stage
COPY --from=build /app/target/quarkus-app/lib/ /app/lib/
COPY --from=build /app/target/quarkus-app/*.jar /app/
COPY --from=build /app/target/quarkus-app/app/ /app/app/
COPY --from=build /app/target/quarkus-app/quarkus/ /app/quarkus/

# Create static directory for images
RUN mkdir -p /app/static

EXPOSE 8080
USER 185

ENV JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/app/quarkus-run.jar"

ENTRYPOINT [ "/opt/jboss/container/java/run/run-java.sh" ]
