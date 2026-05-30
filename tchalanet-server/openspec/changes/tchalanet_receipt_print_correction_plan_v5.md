# Tchalanet — Plan de correction Print Receipt V5

> But : fixer définitivement le contrat de génération des reçus ticket pour PDF, A4, preview reçu et ESC/POS.
>
> Décision V5 : le backend ne gère pas l’imprimante physique. Il reçoit seulement un petit objet `printOptions` avec :
>
> ```text
> outputFormat + paperSize
> ```
>
> Si `printOptions` est absent : fallback backend = `PDF + A4`.

---

## 1. Décision finale V1

Le backend ne doit pas connaître ni gérer :

```text
- imprimante Wi-Fi
- imprimante Bluetooth
- imprimante USB
- imprimante système du navigateur
- printTargetId
- routing vers imprimante physique
```

Le backend doit seulement produire un document ou flux selon :

```text
PrintOutputFormat + PaperSize
```

Responsabilités :

```text
Backend:
- reçoit outputFormat + paperSize
- applique default PDF + A4 si absent
- valide la combinaison
- demande à core.sales de formatter le reçu selon ce profil
- demande à platform.document de générer PDF ou ESC_POS
- retourne les bytes / fichier au client

Client web/mobile/POS:
- choisit l’imprimante physique
- envoie le PDF ou les bytes ESC/POS vers l’imprimante
- gère Wi-Fi / Bluetooth / USB / dialogue système
```

---

## 2. Base existante à respecter

### `PrintOutputFormat`

```java
package com.tchalanet.server.core.sales.api.model.print;

public enum PrintOutputFormat {
    PDF,
    ESC_POS
}
```

Responsabilité :

```text
PrintOutputFormat = type de sortie générée
- PDF
- ESC_POS
```

Il ne doit pas contenir :

```text
- THERMAL_58
- THERMAL_80
- A4
- nombre de colonnes
- règles compactes
```

---

### `PaperSize`

```java
package com.tchalanet.server.platform.document.api.model;

public enum PaperSize {
    RECEIPT_58MM(164.41f),
    RECEIPT_80MM(226.77f),
    A4(595.28f);

    private final float widthPoints;

    PaperSize(float widthPoints) {
        this.widthPoints = widthPoints;
    }

    public float widthPoints() {
        return widthPoints;
    }
}
```

Responsabilité :

```text
PaperSize = support/layout papier
- RECEIPT_58MM
- RECEIPT_80MM
- A4
```

`widthPoints()` sert au rendu PDF.

---

## 3. Contrat d’entrée print

Créer un petit objet dans les requests de print.

### Fichier recommandé

```text
core/sales/api/model/print/PrintOptionsRequest.java
```

ou, si on veut rendre le contrat plus transversal plus tard :

```text
platform/document/api/model/PrintOptionsRequest.java
```

### Code V1

```java
package com.tchalanet.server.core.sales.api.model.print;

import com.tchalanet.server.platform.document.api.model.PaperSize;

public record PrintOptionsRequest(
    PrintOutputFormat outputFormat,
    PaperSize paperSize
) {}
```

### Utilisation dans les requests

```java
public record PrintTicketRequest(
    PrintOptionsRequest printOptions
) {}
```

ou :

```java
public record ReprintTicketRequest(
    PrintOptionsRequest printOptions
) {}
```

ou dans le sell si la vente peut déclencher une impression immédiate :

```java
public record SellTicketRequest(
    // autres champs de vente
    PrintOptionsRequest printOptions
) {}
```

---

## 4. Règle de default

`printOptions` est optionnel.

```text
printOptions absent
  -> outputFormat = PDF
  -> paperSize = A4
```

Si `printOptions` est présent mais partiel :

```text
outputFormat null
  -> PDF

paperSize null
  -> A4
```

Donc la résolution finale est toujours complète.

---

## 5. Combinaisons valides

```text
PDF + A4
  OK
  Usage : document papier, téléchargement, impression navigateur/OS.

PDF + RECEIPT_58MM
  OK
  Usage : preview PDF reçu 58mm.

PDF + RECEIPT_80MM
  OK
  Usage : preview PDF reçu 80mm.

ESC_POS + RECEIPT_58MM
  OK
  Usage : bytes ESC/POS pour imprimante thermique 58mm, envoyés par client mobile/POS.

ESC_POS + RECEIPT_80MM
  OK
  Usage : bytes ESC/POS pour imprimante thermique 80mm, envoyés par client mobile/POS.

ESC_POS + A4
  REFUSÉ
  Raison : ESC/POS est un flux imprimante thermique, pas un document papier A4.
```

