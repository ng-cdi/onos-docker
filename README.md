# ONOS Docker 🐳

This repository simply allows ONOS v1.15 to be built into a Docker container. You should not be using this for generic use of ONOS as it is outdated. This is designed for the NG-CDI intent based networking demo using the ONOS Intent Monitor and Reroute application found [here](https://github.com/ANTLab-polimi/onos-opa-example).

## Repo Contents

The 4 Dockerfiles here are separated solely for easing maintainability of the project. They each build atop of each other, so must be tagged correctly when building!

The layer model is: environment -> (onos / imr) -> demo

- `Dockerfile.environment` defines a container that simply houses the base build environment for the project. This consists of the apt package dependencies along with appropriate maven and bazel versions for the ng-cdi intents demo.
  ```bash
  docker build --rm -f Dockerfile.environment -t ghcr.io/ng-cdi/onos-docker:intents-environment .
  ```
  > The bazel install is at the path `/root/.bazel/bin/` within this built image
  <br>
  > The maven install is at the path `/usr/local/apache-maven-<MAVEN_VERSION>/bin` within this built image

  The maven and bazel versions can be set in the arguments `MAVEN_VERSION` and `BAZEL_VERSION` respectively


- `Dockerfile.onos` defines a container image that houses a built copy of ONOS v1.15. More recent versions of ONOS exist, so don't use this unless you have too.
  > 🛑 Don't build this yourself unless you have lots of RAM, CPUs, storage, and time...
  ```bash
  docker build --rm -f Dockerfile.onos -t ghcr.io/ng-cdi/onos-docker:intents-onos .
  ```


- `Dockerfile.imr` defines a container image that has the IFWD application, an app that requires the IMR service.
  ```bash
  docker build --rm -f Dockerfile.imr -t ghcr.io/ng-cdi/onos-docker:intents-app .
  ```


- `Dockerfile.demo` defines a container image that when instantiated, will run ONOS and install the the application via the ONOS REST API.
  ```bash
  docker build --rm -f Dockerfile.demo -t ghcr.io/ng-cdi/onos-docker:intents-demo .
  ```

To simply build all of this in order, just run the included `build-all.sh` bash script.

## Run

To run this, simply use the `intents-demo` tag:
```bash
docker run --rm -it -p 8101:8101 -p 8181:8181 -p 6653:6653 -p 6640:6640 -p 9876:9876 ghcr.io/ng-cdi/onos-docker:intents-demo
```

## Contributors

 - [Ellie Davies](https://github.com/mavi0)
 - [Will Fantom](https://github.com/willfantom)
