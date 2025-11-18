local setup
docker compose --env-file ./envs/dev/.env \
-f docker-compose.yml -f docker-compose-dev.yml up -d --build

## Certificats locaux (mkcert) + truststore Java

Pour éviter le profil `insecure` et conserver HTTPS local fiable:

Etapes rapides:
1. Installer dépendances (une fois):
   brew install mkcert nss
2. Lancer le script:
   ./scripts/setup-mkcert-truststore.sh
3. Charger les variables d'environnement:
   source ./.env.local-truststore
4. Démarrer l'application:
   ./mvnw spring-boot:run

Le script génère:
- Certificat SAN pour auth.localtest.me, app.localtest.me, api.localtest.me
- PKCS#12 utilisable par Keycloak / proxy
- Truststore JKS: `local-truststore.jks`
- Fichier `.env.local-truststore` prêt à sourcer

Si vous changez / ajoutez des domaines, modifiez le tableau DOMAINS dans `scripts/setup-mkcert-truststore.sh` puis relancez.

Pour revenir au mode relax (débogage rapide), réactivez le profil: `SPRING_PROFILES_ACTIVE=local-ide,insecure`.
