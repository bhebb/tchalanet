---
name: spring-boot-core
description: Use when creating or configuring a Spring Boot module, beans, properties, profiles, or general Spring patterns in tchalanet-server — covers @Configuration, @Bean, actuator, application.yml, starters, Spring context bootstrap, constructor injection, Lombok rules, and error handling.
allowed-tools: Read, Write, Edit, Glob, Grep, Bash
---

# Spring Boot Core — Tchalanet

> Stack : Java 25 · Spring Boot 4.0.1 · Maven · Jakarta EE

## Règles fondamentales

```java
// ✅ Constructor injection TOUJOURS (jamais @Autowired sur champ)
@Service
@RequiredArgsConstructor
public class MyService {
    private final MyRepository repo;
    private final MyPort port;
}

// ❌ Interdit
@Autowired
private MyRepository repo;

// ✅ jakarta.* uniquement (jamais javax.*)
import jakarta.persistence.*;
import jakarta.validation.*;
import jakarta.transaction.*;

// ❌ Interdit
import javax.persistence.*;
```

## Configuration — application.yml

```yaml
# Structure standard Tchalanet
spring:
  application:
    name: tchalanet-server
  datasource:
    url: ${DATABASE_URL}
  jpa:
    hibernate:
      ddl-auto: validate # TOUJOURS validate — Flyway gère le DDL
    open-in-view: false # TOUJOURS désactivé
  mvc:
    servlet:
      path: /api/v1 # Prefix global — ne pas répéter dans les controllers

# Profiles : local, dev, staging, prod
# Mêmes services et topologie partout — seule la config diffère
```

## Lombok — règles

```java
// ✅ Autorisé
@Value           // immutable
@Builder         // builder pattern
@RequiredArgsConstructor  // constructor injection
@Getter          // getters uniquement
@Slf4j           // logging

// ❌ Interdit
@Data            // génère equals/hashCode problématique avec JPA
@AllArgsConstructor  // préférer @Builder
```

## Gestion des erreurs

```java
// ✅ ProblemDetail (RFC 9457) pour toutes les erreurs HTTP
// Géré automatiquement par l'exception handler global
// Ne jamais wrapper une erreur dans ApiResponse<T>

// Exceptions métier → étendent une base commune
public class TicketNotFoundException extends TchNotFoundException {
    public TicketNotFoundException(TicketId id) {
        super("ticket-not-found", "Ticket not found: " + id.value());
    }
}
```

## Actuator et observabilité

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  tracing:
    sampling:
      probability: 1.0 # 100% en dev, ajuster en prod
```

## Profiles et configuration par env

```java
// ✅ @Profile pour les beans env-spécifiques
@Configuration
@Profile("local")
public class LocalDevConfig { ... }

// ✅ Properties typées (jamais @Value partout)
@ConfigurationProperties(prefix = "tchalanet")
public record TchalanetProperties(
    Duration tokenExpiry,
    String keycloakRealm
) {}
```

## Checklist nouveau module Spring Boot

- [ ] `jakarta.*` uniquement — zéro `javax.*`
- [ ] `ddl-auto: validate` dans la config JPA
- [ ] `open-in-view: false`
- [ ] Constructor injection via `@RequiredArgsConstructor`
- [ ] Pas de `@Data` Lombok sur les entités JPA
- [ ] Exceptions métier étendent la hiérarchie commune
- [ ] Properties externalisées via `@ConfigurationProperties`
