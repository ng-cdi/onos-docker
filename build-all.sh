#! /bin/bash
set -e

## Helper script to build all images in order

docker build --rm -f Dockerfiles/Dockerfile.environment -t ghcr.io/ng-cdi/onos-docker:intents-environment .
docker build --rm -f Dockerfiles/Dockerfile.onos -t ghcr.io/ng-cdi/onos-docker:intents-onos .
docker build --rm -f Dockerfiles/Dockerfile.imr -t ghcr.io/ng-cdi/onos-docker:intents-app .
docker build --rm -f Dockerfiles/Dockerfile.demo -t ghcr.io/ng-cdi/onos-docker:intents-demo .
docker build --rm -f Dockerfiles/Dockerfile.cli -t ghcr.io/ng-cdi/onos-docker:intents-cli .

echo "Building Completed"
