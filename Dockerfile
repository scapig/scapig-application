FROM openjdk:8

COPY target/universal/scapig-application-*.tgz .
COPY start-docker.sh .
RUN chmod +x start-docker.sh
RUN tar xvf scapig-application-*.tgz

EXPOSE 7020