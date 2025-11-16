SHELL := /bin/bash
INFRA_DIR := tchalanet-infra
ENV ?= dev

.PHONY: help up up-all down down-all up-api print-env doppler-seed
help:
	@echo "Usage: make <target> ENV=dev|staging|prod"
	@echo "Targets: up, up-all, down, down-all, up-api, doppler-seed, print-env"

up:         ; @$(MAKE) -C $(INFRA_DIR) up ENV=$(ENV)
up-all:     ; @$(MAKE) -C $(INFRA_DIR) up-all ENV=$(ENV)
down:       ; @$(MAKE) -C $(INFRA_DIR) down ENV=$(ENV)
down-all:   ; @$(MAKE) -C $(INFRA_DIR) down-all ENV=$(ENV)
up-api:     ; @$(MAKE) -C $(INFRA_DIR) up-api ENV=$(ENV)
doppler-seed: ; @$(MAKE) -C $(INFRA_DIR) doppler-seed ENV=$(ENV)
print-env:  ; @$(MAKE) -C $(INFRA_DIR) print-env ENV=$(ENV)

.PHONY: help up up-all down down-all up-api print-env doppler-seed compose-build