---

## 6. Créer `DocumentPrintProfile`

Le backend doit transformer `PrintOptionsRequest` en profil interne de génération.

### Fichier à créer

```text
platform/document/api/model/DocumentPrintProfile.java
```

### Code proposé

```java
package com.tchalanet.server.platform.document.api.model;

import com.tchalanet.server.core.sales.api.model.print.PrintOutputFormat;

public record DocumentPrintProfile(
    PrintOutputFormat outputFormat,
    PaperSize paperSize,
    int textColumns,
    boolean monospace,
    boolean compact
) {
    public DocumentPrintProfile {
        if (outputFormat == null) {
            throw new IllegalArgumentException("outputFormat is required");
        }
        if (paperSize == null) {
            throw new IllegalArgumentException("paperSize is required");
        }
        if (textColumns <= 0) {
            throw new IllegalArgumentException("textColumns must be positive");
        }
        if (outputFormat == PrintOutputFormat.ESC_POS && paperSize == PaperSize.A4) {
            throw new IllegalArgumentException("ESC_POS does not support A4 paper");
        }
    }

    public static DocumentPrintProfile of(PrintOutputFormat outputFormat, PaperSize paperSize) {
        int columns = switch (paperSize) {
            case RECEIPT_58MM -> 32;
            case RECEIPT_80MM -> 42;
            case A4 -> 80;
        };

        boolean receiptPaper = paperSize == PaperSize.RECEIPT_58MM
            || paperSize == PaperSize.RECEIPT_80MM;

        return new DocumentPrintProfile(
            outputFormat,
            paperSize,
            columns,
            true,
            receiptPaper
        );
    }

    public boolean receiptPaper() {
        return paperSize == PaperSize.RECEIPT_58MM
            || paperSize == PaperSize.RECEIPT_80MM;
    }
}
```

### Note d’architecture

Actuellement `PrintOutputFormat` est dans `core.sales.api.model.print`.  
Pour V1, on peut l’utiliser tel quel.

À moyen terme, `PrintOutputFormat` pourrait être déplacé vers :

```text
platform.document.api.model.PrintOutputFormat
```

car c’est une notion de génération document, pas strictement sales.

---

## 7. Créer un resolver simple

Pas besoin de terminal ni d’imprimante.

### Fichier à créer

```text
platform/document/api/DocumentPrintProfileResolver.java
```

ou côté sales/application si on veut rester local au ticket print :

```text
core/sales/internal/application/receipt/TicketDocumentPrintProfileResolver.java
```

### Code proposé

```java
package com.tchalanet.server.platform.document.api;

import com.tchalanet.server.core.sales.api.model.print.PrintOptionsRequest;
import com.tchalanet.server.core.sales.api.model.print.PrintOutputFormat;
import com.tchalanet.server.platform.document.api.model.DocumentPrintProfile;
import com.tchalanet.server.platform.document.api.model.PaperSize;
import org.springframework.stereotype.Component;

@Component
public class DocumentPrintProfileResolver {

    public DocumentPrintProfile resolve(PrintOptionsRequest options) {
        var outputFormat = options == null || options.outputFormat() == null
            ? PrintOutputFormat.PDF
            : options.outputFormat();

        var paperSize = options == null || options.paperSize() == null
            ? PaperSize.A4
            : options.paperSize();

        return DocumentPrintProfile.of(outputFormat, paperSize);
    }
}
```

Règle :

```text
Le resolver ne résout pas une imprimante.
Il résout seulement le profil de génération document.
```

---

## 8. Créer `TicketReceiptLayoutProfile`

`core.sales` ne doit pas connaître `widthPoints`. Il a seulement besoin de règles texte.

### Fichier à créer

```text
core/sales/internal/application/receipt/formatter/TicketReceiptLayoutProfile.java
```

### Code proposé

