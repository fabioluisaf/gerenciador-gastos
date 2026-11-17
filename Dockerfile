# syntax=docker/dockerfile:1

# ---------- Build ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Copia primeiro apenas o necessário para resolver dependências (aproveita cache de camadas)
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q package -DskipTests \
    && cp target/*.jar app.jar \
    && java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# ---------- Runtime ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring

# Camadas do Spring Boot, da que menos muda para a que mais muda
COPY --from=build /app/extracted/dependencies/ ./
COPY --from=build /app/extracted/spring-boot-loader/ ./
COPY --from=build /app/extracted/snapshot-dependencies/ ./
COPY --from=build /app/extracted/application/ ./

USER spring:spring

ENV PORTA_APP=8080 \
    JAVA_OPTS=""

EXPOSE 8080

# Variáveis obrigatórias em tempo de execução: DB_URL, DB_USERNAME, DB_PASSWORD
# Perfil: SPRING_PROFILES_ACTIVE=dev ou prod (sem perfil valem os padrões seguros)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
