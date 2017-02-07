#!/usr/bin/env bash

declare CONTAINER_NAME=$1

# spawn new container
docker run --hostname "$CONTAINER_NAME" \
    --name "$CONTAINER_NAME" \
    --env "KAFKA_ADVERTISED_HOST_NAME=$CONTAINER_NAME" \
    --env "KAFKA_BROKER_ID=-1" \
    --env "KAFKA_ADVERTISED_PORT=9092" \
    --env "KAFKA_ZOOKEEPER_CONNECT=wasp-zookeeper-0001:2181" \
    --net "wasp-network" \
    --detach \
    wurstmeister/kafka:0.8.2.1