```java
package com.tchalanet.server.core.sales.internal.application.receipt.formatter;

import com.tchalanet.server.platform.document.api.model.DocumentPrintProfile;

public record TicketReceiptLayoutProfile(
    int charsPerLine,
    boolean compact,
    boolean compactCurrencyDisplay,
    boolean printFullVerificationUrl
) {
    public TicketReceiptLayoutProfile {
        if (charsPerLine <= 0) {
            throw new IllegalArgumentException("charsPerLine must be positive");
        }
    }

    public static TicketReceiptLayoutProfile from(DocumentPrintProfile profile) {
        boolean receipt = profile.receiptPaper();

        return new TicketReceiptLayoutProfile(
            profile.textColumns(),
            profile.compact(),
            receipt,
            !receipt
        );
    }
}
```

Règles :

```text
RECEIPT_58MM:
  charsPerLine = 32
  compactCurrencyDisplay = true
  printFullVerificationUrl = false

RECEIPT_80MM:
  charsPerLine = 42
  compactCurrencyDisplay = true
  printFullVerificationUrl = false

A4:
  charsPerLine = 80
  compactCurrencyDisplay = false
  printFullVerificationUrl = true
```

---

## 9. Ajouter `ReceiptTextLayout`

### Fichier à créer

```text
core/sales/internal/application/receipt/formatter/ReceiptTextLayout.java
```

### Code proposé

```java
package com.tchalanet.server.core.sales.internal.application.receipt.formatter;

import org.springframework.stereotype.Component;

@Component
public class ReceiptTextLayout {

    public String separator(TicketReceiptLayoutProfile profile) {
        return "-".repeat(profile.charsPerLine());
    }

    public String truncate(String value, int width) {
        if (value == null) {
            return "";
        }
        var text = value.trim();
        if (text.length() <= width) {
            return text;
        }
        if (width <= 3) {
            return text.substring(0, width);
        }
        return text.substring(0, width - 3) + "...";
    }

    public String center(String value, TicketReceiptLayoutProfile profile) {
        var text = truncate(value, profile.charsPerLine());
        int left = Math.max(0, (profile.charsPerLine() - text.length()) / 2);
        return " ".repeat(left) + text;
    }

    public String rightPad(String value, int width) {
        var text = value == null ? "" : value;
        if (text.length() >= width) {
            return truncate(text, width);
        }
        return text + " ".repeat(width - text.length());
    }

    public String leftPad(String value, int width) {
        var text = value == null ? "" : value;
        if (text.length() >= width) {
            return truncate(text, width);
        }
        return " ".repeat(width - text.length()) + text;
    }

    public String labelValue(String label, String value, TicketReceiptLayoutProfile profile) {
        return truncate(label + ": " + value, profile.charsPerLine());
    }

    public String leftRight(String left, String right, TicketReceiptLayoutProfile profile) {
        var safeLeft = left == null ? "" : left;
        var safeRight = right == null ? "" : right;
        int spaces = profile.charsPerLine() - safeLeft.length() - safeRight.length();

        if (spaces < 1) {
            return truncate(safeLeft + " " + safeRight, profile.charsPerLine());
        }

        return safeLeft + " ".repeat(spaces) + safeRight;
    }
}
```

---

## 10. Modifier la signature du formatter principal

### Avant

```java
public TicketReceiptPrintContent format(TicketReceiptView receipt, PrintOutputFormat format)
```

### Après

```java
public TicketReceiptPrintContent format(
    TicketReceiptView receipt,
    DocumentPrintProfile documentProfile
)
```

Puis :

```java
var layoutProfile = TicketReceiptLayoutProfile.from(documentProfile);
```

Pourquoi :

```text
PrintOutputFormat seul ne suffit pas.
PDF peut être A4, 58mm ou 80mm.
ESC_POS peut être 58mm ou 80mm.
```

---

## 11. Corriger `TicketReceiptPrintFormatter`

Responsabilités :

```text
- recevoir DocumentPrintProfile ;
- dériver TicketReceiptLayoutProfile ;
- assembler les sections ;
- placer les séparateurs majeurs ;
- déléguer l’argent au TicketReceiptMoneyFormatter ;
- déléguer les lignes jeu au TicketReceiptGameLinesFormatter ;
- ne pas imprimer l’URL complète sur receipt paper ;
- appliquer un guard anti-overflow.
```

Séparateurs :

```text
[Branding]
separator
[Ticket facts]
separator
[Draw + games]
separator
[Money summary]
separator
[Verification]
```

Style unique :

```java
layout.separator(layoutProfile)
```

---

## 12. Corriger `TicketReceiptMoneyFormatter`

### Problème actuel

```java
public String format(Money money) {
    return money.amount().setScale(2, RoundingMode.HALF_UP) + " " + money.currency().code();
}
```

