#!/usr/bin/env bash

# cd into the project root
SCRIPT_DIR=`dirname $0`
cd $SCRIPT_DIR/..

# check that a configuration for running in this mode is available
if [[ -z "$WASP_CLUSTER_CONF" ]]; then
    echo "Please set WASP_CLUSTER_CONF to point to a proper configuration file for running in this mode."
    exit 1
fi

# run stage
sbt stage

# run wasp
./target/universal/stage/bin/wasp -Dconfig.file=$WASP_CLUSTER_CONF