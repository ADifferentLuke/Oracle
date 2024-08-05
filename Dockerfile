FROM openjdk:18
ARG JAR_FILE
COPY target/${JAR_FILE} /usr/local/oracle/oracle.jar
WORKDIR /usr/local/oracle/
EXPOSE 8080
ENTRYPOINT ["java","-jar","oracle.jar"]
