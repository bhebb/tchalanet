# Platform Capability `platform.document` — Document Rendering

> Archetype : Application Service Module.

## 1. Rôle

Exposer un contrat **transversal** unique pour rendre des documents binaires
(PDF, ESC/POS pour imprimantes thermiques, PNG pour QR).

```java
RenderedDocument render(DocumentRenderRequest request);
```

**Ce module fait** :
- Rendre un `DocumentRenderRequest` typé dans un format cible (`DocumentFormat`).
- Supporter plusieurs `DocumentContent` typés (reçus, rapports, contenu générique).
- Encapsuler PDFBox, ZXing et le builder ESC/POS dans `internal/`.

**Ce module ne fait pas** :
- Composer les données métier (le caller doit fournir le contenu déjà formaté).
- Stocker les documents générés.
- Connaître `Ticket`, `Payout`, `Draw`, `SalesSession`, `Outlet` ou tout type
  `core.*` / `features.*`.
- Distribuer par email/SMS (→ `platform.communication`).

## 2. Structure

```text
platform/document/
  api/
    DocumentApi.java
    model/
      DocumentRenderRequest.java
      RenderedDocument.java
      DocumentKind.java          ← RECEIPT, REPORT, QR, GENERIC
      DocumentFormat.java        ← PDF, ESC_POS, PNG (+ contentType/extension)
      DocumentContent.java       ← sealed
      ReceiptDocumentContent.java
      ReportDocumentContent.java
      GenericDocumentContent.java
      DocumentSection.java
      DocumentLine.java          ← (text, LineStyle)
      LineStyle.java             ← NORMAL, BOLD, TITLE
      DocumentTable.java
      DocumentAsset.java         ← QR | IMAGE (bytes or payload+sizePx)
      AssetKind.java
      DocumentOptions.java
  internal/
    service/DefaultDocumentApi.java  ← dispatch par DocumentFormat
    render/
      DocumentRenderer.java          ← format() + render(request)
      PdfDocumentRenderer.java
      EscPosDocumentRenderer.java
      QrPngDocumentRenderer.java
    pdf/ReceiptPdfRenderer.java      ← PDFBox
    escpos/EscPosBuilder.java
    qr/QrRenderer.java + zxing/ + escpos/
    receipt/ReceiptModel(.Line/.Span)
```

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
