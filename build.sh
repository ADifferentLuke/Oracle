#!/bin/bash

####
# Cloud Build will eventually replace this
#

JAR=`ls target/ | grep jar | grep -v original`
VERSION=v0.1

echo "docker build -f ./Dockerfile --build-arg=\"JAR_FILE=$JAR\" -t 'net.lukemcomber/oracle:'$VERSION ."
docker build -f ./Dockerfile --build-arg="JAR_FILE=$JAR" -t 'net.lukemcomber/oracle:'$VERSION .

docker run -p 8080:8080 'net.lukemcomber/oracle:'$VERSION
