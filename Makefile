IFWD_VERSION=1.9.0

REGISTRY=ghcr.io
ORG=ng-cdi
BAZEL_VERSION=1.2.1
MAVEN_VERSION=3.3.9
UBUNTU_VERSION=bionic
IMAGEBASE=${REGISTRY}/${ORG}
VERSION=latest

.PHONY: build-environment build-ifwd build-demo build-cli build

build-environment:
	@docker build --pull --rm -f Dockerfile.environment --build-arg UBUNTU_VERSION=${UBUNTU_VERSION} --build-arg BAZEL_VERSION=${BAZEL_VERSION} --build-arg MAVEN_VERSION=${MAVEN_VERSION}  -t ${IMAGEBASE}/onos:env-${VERSION} .

build-ifwd: build-environment
	@docker build --rm -f Dockerfile.imr --build-arg ENV_VERSION=${VERSION} -t ${IMAGEBASE}/onos-ifwd:${IFWD_VERSION} .

build-demo: build-ifwd
	@docker build --rm -f Dockerfile.demo --build-arg ENV_VERSION=${VERSION} -t ${IMAGEBASE}/demo-onos:${VERSION} .

build-cli: build-ifwd
	@docker build --rm -f Dockerfile.cli -t ${IMAGEBASE}/onos-cli:${VERSION} .

build: build-environment build-ifwd build-demo build-cli

push:
	@docker push ${IMAGEBASE}/onos:${VERSION}
	@docker push ${IMAGEBASE}/onos-ifwd:${IFWD_VERSION}
	@docker push ${IMAGEBASE}/demo-onos:${VERSION}
	@docker push ${IMAGEBASE}/onos-cli:${VERSION}

all: build push
