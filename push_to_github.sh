#!/bin/bash
echo "==================================================="
echo "  AUTOMATIC PUSH TO GITHUB BY ARENA AGENT"
echo "==================================================="
echo ""
cd "$(dirname "$0")"
git init
git add .
git commit -m "Initial commit - WhatsApp Status HQ Compressor with GitHub Actions Workflow"
git branch -M main
git remote add origin https://github.com/Kallxy1/apkcompres.git
echo ""
echo "Menghubungkan ke GitHub... Anda mungkin diminta untuk login di browser."
echo ""
git push -u origin main --force
echo ""
echo "==================================================="
echo "  SELESAI! Silakan buka repositori GitHub Anda."
echo "==================================================="
