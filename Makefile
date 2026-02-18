.PHONY: help infra-up infra-down app-up app-down up down restart ps logs build clean

help:
	@echo "Usage: make [target]"
	@echo ""
	@echo "Targets:"
	@echo "  help        Show this help message"
	@echo "  infra-up    Start infrastructure (DB, Redis, Kafka, etc.)"
	@echo "  infra-down  Stop infrastructure"
	@echo "  app-up      Build and start application services (detached)"
	@echo "  app-down    Stop application services"
	@echo "  up          Start everything (Infrastructure + Applications)"
	@echo "  down        Stop everything"
	@echo "  restart     Restart everything"
	@echo "  ps          Show status of all containers"
	@echo "  logs        View logs (usage: make logs [service=<service_name>])"
	@echo "  build       Build application services"
	@echo "  clean       Stop everything and remove volumes"

infra-up:
	docker compose -f infra/docker-compose.yml up -d

infra-down:
	docker compose -f infra/docker-compose.yml down

app-up:
	docker compose up -d --build

app-down:
	docker compose down

up: infra-up app-up

down: app-down infra-down

restart: down up

ps:
	@echo "--- Infrastructure ---"
	@docker compose -f infra/docker-compose.yml ps
	@echo ""
	@echo "--- Applications ---"
	@docker compose ps

logs:
ifdef service
	docker compose logs -f $(service)
else
	docker compose logs -f
endif

build:
	docker compose build

clean:
	docker compose down -v
	docker compose -f infra/docker-compose.yml down -v
