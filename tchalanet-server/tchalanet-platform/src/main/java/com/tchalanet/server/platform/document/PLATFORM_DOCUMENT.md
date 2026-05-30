# Platform Capability `platform.document` — Document Rendering

> Archetype : Application Service Module.


## Rôle

Exposer un contrat transversal unique pour rendre des documents binaires (PDF, ESC/POS, PNG QR).

**Ce module fait** :
- `render(DocumentRenderRequest)` → `RenderedDocument` (API Java)
- Supporte plusieurs formats et types de contenu (reçu, rapport, QR, générique)
- Encapsule PDFBox, ZXing, builder ESC/POS dans `internal/`

**Ce module ne fait pas** :
- Composer les données métier (le contenu doit être déjà formaté)
- Stocker les documents générés
- Connaître les types métier core/features
- Distribuer (voir `platform.communication`)


## Surface API

- `DocumentApi` (Java) : `render(DocumentRenderRequest)`
- Modèles : `DocumentRenderRequest`, `RenderedDocument`, `DocumentKind`, `DocumentFormat`, `DocumentContent` (scellé)

Pas d’endpoint REST public direct (API Java consommée par d’autres modules platform/core).


## Enums

### `DocumentKind` : `RECEIPT` · `REPORT` · `QR_CODE`
### `DocumentFormat`
| Valeur | Content-Type | Extension |
|---|---|---|
| `PDF` | `application/pdf` | `.pdf` |
| `ESC_POS` | `application/octet-stream` | `.bin` |
| `PNG` | `image/png` | `.png` |
| `HTML_PREVIEW` | `text/html` | `.html` |

### `PaperSize` : `RECEIPT_58MM` (164pt) · `RECEIPT_80MM` (227pt) · `A4` (595pt)
### `LineStyle` : `NORMAL` · `BOLD` · `TITLE` · `SMALL` · `WARNING`
### `AssetKind` : `QR` · `IMAGE` · `LOGO`

`DocumentOptions` factory : `defaults()` · `receipt80mm()` · `receipt58mm()`  
`DocumentContent` : sealed interface — `GenericDocumentContent` · `QrDocumentContent` · `ReceiptDocumentContent` · `ReportDocumentContent`

## Intégration

- Les modules platform/core appellent `DocumentApi` pour générer des documents binaires
- La distribution se fait via `platform.communication` si besoin

## Règles et limitations

- Pas de logique métier ni de stockage dans ce module
- Les formats supportés sont extensibles côté internal/

## 3. Imports autorisés

| Caller                | Import autorisé                                          |
| --------------------- | -------------------------------------------------------- |
| `core.*`, `features.*` | `platform.document.api`, `platform.document.api.model`  |
| `platform.document.internal.*` | **interdit hors du module**                    |

Tout nouvel ajout dans `api/model/` doit rester **agnostique du domaine**.

## 4. Construction d'une requête côté sales

Côté consommateur (features ou core qui possède la donnée), créer un petit
factory et y mapper la donnée métier vers un `DocumentRenderRequest`. Exemple
réel : [TicketReceiptDocumentRequestFactory](../../../../tchalanet-features/src/main/java/com/tchalanet/server/features/receipt/app/TicketReceiptDocumentRequestFactory.java).

```java
var request = new DocumentRenderRequest(
    DocumentKind.RECEIPT,
    DocumentFormat.PDF,
    "Ticket Tchalanet",
    ReceiptDocumentContent.ofBodyLines(bodyLines),
    List.of(DocumentAsset.qr("ticket-qr", verifyUrl, 300)),
    DocumentOptions.defaults(),
    locale,
    Map.of());

RenderedDocument doc = documentApi.render(request);
// doc.bytes(), doc.contentType(), doc.filename(), doc.format()
```

## 5. Compatibilité format ↔ contenu (état actuel)

| Format    | ReceiptDocumentContent | ReportDocumentContent | GenericDocumentContent |
| --------- | :--------------------: | :-------------------: | :--------------------: |
| `PDF`     | ✅                     | ❌ (non implémenté)   | ❌                     |
| `ESC_POS` | ✅                     | ❌                    | ❌                     |
| `PNG`     | n/a (QR-only)          | n/a                   | ✅ (asset QR requis)   |

Les combinaisons non supportées **doivent** lever un `IllegalArgumentException`
avec un message explicite. Pas d'exception `Spring MVC` exposée par l'API.

## 6. QR

Un `DocumentAsset` de kind `QR` peut être fourni :
- soit par `bytes` déjà rendus,
- soit par `payload` (URL ou texte) + `sizePx` ; le renderer interne (PNG ou
  ESC/POS) génère les bytes au moment du rendu.

## 7. Tests

- `DefaultDocumentApiTest` — dispatch + erreurs.
- `PdfDocumentRendererTest` — `%PDF` magic + content-type.
- `EscPosDocumentRendererTest` — init `0x1B 0x40`, cut `0x1D 0x56 0x00`.
- `QrPngDocumentRendererTest` — PNG signature.
