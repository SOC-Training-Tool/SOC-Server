#!/usr/bin/env sh

sbt assembly
docker build -t catan ./
docker push gherman27/catan-ml:catan