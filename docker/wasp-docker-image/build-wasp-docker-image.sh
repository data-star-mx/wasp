#!/usr/bin/env bash

# cd into the project root
SCRIPT_DIR=`dirname $0`
cd $SCRIPT_DIR/../..

# create preconditions for image build success, aka prepare dist zip
sbt dist

# cd into build dir
cd docker/wasp-docker-image

# copy dist zip in build dir
cp ../../target/universal/wasp.zip .

# build image
sudo docker build -t agilelab/wasp .

# cleanup
rm wasp.zip