Ce formatter répète la devise partout.

### Correction

```java
public String format(Money money, TicketReceiptLayoutProfile profile) {
    if (money == null) {
        return null;
    }

    var amount = money.amount()
        .setScale(2, RoundingMode.HALF_UP)
        .toPlainString();

    if (profile.compactCurrencyDisplay()) {
        return amount;
    }

    return amount + " " + money.currency().code();
}
```

### Note de devise

Sur receipt paper :

```text
Montants en HTG
```

Puis :

```text
1.00
50.00
150.00
```

Sur A4 :

```text
1.00 HTG
50.00 HTG
150.00 HTG
```

---

## 13. Revoir `TicketReceiptView`

### Problème actuel

`TicketReceiptView` transporte des montants déjà formatés en `String` :

```java
String stakeTotal,
String totalAmount,
String potentialPayout
```

C’est trop tôt.

### Option propre

Porter des valeurs métier :

```java
TicketMoneyBreakdown moneyBreakdown,
Money potentialPayout
```

ou au minimum :

```java
Money stakeTotal,
Money totalAmount,
Money potentialPayout
```

### Pourquoi

Le formatter final doit pouvoir choisir :

```text
PDF + A4:
  3.00 HTG

PDF/ESC_POS + RECEIPT_58MM:
  3.00
  avec note "Montants en HTG"
```

Compromis temporaire :

```text
Si on garde les strings en V1, retirer la devise uniquement quand compactCurrencyDisplay = true.
```

Mais ça doit rester transitoire.

---

## 14. Corriger `TicketReceiptGameLinesFormatter`

Nouvelle signature :

```java
public List<TicketReceiptTextLine> format(
    List<TicketReceiptLineView> receiptLines,
    TicketReceiptTranslations translations,
    TicketReceiptLayoutProfile profile
)
```

Règles :

```text
- utiliser ReceiptTextLayout ;
- colonnes fixes selon 32 / 42 / 80 chars ;
- ne pas répéter HTG sur receipt paper ;
- optionLabel / betType sur ligne séparée ;
- promotion tronquée si nécessaire ;
- aucune ligne > charsPerLine.
```

Colonnes 58mm :

```text
No        Mise      Gain
#1 11        1.00     50.00
```

Colonnes 80mm :

```text
No            Mise          Gain
#1 11          1.00        50.00
```

A4 peut être plus détaillé.

---

## 15. Corriger `DefaultTicketDrawLabelFormatter` ou son usage

Problème :

```text
Haiti • Georgia • Late - 2026-05-28 23:34
```

peut être trop long.

V1 simple :

```java
layout.truncate(receipt.drawLabel(), profile.charsPerLine())
```

V2 meilleure :

```java
THERMAL_SHORT
```

Exemple :

```text
Haiti Georgia Late
Heure: 2026-05-28 23:34
```

---

## 16. Corriger vérification / QR

`TicketVerificationUrlBuilder` reste correct.

Règle :

```text
QR payload = verificationUrl complet
Texte receipt paper = Code: EERV-FD5K
Texte A4 = URL complète possible
```

À garder :

```java
new TicketReceiptQrView(receipt.verificationUrl(), receipt.verificationUrl())
```

À changer dans footer :

```java
if (profile.printFullVerificationUrl()) {
    addLabel(footer, translations.text(TicketReceiptI18nKeys.VERIFICATION), receipt.verificationUrl(), false, profile);
} else {
    addLabel(footer, "Code", receipt.displayCode(), false, profile);
}
```

---

## 17. Ajouter guard anti-overflow

À la fin de `TicketReceiptPrintFormatter`, vérifier toutes les lignes :

```java
private void assertLines(String part, List<TicketReceiptTextLine> lines, TicketReceiptLayoutProfile profile) {
    for (var line : lines) {
        if (line.text().length() > profile.charsPerLine()) {
            throw new IllegalStateException(
                "Receipt " + part + " line exceeds width "
                    + profile.charsPerLine() + ": " + line.text()
            );
        }
    }
}
```

À appliquer sur :

```text
- header
- section titles
- section lines
- totals
- footer
```

---

## 18. Corriger `platform.document` renderer

### PDF

Utiliser :

```text
PaperSize.widthPoints()
```

Règles :

```text
PDF + RECEIPT_58MM:
  largeur PDF 164.41 points
  rendu preview reçu

PDF + RECEIPT_80MM:
  largeur PDF 226.77 points
  rendu preview reçu

PDF + A4:
  largeur PDF 595.28 points
  rendu papier
```

