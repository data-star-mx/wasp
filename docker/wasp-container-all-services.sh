#!/usr/bin/env bash

# cd into the docker dir
SCRIPT_DIR=`dirname $0`
cd $SCRIPT_DIR

# run container
sudo docker-compose -f wasp-all-services-docker-compose.yml up