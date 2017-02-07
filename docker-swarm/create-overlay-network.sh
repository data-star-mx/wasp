#!/usr/bin/env bash

# create docker_default overlay network
docker network create --driver=overlay wasp-network