Autres règles :

```text
- police monospace pour reçu textuel ;
- ne pas re-wrapper les lignes déjà formatées ;
- respecter DocumentPrintProfile.paperSize().
```

### ESC_POS

Règles :

```text
- ESC_POS accepte RECEIPT_58MM ou RECEIPT_80MM ;
- ESC_POS + A4 est invalide ;
- utiliser textColumns du DocumentPrintProfile ;
- retourner les bytes ESC/POS au client ;
- le client mobile/POS envoie à l’imprimante.
```

---

## 19. Tests à ajouter

### `DocumentPrintProfileResolverTest`

```text
- null options => PDF + A4
- null outputFormat => PDF
- null paperSize => A4
- PDF + A4 => OK
- PDF + RECEIPT_58MM => OK
- PDF + RECEIPT_80MM => OK
- ESC_POS + RECEIPT_58MM => OK
- ESC_POS + RECEIPT_80MM => OK
- ESC_POS + A4 => rejected
```

### `DocumentPrintProfileTest`

```text
- PDF + A4 => 80 columns
- PDF + RECEIPT_58MM => 32 columns
- PDF + RECEIPT_80MM => 42 columns
- ESC_POS + RECEIPT_58MM => 32 columns
- ESC_POS + RECEIPT_80MM => 42 columns
```

### `TicketReceiptLayoutProfileTest`

```text
- receipt paper => compactCurrencyDisplay true
- receipt paper => printFullVerificationUrl false
- A4 => compactCurrencyDisplay false
- A4 => printFullVerificationUrl true
```

### Formateurs sales

```text
TicketReceiptBrandingFormatterTest
TicketReceiptFactsFormatterTest
TicketReceiptGameLinesFormatterTest
TicketReceiptMoneyFormatterTest
TicketReceiptPrintFormatterTest
```

### Snapshots

```text
- PDF + A4
- PDF + RECEIPT_58MM
- PDF + RECEIPT_80MM
- ESC_POS + RECEIPT_58MM
- ESC_POS + RECEIPT_80MM
- long ticket code
- long draw label
- multiple games
- promotion line
- verification footer
```

---

## 20. Ordre recommandé d’implémentation

1. Garder `PrintOutputFormat` inchangé : `PDF`, `ESC_POS`.
2. Réutiliser `PaperSize` existant.
3. Créer `PrintOptionsRequest`.
4. Ajouter `printOptions` aux requests de print/reprint.
5. Créer `DocumentPrintProfile`.
6. Créer `DocumentPrintProfileResolver` avec default `PDF + A4`.
7. Créer `TicketReceiptLayoutProfile`.
8. Créer `ReceiptTextLayout`.
9. Modifier `TicketReceiptPrintFormatter` pour recevoir `DocumentPrintProfile`.
10. Modifier les sous-formateurs pour recevoir `TicketReceiptLayoutProfile`.
11. Corriger séparateurs.
12. Corriger montants : note unique sur receipt paper.
13. Corriger lignes de jeu.
14. Corriger label tirage.
15. Corriger footer vérification / QR.
16. Ajouter guard anti-overflow.
17. Corriger renderer PDF selon `PaperSize.widthPoints()`.
18. Corriger renderer ESC_POS selon `textColumns`.
19. Ajouter tests resolver/profile.
20. Ajouter tests unitaires par formateur.
21. Ajouter snapshots PDF/ESC_POS.

---

## 21. Résumé final

La séparation V1 devient :

```text
PrintOptionsRequest
  = petit objet optionnel envoyé par web/mobile/POS
  = outputFormat + paperSize

Default si absent
  = PDF + A4

PrintOutputFormat
  = PDF ou ESC_POS

PaperSize
  = A4, RECEIPT_58MM, RECEIPT_80MM

DocumentPrintProfile
  = output + paper + columns + compact

TicketReceiptLayoutProfile
  = largeur texte + règles compactes sales

ReceiptTextLayout
  = separator, truncate, pad, center, labelValue

core.sales
  = formatte le reçu selon le profil reçu

platform.document
  = génère PDF ou ESC_POS selon le même profil

client web/mobile/POS
  = choisit et gère l’imprimante physique
```

Le backend génère.  
Le client imprime.  
Le contrat est `outputFormat + paperSize`.
