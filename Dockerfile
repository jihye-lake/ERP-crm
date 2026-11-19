# ---- build ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# 의존성 레이어를 먼저 받아두면 소스만 바뀌었을 때 빌드가 훨씬 빨라짐
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q package -DskipTests

# ---- run ----
FROM eclipse-temurin:17-jre
WORKDIR /app

# root로 돌리지 않기
RUN useradd --system --uid 1001 erp
COPY --from=build /workspace/target/erp-crm-*.jar app.jar
USER erp

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
