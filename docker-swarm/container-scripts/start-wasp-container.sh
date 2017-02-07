#!/usr/bin/env bash

declare CONTAINER_NAME=$1

# spawn new container
docker run --hostname "$CONTAINER_NAME" \
    --name "$CONTAINER_NAME" \
    --publish "4040:4040" \
    --publish "7530:9000" \
    --net "wasp-network" \
    --volume "/root/wasp/docker/wasp/wasp-container-all-services.conf:/opt/wasp/wasp-container.conf" \
    --detach \
    agilelab/wasp
