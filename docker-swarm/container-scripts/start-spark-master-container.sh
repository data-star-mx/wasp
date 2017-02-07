#!/usr/bin/env bash

declare CONTAINER_NAME=$1

# spawn new container
docker run --hostname "$CONTAINER_NAME" \
    --name "$CONTAINER_NAME" \
    --publish "7077:7077" \
    --net "wasp-network" \
    --detach \
    gettyimages/spark:1.6.2-hadoop-2.6 \
    bin/spark-class org.apache.spark.deploy.master.Master -h $CONTAINER_NAME