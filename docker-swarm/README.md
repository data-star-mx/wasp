# WASP Docker Swarm deployment

These instructions describe how to deploy WASP using Docker Swarm - note that **it uses "old-style" Swarm standalone**, a separate product from Docker, **not the "new-style" Swarmkit/swarm mode** built into Docker engine >=1.12.

See [here](https://github.com/docker/swarm#swarm-disambiguation) for a more extensive disambiguation of the two systems.

To set up a Docker Swarm cluster, see the [official documentation](https://docs.docker.com/swarm/).

These scripts exist mostly to work around the underscore being used as a separator in container names by Compose, making the generated container names/hostnames not valid URLs.  See [here](https://github.com/docker/compose/issues/229) for a discussion of the problem.

#HOWTO

TODO :)
