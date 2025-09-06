# Sécurité du Backend
Cette page détaille les mécanismes de sécurité mis en place dans le backend Tchalanet.
## Architecture de sécurité

graph TD
CLIENT[Client] -->|1. Authentification| AUTH[Service d'Auth]
AUTH -->|2. JWT Token| CLIENT
CLIENT -->|3. Requête avec JWT| API[API Gateway]
API -->|4. Validation Token| JWT[JWT Validator]
JWT -->|5. OK| API
API -->|6. Requête| RESOURCE[Resource Server]
RESOURCE -->|7. Check Permissions| ACL[Access Control]
RESOURCE -->|8. Check Row Access| RLS[Row Level Security]
