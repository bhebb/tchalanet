#!/bin/bash
# Script pour copier les fichiers i18n dans le dossier public

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR/../.."
I18N_SOURCE="$PROJECT_ROOT/libs/i18n/locales"
I18N_DEST="$SCRIPT_DIR/public/assets/i18n"

echo "📁 Copie des fichiers i18n..."
echo "   Source: $I18N_SOURCE"
echo "   Dest:   $I18N_DEST"

# Créer le dossier de destination s'il n'existe pas
mkdir -p "$I18N_DEST"

# Copier les fichiers
cp -r "$I18N_SOURCE"/* "$I18N_DEST/"

echo "✅ Fichiers i18n copiés avec succès !"
ls -lh "$I18N_DEST"

