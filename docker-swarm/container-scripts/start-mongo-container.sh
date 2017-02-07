#!/usr/bin/env bash

declare CONTAINER_NAME=$1

# spawn new container
docker run --hostname "$CONTAINER_NAME" \
    --name "$CONTAINER_NAME" \
    --publish "27017:27017" \
    --net "wasp-network" \
    --detach \
    mongo:3.0.2