#!/usr/bin/env sh

sbt assembly
docker login --username "$DOCKER_USERNAME" --password "$DOCKER_SECRET"
docker build -t catan-ml ./
docker push gherman27/catan-ml:latest