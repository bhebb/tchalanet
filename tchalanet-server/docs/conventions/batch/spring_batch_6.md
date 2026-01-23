# Spring Batch 6.0 — Règles Tchalanet

> **Status**: NORMATIVE  
> **Scope**: tchalanet-server (common/batch, core/\*/infra/batch)  
> **Audience**: Backend developers  
> **Last reviewed**: 2026-01-22  
> **Source**:
>
> - https://github.com/spring-projects/spring-batch/wiki/Spring-Batch-6.0-Migration-Guide
> - https://docs.spring.io/spring-batch/docs/6.0.x/reference/html/whatsnew.html

---

## 🎯 Objectif

Ce document fixe les règles normatives pour l'utilisation de Spring Batch 6.0 dans Tchalanet. Il complète et remplace les recommandations v4/v5, et précise les impacts concrets sur l'architecture `common.batch` et `features.ops`.

---

## 1) Store explicite (JDBC)

En Batch 6, l’infra de repository doit être explicite (JDBC ou autre store supporté).  
Pour JDBC, activez le repository JDBC dans une configuration d'infra (là où le DataSource et le TransactionManager existent).

Exemple indicatif (implementation module / infra):

```java
@Configuration
@EnableBatchProcessing
@EnableJdbcJobRepository(
  dataSourceRef = "batchDataSource",
  transactionManagerRef = "batchTransactionManager"
)
public class BatchInfraConfig { }
```

Règle Tchalanet :

- ❌ ne pas mettre cette configuration dans `common.batch` (ce package reste purement technique, sans contrôleurs ni datasources)
- ✅ mettre cette configuration dans les modules d'infra (ex : `server-infra`)

---

## 2) `JobExplorer` interdit (déprécié)

- `JobExplorer` est deprecated depuis SB6 et prévu pour retrait futur.
- Règle :
  - ✅ utiliser `JobRepository` (ou un adapter) pour lire les exécutions/instances
  - ❌ ne pas injecter `JobExplorer` dans `features.ops` ou controllers

---

## 3) Interfaces `Item*` (packages SB6)

SB6 a clarifié certains packages. Respectez les imports recommandés pour éviter les ambigüités entre versions.

- Exemple d'interfaces canonique (vérifier la version et IDE auto-import) :

```text
org.springframework.batch.core.ItemReader
org.springframework.batch.core.ItemProcessor
org.springframework.batch.core.ItemWriter
```

Règle :

- ❌ éviter les imports anciens/ambigus provenant d'APIs legacy
- ✅ vérifier les imports fournis par votre IDE et target SB6

---

## 4) StepBuilder & transaction manager (SB6)

La surcharge `chunk(int, transactionManager)` est dépréciée en SB6.

Règle :

- ✅ appeler `.chunk(int)` puis `.transactionManager(txManager)` séparément
- ❌ ne pas utiliser `.chunk(int, txManager)`

Exemple :

```java
new StepBuilder("settleDrawsStep", jobRepository)
  .<DrawId, DrawId>chunk(10)
  .transactionManager(batchTxManager)
  .reader(reader)
  .processor(processor)
  .writer(writer)
  .build();
```

---

## 5) Lancement des jobs : règle projet (NORMATIVE)

### 5.1 Politique projet

Tchalanet déclenche les jobs opératoires via :

- ✅ `JobOperator.start(Job, JobParameters)` (usage canonique pour ops)

Interdits :

- ❌ `JobOperator.start(String, Properties)` (pattern legacy)
- ❌ appels ad-hoc à `JobLauncher` depuis controllers ops si cela contourne la stratégie d'opérations

Note : Spring ne marque pas `JobLauncher` comme supprimé partout, mais pour la surface « ops » nous standardisons sur `JobOperator` afin d'avoir un point central, puissant et compatible SB6.

---

## 6) Modèle de données : IDs primitifs et conversions temporelles

- Les identifiants Batch doivent être traités comme des `long` primitifs dans les DTOs et mappings (SB6 favorise les primitives pour les IDs).
- `JobExecution.getStartTime()` / `getEndTime()` peuvent être exposés comme `LocalDateTime` par SB6 : toute conversion vers `Instant` doit préciser explicitement la zone.

Règle de conversion (exemple) :

```java
ZoneId zone = (ctx != null && ctx.tenantZoneId() != null) ? ctx.tenantZoneId() : ZoneId.of("UTC");
Instant instant = ZonedDateTime.of(localDateTime, zone).toInstant();
```

- ❌ éviter `localDateTime.toInstant()` sans zone.

---

## 7) Checklist PR (exigences de validation)

Avant de merger une PR touchant du code Batch, vérifier :

- Pas d'usage de `JobExplorer`
- Pas d'appel à `JobOperator.start(String, Properties)` ou patterns legacy
- `StepBuilder`: ne pas utiliser `chunk(int, txManager)` (utiliser `.transactionManager(...)` séparément)
- Imports `Item*` conformes à la version SB6
- DTOs batch utilisent `long` pour les IDs
- Conversions `LocalDateTime -> Instant` explicitement avec zone
- Readers/Writers/Step components en constructor injection (pas de setters + afterPropertiesSet)

---

## 8) Références

- Spring Batch 6.0 Migration Guide (Wiki): https://github.com/spring-projects/spring-batch/wiki/Spring-Batch-6.0-Migration-Guide
- Spring Batch 6 What’s new: https://docs.spring.io/spring-batch/reference/whatsnew.html

---
