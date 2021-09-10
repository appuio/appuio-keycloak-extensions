# Set Shell to bash, otherwise some targets fail with dash/zsh etc.
SHELL := /bin/bash

# Disable built-in rules
MAKEFLAGS += --no-builtin-rules
MAKEFLAGS += --no-builtin-variables
.SUFFIXES:
.SECONDARY:
.DEFAULT_TARGET: help

IMG_TAG ?= latest
QUAY_IMG ?= quay.io/appuio/appuio-keycloak-extensions:$(IMG_TAG)

maven_suppress_downloads = -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn

.PHONY: help
help: ## Show this help
	@grep -E -h '\s##\s' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = "(: ).*?## "}; {gsub(/\\:/,":", $$1)}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

.PHONY: compile
compile: ## Compile the project
	mvn $(maven_suppress_downloads) $(maven_args) compile

.PHONY: lint
lint: compile ## Ensures formatting
	@echo 'Check for uncommitted changes ...'
	git diff --exit-code

.PHONY: test
test: test\:java ## All-in-one test target

.PHONY: test\:java
test\:java: ## Run unit tests
	mvn $(maven_suppress_downloads) $(maven_args) test

.PHONY: build
build: build.java build.docker ## All-in-one build target

.PHONY: build.java
build.java: ## Build Java assets
	mvn $(maven_suppress_downloads) $(maven_args) package -DskipTests=true

.PHONY: build.docker
build.docker: ## Build the docker image
	docker build -t $(QUAY_IMG) .
