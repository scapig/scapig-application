#!/bin/sh
sbt universal:package-zip-tarball
docker build -t scapig-application .
docker tag scapig-application scapig/scapig-application
docker push scapig/scapig-application
