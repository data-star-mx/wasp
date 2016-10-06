#!/usr/bin/env bash
#  vim:ts=4:sts=4:sw=4:et
#
#  Author: Hari Sekhon
#  Date: 2016-04-24 21:29:46 +0100 (Sun, 24 Apr 2016)
#
#  https://github.com/harisekhon/Dockerfiles/solrcloud
#
#  License: see accompanying Hari Sekhon LICENSE file
#
#  If you're using my code you're welcome to connect with me on LinkedIn and optionally send me feedback
#
#  https://www.linkedin.com/in/harisekhon
#

set -euo pipefail
[ -n "${DEBUG:-}" ] && set -x

srcdir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

export JAVA_HOME="${JAVA_HOME:-/usr}"

export SOLR_HOME="/solr"

#tree -d "$SOLR_HOME/example/solr/collection1/"

# solr -e cloud fails if not called from $SOLR_HOME
cd "$SOLR_HOME"

if [ $# -gt 0 ]; then
    exec $@
else
    # exits with 141 for pipefail breaking yes stdout
    set +o pipefail
    solr -e cloud -noprompt

    unzip -o /solr/example/webapps/solr.war -d /solr/example/solr-webapp/webapp
    cd /solr/example/solr/collection1/
    unzip -o /solr/example/solr/collection1/waspConfigSet.zip
    /solr/example/scripts/cloud-scripts/zkcli.sh -zkhost localhost:9983 -cmd upconfig -confdir /solr/example/solr/collection1/waspConfigSet/ -confname waspConfigSet

    # TODO connect to the zookeeper on 2118.
    #/solr/example/scripts/cloud-scripts/zkcli.sh -zkhost localhost:2118 -cmd upconfig -confdir /solr/example/solr/collection1/waspConfigSet/ -confname waspConfigSet

    wget "http://localhost:8983/solr/admin/collections?action=DELETE&name=collection1"

    cd "$SOLR_HOME"

    #while (true) ; do sleep 10; done;
    if ls -d "$SOLR_HOME"-4* &>/dev/null; then
        tail -f "$SOLR_HOME/"node*/logs/*
    else
        tail -f "$SOLR_HOME/example/cloud/"node*/logs/*
   fi
fi