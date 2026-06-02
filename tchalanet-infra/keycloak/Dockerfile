# ./keycloak/Dockerfile

FROM maven:3.9-eclipse-temurin-21-alpine AS provider-builder
WORKDIR /build
COPY tchalanet-keycloak-provider/ .
RUN mvn -q -DskipTests clean package && echo "✔ Provider JAR built"

FROM keycloak/keycloak:26.6.2

# Activer mgmt port pour /health et /metrics
ENV KC_HEALTH_ENABLED=true \
    KC_METRICS_ENABLED=true \
    KC_HTTP_MANAGEMENT_ENABLED=true \
    TZ=UTC

# Thèmes & provider
#  COPY --chown=keycloak:keycloak themes/ /opt/keycloak/themes/
COPY --from=provider-builder --chown=keycloak:keycloak /build/target/*.jar /opt/keycloak/providers/
# (optionnel) Realms pré-générés
COPY --chown=keycloak:keycloak realms/*-realm.json /opt/keycloak/data/import/

USER keycloak
WORKDIR /opt/keycloak
# Build-time options only. Per Keycloak 26 docs:
#   - db, transaction-xa-enabled  →  BUILD-TIME (require re-augmentation if changed)
#   - http-enabled, http-management-enabled, cache, proxy-headers  →  RUNTIME
# Runtime options live in docker-compose-keycloak.yml's `command:` block.
RUN /opt/keycloak/bin/kc.sh build \
    --db=postgres \
    --transaction-xa-enabled=false
