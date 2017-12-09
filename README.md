## scapig-application

## Building
``
sbt clean test it:test component:test
``

## Packaging
``
sbt universal:package-zip-tarball
docker build -t scapig-application .
``

## Running
``
docker run -p7020:7020 -i -a stdin -a stdout -a stderr scapig-application sh start-docker.sh
``