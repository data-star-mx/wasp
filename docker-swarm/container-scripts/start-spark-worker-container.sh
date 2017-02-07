#!/usr/bin/env bash

declare CONTAINER_NAME=$1

# spawn new container
docker run --hostname "$CONTAINER_NAME" \
    --name "$CONTAINER_NAME" \
    --env "SPARK_WORKER_CORES=4" \
    --env "SPARK_WORKER_MEMORY=4g" \
    --net "wasp-network" \
    --detach \
    gettyimages/spark:1.6.2-hadoop-2.6 \
    bin/spark-class org.apache.spark.deploy.worker.Worker spark://wasp-spark-master-0000:7077