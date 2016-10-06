#!/usr/bin/env bash

# update /etc/hosts with line to resolve host's hostname to container interface
CONTAINER_IP=$(getent hosts $HOSTNAME | awk '{print $1}')
echo $CONTAINER_IP  $HOST_HOSTNAME >> /etc/hosts

# remove pid file from previous runs
rm RUNNING_PID

# start wasp
exec /opt/wasp/bin/wasp -Dconfig.file=wasp-container.conf