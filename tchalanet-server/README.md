
local setup
docker compose --env-file ./envs/dev/.env \
-f docker-compose.yml -f docker-compose-dev.yml up -d --build
