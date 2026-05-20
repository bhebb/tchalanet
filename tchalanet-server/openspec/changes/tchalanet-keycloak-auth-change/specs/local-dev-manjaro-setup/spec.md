# Spec — local-dev-manjaro-setup

## Intent

Document a reproducible Manjaro workstation setup for Tchalanet infra, API, web, mobile, and Keycloak authentication flows.

## Required capabilities

```text
Docker / Compose
Postgres
Redis
Traefik
Keycloak + custom provider
Backend Java 25
Web Nx/pnpm
Flutter Android
Local hostnames or LAN fallback
Swagger OAuth testing
```

## Packages

```bash
sudo pacman -Syu
sudo pacman -S --needed \
  git base-devel docker docker-compose \
  nodejs pnpm unzip curl jq make \
  android-tools android-udev
```

## Docker setup

```bash
sudo systemctl enable --now docker
sudo usermod -aG docker "$USER"
```

Log out/in, then verify:

```bash
docker version
docker compose version
```

## Java 25

Backend runtime is Java 25. If Manjaro packages do not provide the expected version, use SDKMAN.

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 25-tem
sdk use java 25-tem
java -version
```

## Backend run

```bash
cd tchalanet-server
./mvnw clean verify
./mvnw -pl tchalanet-app -am spring-boot:run
```

## Infra run

```bash
cd tchalanet-infra
docker compose --env-file envs/local/.env -f compose/docker-compose.local.yml up -d postgres redis traefik keycloak
```

## Web run

```bash
pnpm install
pnpm nx serve tchalanet-web
```

## Flutter run

```bash
cd tchalanet-mobile
flutter doctor
flutter pub get
flutter run
```

## Acceptance criteria

- Docker services start cleanly.
- Keycloak realm import works.
- Provider is loaded.
- Swagger OAuth login works.
- Backend `GET /tenant/me/profile` works after token login.
- Web login/profile load works.
- Flutter login/profile load works.
