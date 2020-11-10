#! /bin/bash
set -e

## Helper script to build all images in order

cd ./Dockerfiles

docker build --rm -f Dockerfile.environment -t ghcr.io/ng-cdi/onos-docker:intents-environment .
docker build --rm -f Dockerfile.onos -t ghcr.io/ng-cdi/onos-docker:intents-onos .
docker build --rm -f Dockerfile.imr -t ghcr.io/ng-cdi/onos-docker:intents-app .
docker build --rm -f Dockerfile.demo -t ghcr.io/ng-cdi/onos-docker:intents-demo .

echo "Building Completed"
