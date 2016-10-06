#!/usr/bin/env bash

# cd into the project root
SCRIPT_DIR=`dirname $0`
cd $SCRIPT_DIR/..

# run stage
sbt stage

# run wasp
./target/universal/stage/bin/wasp -Dconfig.file=docker/wasp-host-all-services.conf