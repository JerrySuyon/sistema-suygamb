FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY . .
RUN chmod +x mvnw && ./mvnw clean package -DskipTests
EXPOSE 10000
CMD ["java", "-jar", "target/sistema-suygamb-0.0.1-SNAPSHOT.jar", "--server.port=10000"]
