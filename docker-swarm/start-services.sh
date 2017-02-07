#!/usr/bin/env bash

# cd into script dir
SCRIPT_DIR=`dirname $0`
cd $SCRIPT_DIR

./manage-wasp-swarm.sh zookeeper 1

./manage-wasp-swarm.sh kafka 2

./manage-wasp-swarm.sh elastickibana 2

./manage-wasp-swarm.sh spark-master 1

./manage-wasp-swarm.sh spark-worker 2

./manage-wasp-swarm.sh mongo 1

./manage-wasp-swarm.sh wasp 1