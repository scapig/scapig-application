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

## Publishing
``
docker tag scapig-application scapig/scapig-application:VERSION
docker login
docker push scapig/scapig-application:VERSION
``

## Running
``
docker run -p9012:9012 -d scapig/scapig-application:VERSION
``
