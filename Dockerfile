# ---- 1단계: 빌드 (Gradle로 실행용 jar 만들기) ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 의존성 먼저 받아 두기 (소스만 바뀌면 이 단계는 캐시 재사용)
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null 2>&1 || true

# 소스 복사 후 jar 빌드 (테스트는 건너뜀)
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# ---- 2단계: 실행 (JRE만 담아 가볍게) ----
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=Asia/Seoul
# t3.micro(메모리 1GB) 기준. 서버를 키우면 값만 늘리면 됨
ENV JAVA_OPTS="-Xms256m -Xmx512m"

COPY --from=build /app/build/libs/*.jar app.jar
# 로컬 저장 모드(STORAGE_TYPE=local)에서 녹음 파일이 쌓이는 곳
RUN mkdir -p /app/uploads

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
