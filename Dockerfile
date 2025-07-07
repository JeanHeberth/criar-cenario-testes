# Etapa 1: Build com ./gradlew
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copia arquivos do projeto (incluindo o wrapper)
COPY . .

# Dá permissão de execução ao gradlew
RUN chmod +x ./gradlew

# Executa build (gera .jar)
RUN gradle bootJar --no-daemon

# Etapa 2: imagem final
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# Copia o jar gerado da etapa anterior
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8089
ENTRYPOINT ["java", "-Djna.library.path=/usr/lib/x86_64-linux-gnu/", "-jar", "app.jar"]