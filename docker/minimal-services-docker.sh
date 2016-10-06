#!/usr/bin/env bash

case $1 in
"elastic")
    INDEXED_DATASTORE_SERVICE="-f elastickibana-docker-compose.yml"
    ;;
"solr")
    INDEXED_DATASTORE_SERVICE="-f solr-docker-compose.yml"
    ;;
"both")
    INDEXED_DATASTORE_SERVICE="-f elastickibana-docker-compose.yml -f solr-docker-compose.yml"
    ;;
*)
    echo "Invalid or missing indexed datastore service argument (valid: \"elastic\", \"solr\", \"both\"); using elasticsearch."
    INDEXED_DATASTORE_SERVICE="-f elastickibana-docker-compose.yml"
    ;;
esac

sudo docker-compose -f mongodb-docker-compose.yml $INDEXED_DATASTORE_SERVICE up