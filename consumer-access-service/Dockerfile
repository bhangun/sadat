FROM quay.io/quarkus/quarkus-micro-image:2.0

WORKDIR /work
COPY target/quarkus-app/ /work/

EXPOSE 8081

CMD ["java", "-jar", "/work/quarkus-run.jar"]
