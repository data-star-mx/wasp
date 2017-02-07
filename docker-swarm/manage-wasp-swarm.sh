#!/usr/bin/env bash

# cd into script dir
SCRIPT_DIR=`dirname $0`
cd $SCRIPT_DIR

# import functions
. functions.sh

# parse args, act accordingly
scale_service $1 $2