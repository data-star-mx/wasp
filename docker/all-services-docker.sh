#!/usr/bin/env bash

case $1 in
"elastic")
    INDEXED_DATASTORE_SERVICE="-f elastickibana-docker-compose.yml"
    ;;
"solr")
    INDEXED_DATASTORE_SERVICE="-f solrcloud-docker-compose.yml"
    ;;
"both")
    INDEXED_DATASTORE_SERVICE="-f elastickibana-docker-compose.yml -f solrcloud-docker-compose.yml"
    ;;
*)
    echo "Invalid or missing indexed datastore service argument (valid: \"elastic\", \"solr\", \"both\"); using elasticsearch."
    INDEXED_DATASTORE_SERVICE="-f elastickibana-docker-compose.yml"
    ;;
esac

sudo docker-compose -f externalizable-services-docker-compose.yml -f mongodb-docker-compose.yml $INDEXED_DATASTORE_SERVICE up