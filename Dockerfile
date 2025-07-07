# Etapa de build
FROM gradle:8.6.0-jdk21 AS build

WORKDIR /app
COPY . .

# Gera o .jar usando bootJar (usado com Spring Boot plugin)
RUN gradle bootJar --no-daemon

# Etapa de execução (imagem mais leve)
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Copia o .jar gerado para a imagem final
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8089

# Executa a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]
