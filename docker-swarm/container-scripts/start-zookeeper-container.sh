#!/usr/bin/env bash

declare CONTAINER_NAME=$1

# spawn new container
docker run --hostname "$CONTAINER_NAME" \
    --name "$CONTAINER_NAME" \
    --net "wasp-network" \
    --detach \
    wurstmeister/zookeeper:3.4.6