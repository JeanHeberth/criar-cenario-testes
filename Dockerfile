# Dockerfile (na raiz)
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app
COPY . .

EXPOSE 8089

CMD ["java", "-jar", "build/libs/criar-cenario-testes-0.0.1-SNAPSHOT.jar"]
