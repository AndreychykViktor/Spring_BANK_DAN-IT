# Multi-stage build для оптимізації розміру образу
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Копіюємо pom.xml та завантажуємо залежності (кешування)
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B

# Копіюємо вихідний код та збираємо додаток
COPY backend/src ./src
RUN mvn clean package -DskipTests

# Production образ - використовуємо стандартний образ замість alpine для кращої сумісності
FROM eclipse-temurin:17-jre
WORKDIR /app

# Встановлюємо curl для health check
RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

# Створюємо користувача для безпеки
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Копіюємо JAR файл з build стадії
COPY --from=build /app/target/*.jar app.jar

# Відкриваємо порт
EXPOSE 9000

# Health check (використовуємо curl замість wget для кращої сумісності)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:9000/api/test/health || exit 1

# Запускаємо додаток
ENTRYPOINT ["java", "-jar", "app.jar"]

