#!/usr/bin/env sh

sbt assembly
docker build -t catan-ml ./
docker push gherman27/catan-ml:latest