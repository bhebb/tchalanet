# ./keycloak/Dockerfile

FROM maven:3.9-eclipse-temurin-21-alpine AS provider-builder
WORKDIR /build
COPY tchalanet-keycloak-provider/ .
RUN mvn -q -DskipTests clean package && echo "✔ Provider JAR built"

FROM quay.io/keycloak/keycloak:26.4.0

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
RUN /opt/keycloak/bin/kc.sh build
