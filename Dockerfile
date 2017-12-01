FROM openjdk:8

COPY target/universal/tapi-application-*.tgz .
COPY start-docker.sh .
RUN chmod +x start-docker.sh
RUN tar xvf tapi-application-*.tgz

EXPOSE 7020