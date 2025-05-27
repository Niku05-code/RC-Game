# Folosește o imagine oficială OpenJDK ca bază
FROM openjdk:17-jdk-slim

# Setează directorul de lucru în container
WORKDIR /app

# Copiază TOATE fișierele proiectului în container (inclusiv src/, out/, etc.)
COPY . .

# Compilează toate fișierele Java
RUN find src -name '*.java' > sources.txt && \
    javac @sources.txt -d out

# Pornește serverul la rularea containerului
CMD ["java", "-cp", "out", "game.server.GameServer"]