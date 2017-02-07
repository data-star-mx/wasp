#!/usr/bin/env bash

# generates container name from service $1 and  number $2
function generate_container_name {
    declare SERVICE_NAME=$1
    declare -i CONTAINER_NUMBER=$2

    # pad number to 4 digits with leading zeroes
    declare PADDED_NUMBER=$(printf "%04d" $CONTAINER_NUMBER)

    echo "wasp-$SERVICE_NAME-$PADDED_NUMBER"
}

# gets the running containers for the service $1
function get_running_service_containers {
    declare SERVICE_NAME=$1

    declare RUNNING_CONTAINERS=$(docker ps --format "{{.Names}}\t{{.ID}}" |
        grep "[a-zA-Z0-9\.\\]*wasp-$SERVICE_NAME-[0-9]\{4\}") # regex matches names like */wasp-service-0000

    echo "$RUNNING_CONTAINERS"
}

# gets the stopped containers for the service $1
function get_stopped_service_containers {
    declare SERVICE_NAME=$1

    declare STOPPED_CONTAINERS=$(docker ps -a --format "{{.Names}}\t{{.ID}}\t{{.Status}}" |
        grep "[a-zA-Z0-9\.\\]*wasp-$SERVICE_NAME-[0-9]\{4\}" | # regex matches names like */wasp-service-0000
        grep "Exited" | # filter stopped containers
        awk '{ print $1 "\t" $2 }') # throw away status field

    echo "$STOPPED_CONTAINERS"
}

# gets the highest container number for the service $1
function get_highest_container_number_for_service {
    declare SERVICE_NAME=$1

    declare ALL_CONTAINERS=$(docker ps -a --format "{{.Names}}" |
        grep "[a-zA-Z0-9\.\\]*wasp-$SERVICE_NAME-[0-9]\{4\}") # regex matches names like */wasp-service-0000
    declare HIGHEST_NUMBER_CONTAINER=$(echo "$ALL_CONTAINERS" |
        sort -t '/' -k 2 | # split on / names like */wasp-service-0000, sort only on the wasp-service-0000 part
        tail -n 1) # get the last one
    declare HIGHEST_NUMBER=$(get_container_number $HIGHEST_NUMBER_CONTAINER) # parse container name, get number

    echo "$HIGHEST_NUMBER"
}

# parse the container name $1 and return the number
function get_container_number {
    declare NAME=$1

    # split on dash
    declare -a NAME_ARRAY
    IFS='-' read -ra NAME_ARRAY <<< "$NAME"

    # return last element
    echo ${NAME_ARRAY[*]: -1}
}

# scale up/down service $1 to $2 instances
function scale_service {
    declare SERVICE_NAME=$1
    declare -i DESIRED_INSTANCES=$2

    # get containers for service
    SERVICE_CONTAINERS="$(get_running_service_containers $SERVICE_NAME)"

    # find number of running containers
    # if SERVICE_CONTAINERS is empty, there are no running instances, otherwise count the lines
    # (workaround for avoiding counting a single line without newline at the end)
    declare -i NUM_RUNNING_INSTANCES
    if [[ -z "$SERVICE_CONTAINERS" ]]; then
        NUM_RUNNING_INSTANCES=0
    else
        NUM_RUNNING_INSTANCES=$(echo "$SERVICE_CONTAINERS" |
            wc | # count lines
            awk '{ print $1 }') # get first field (line count)
    fi

    echo "Detected $NUM_RUNNING_INSTANCES running instances for service $SERVICE_NAME."
    echo "Scaling to $DESIRED_INSTANCES instances."

    # scale up if needed
    while [ $NUM_RUNNING_INSTANCES -lt $DESIRED_INSTANCES ]; do
        echo "Starting a container for service $SERVICE_NAME..."
        start_container $SERVICE_NAME
        NUM_RUNNING_INSTANCES=$((NUM_RUNNING_INSTANCES+1)) # within double parenthesis, parameter dereferencing is optional
    done
    # scale down if needed
    while [ $NUM_RUNNING_INSTANCES -gt $DESIRED_INSTANCES ]; do
        echo "Stopping a container for service $SERVICE_NAME..."
        stop_container $SERVICE_NAME
        NUM_RUNNING_INSTANCES=$((NUM_RUNNING_INSTANCES-1)) # within double parenthesis, parameter dereferencing is optional
    done
}

# start a container for service $1
function start_container {
    declare SERVICE_NAME=$1

    # find if there are any stopped containers for this service
    STOPPED_CONTAINERS=$(get_stopped_service_containers $SERVICE_NAME)

    # sort by container name, get the first (oldest) one's id
    WINNER_CONTAINER=$(echo "$STOPPED_CONTAINERS" |
        sort -t '/' -k 2 | # split on / names like */wasp-service-0000, sort only on the wasp-service-0000 part
        head -1) # get first one
    WINNER_NAME=$(echo "$WINNER_CONTAINER" | awk '{ print $1 }')
    WINNER_ID=$(echo "$WINNER_CONTAINER" | awk '{ print $2 }')

    # restart stopped container or start new one if none exists
    if [[ -z "$WINNER_ID" ]]; then # no stopped container; start new container
        # generate number for new container
        declare HIGHEST_NUMBER=$(get_highest_container_number_for_service $SERVICE_NAME)
        declare NEW_NUMBER=$((10#$HIGHEST_NUMBER+1)) # 10# is needed because otherwise leading zeroes cause numbers to read as octal

        # generate padded container number new container
        declare NEW_NAME=$(generate_container_name $SERVICE_NAME $NEW_NUMBER)

        # invoke start script
        echo "Starting new container $NEW_NAME..."
        ./container-scripts/start-$SERVICE_NAME-container.sh $NEW_NAME
    else # stopped container found; start it
        echo "Restarting stopped container $WINNER_NAME ($WINNER_ID)..."
        docker start $WINNER_ID
    fi
}

# stop a container for service $1
function stop_container {
    declare SERVICE_NAME=$1

    # get containers for service
    RUNNING_CONTAINERS=$(get_running_service_containers $SERVICE_NAME)

    # sort by container name, get the last (youngest) one's id
    VICTIM_CONTAINER=$(echo "$RUNNING_CONTAINERS" |
        sort -t '/' -k 2 | # split on / names like */wasp-service-0000, sort only on the wasp-service-0000 part
        tail -n 1) # get last one
    VICTIM_NAME=$(echo "$VICTIM_CONTAINER" | awk '{ print $1 }')
    VICTIM_ID=$(echo "$VICTIM_CONTAINER" | awk '{ print $2 }')

    # stop it
    echo "Stopping victim container $VICTIM_NAME ($VICTIM_ID)..."
    docker stop $VICTIM_ID
}