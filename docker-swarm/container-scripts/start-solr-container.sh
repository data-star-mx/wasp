#!/usr/bin/env bash

declare CONTAINER_NAME=$1

# spawn new container
docker run --hostname "$CONTAINER_NAME" \
    --name "$CONTAINER_NAME" \
    --publish "8983:8983" \
    --publish "7574:7574" \
    --publish "9983:9983" \
    --net "wasp-network" \
    --detach \
    agilelab/solrcloud