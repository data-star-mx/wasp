#!/usr/bin/env bash

# cd into the docker dir
SCRIPT_DIR=`dirname $0`
cd $SCRIPT_DIR

# check that a configuration for running in this mode is available
if [[ -z "$WASP_CLUSTER_CONF" ]]; then
    echo "Please set WASP_CLUSTER_CONF to point to a proper configuration file for running in this mode."
    exit 1
fi

# check that a hadoop configuration & username are available, for YARN
if [[ -z "$HADOOP_CONF_DIR" ]] || [[ -z "$HADOOP_USER_NAME" ]] ; then
    echo "Please set HADOOP_CONF_DIR and HADOOP_USER_NAME to proper values for your deployment."
    exit 1
fi

# export host's hostname for use inside the container
export HOST_HOSTNAME=`hostname`

# run container
sudo -E docker-compose -f wasp-minimal-services-docker-compose.yml up