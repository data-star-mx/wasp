# WASP Docker images

This directory contains everything needed to run WASP and its services, either locally or with the support of a Cloudera cluster.

WASP itself can run inside a Docker container or directly on your machine.

## Services needed by WASP

WASP needs external services to run; this directory contains everything necessary to provide all or part of these services using the provided Docker images.

Broadly speaking, there are two kinds of services which WASP will use while deployed:
- externalizable services, that can be provided and managed by a Cloudera cluster (running CDH 5.7+):
    - ZooKeeper
    - Kafka
    - YARN (as a resource manager for Spark)
    - Solr
- services which need to be deployed separately from the Cloudera cluster:
    - MongoDB
    - ElasticSearch
    - Kibana
    - Banana
    
Regarding Solr/ElasticSearch, only one of these needs to be available; which of Kibana/Banana you will need depends on this.

## Using the scripts

The available scripts cover most use cases; just use these and everything should just work. For the details on how all this works and what the files do, see the next section.

#### Creating the network

The containers used by WASP need a separate network, with a well defined name. To create the network, run the `create-network.sh` script.

#### Building the WASP image

To build the WASP docker image, simply run the `build-wasp-docker-image.sh` script under `docker/wasp-docker-image/`.

#### Running in mode #1: everything on your development machine

Running in this mode means every service runs on your local machine; as such, it is pretty resource intensive.

1. Start the services:

   `all-services-docker.sh`
   
   you can choose which of ElasticSerach+Kibana or Solr+Banana gets started by specifying an optional argument; use "elastic", "solr", "both" to run either one or both.

2. Then, run WASP, either in a Docker container or locally:

   `wasp-container-all-services.sh`
   
   or

   `wasp-host-all-services.sh`
   
   or just by using a custom command, eg `sbt run`.

#### Running in mode #2: using a cluster

Running in this mode takes advantage of services running on a cluster.

You have to provide a cluster-specific configuration file telling WASP where to find the services by setting the WASP_CLUSTER_CONF environment variable to point to a proper configuration file. See `wasp-container-minimal-services.conf.template` and `wasp-host-minimal-services.conf.template` for templates for such a configuration file (respectively, for WASP inside a container or on the host).

You also need to set the HADOOP_CONF_DIR (and/or YARN_CONF_DIR) and HADOOP_USER_NAME environment variables to point to the correct configuration for your cluster.

Additionally, for all this to work properly, the hostname of the machine running the container with WASP inside (or running WASP directly) must be resolvable by the machines of the cluster, because the Spark executors must be able to connect back.

1. Start the services:

   `minimal-services-docker.sh`

2. Then, run WASP, either in a Docker container or locally:

   `wasp-container-minimal-services.sh`
   
   or

   `wasp-host-minimal-services.sh`
   
   or just by using a custom command, eg `sbt run`.

## What the various \*-docker-compose.yml/\*.conf files do

#### The `*-docker-compose.yml` files

There are six `*-docker-compose.yml` files, each with its own purpose.

The first two, `wasp-all-services-docker-compose.yml` and `wasp-minimal-services-docker-compose.yml`, define containers running WASP for the two use cacses outlined above. This is useful if for any reason you don't want to run WASP directly on your machine while developing, and for deployment purposes.

The other four deal with the services needed by WASP:
- `externalizable-services-docker-compose.yml` defines containers for most of the services that substitute for some of those provided by a Cloudera cluster:
    - ZooKeeper
    - Kafka
    - Spark Standalone Master & Worker(s) (instead of YARN)
- `solr-docker-compose.yml` defines a container for Solr 4.10, the last servcie which may be provided by a Cloudera cluster; this is separate from the others to allow switching between ElasticSearch and Solr
- `mongodb-docker-compose.yml` and `elastickibana-docker-compose.yml` define containers for services which are not provided by a Cloudera cluster:
    - MongoDB
    - ElasticSearch
    - Kibana
    
Using just `mongodb-docker-compose.yml` and `elastickibana-docker-compose.yml` and configuring WASP appropriately allows you to offload most of the computational and storage needs of WASP to a cluster.

Using `mongodb-docker-compose.yml`, `elastickibana-docker-compose.yml` and `externalizable-services-docker-compose.yml` together allows you to run everything WASP needs on one machine, to develop even without a cluster avalable.

#### The `*.conf` files

The `*-docker-compose.yml` files above work in conjunction with the similarly-named `*-docker.conf` files, that tell WASP where to look for the services in the corresponding situation. If you're running a custom WASP command instead of using the scripts, you need to pass these conf files (or your modified versions) to WASP through a command-line argument, like this:
`-Dconfig.file=path/to/file.conf`

To use the services running on a Cloudera cluster, you will need to provide an appropriate configuration file telling WASP how to reach them; see `wasp-container-minimal-services.conf.template` and `wasp-host-minimal-services.conf.template` for example config files. The `-Dconfig.file` argument described in the previous paragraph only allows you to specify a single file; use the `include` directive to include other `.conf` files in the one you pass as an argument.

## A note about MongoDB databases

Because WASP keeps all the configuration on the MongoDB database and only reads from the above `.conf` files to initialize it and to know which database to use, the configuration files specify a different database name for each situation; this ensures configurations for different running modes are cleanly separated and isolated from eachother. However, this also means that pipegraphs etc are isolated for each running mode, and changes done while running in a mode (eg, cluster) will not apply to another mode (eg, standalone).