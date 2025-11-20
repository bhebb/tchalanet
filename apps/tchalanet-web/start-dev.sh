#!/bin/bash
# Script de test pour démarrer le serveur Nx en mode development

# Remonter à la racine du monorepo (2 niveaux depuis apps/tchalanet-web/)
cd "$(dirname "$0")/../.."

echo "🧹 Nettoyage du cache Vite..."
rm -rf node_modules/.vite

echo ""
echo "📁 Copie des fichiers i18n..."
mkdir -p public/assets/i18n
cp -f ../../libs/i18n/locales/*.json public/assets/i18n/
echo "✅ Fichiers i18n copiés"

echo ""
echo "🚀 Démarrage du serveur en mode development..."
echo "   - Mode: development"
echo "   - Port: 4200"
echo "   - Fichier .env: .env.development"
echo ""

npx nx serve tchalanet-web --configuration=development

