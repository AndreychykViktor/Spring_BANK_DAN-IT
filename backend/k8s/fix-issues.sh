#!/bin/bash

# Скрипт для виправлення проблем з GitHub Actions та AWS EKS

set -e

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║     🔧 ВИПРАВЛЕННЯ ПРОБЛЕМ З GITHUB ACTIONS ТА AWS EKS        ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# Кольори
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Зберегти поточний контекст
CURRENT_CONTEXT=$(kubectl config current-context 2>/dev/null || echo "")

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "1️⃣  ВИПРАВЛЕННЯ GITHUB ACTIONS"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Перевірити, чи існує .github/workflows/ci-cd.yml
if [ ! -f ".github/workflows/ci-cd.yml" ]; then
    echo -e "${RED}❌ Файл .github/workflows/ci-cd.yml не знайдено${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Файл .github/workflows/ci-cd.yml знайдено${NC}"

# Перевірити, чи файл додано в git
if git ls-files --error-unmatch .github/workflows/ci-cd.yml > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Файл вже додано в git${NC}"
else
    echo -e "${YELLOW}⚠️  Файл не додано в git, додаю...${NC}"
    git add .github/
    echo -e "${GREEN}✅ Файл додано в git${NC}"
fi

# Перевірити статус
if git diff --cached --quiet .github/; then
    echo -e "${YELLOW}⚠️  Немає змін для коміту${NC}"
else
    echo -e "${YELLOW}📝 Зміни готові для коміту:${NC}"
    git status .github/ | grep -E "new file|modified" || echo "   (немає нових змін)"
    echo ""
    echo -e "${YELLOW}💡 Виконайте команди:${NC}"
    echo "   git commit -m \"feat: add CI/CD pipeline for multi-cloud\""
    echo "   git push origin main"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "2️⃣  НАЛАШТУВАННЯ AWS EKS"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Перевірити AWS контекст
AWS_CTX=$(kubectl config get-contexts -o name | grep -i eks | head -1 || echo "")
if [ -z "$AWS_CTX" ]; then
    echo -e "${RED}❌ AWS EKS контекст не знайдено${NC}"
    echo -e "${YELLOW}💡 Налаштуйте AWS EKS спочатку${NC}"
    exit 1
fi

echo -e "${GREEN}✅ AWS EKS контекст знайдено: $AWS_CTX${NC}"

# Перемкнутися на AWS контекст
kubectl config use-context "$AWS_CTX" > /dev/null 2>&1
echo -e "${GREEN}✅ Перемкнуто на AWS контекст${NC}"

# Перевірити namespace
if kubectl get namespace bank-system > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Namespace bank-system існує${NC}"
else
    echo -e "${YELLOW}⚠️  Namespace bank-system не існує, створюю...${NC}"
    
    # Створити namespace
    if [ -f "backend/k8s/namespace.yaml" ]; then
        kubectl apply -f backend/k8s/namespace.yaml
        echo -e "${GREEN}✅ Namespace створено${NC}"
    else
        # Створити namespace вручну
        kubectl create namespace bank-system
        echo -e "${GREEN}✅ Namespace створено вручну${NC}"
    fi
fi

# Перевірити deployment
if kubectl get deployment bank-app -n bank-system > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Deployment bank-app існує${NC}"
    echo -e "${YELLOW}💡 Якщо потрібно застосувати манифести:${NC}"
    echo "   cd backend/k8s"
    echo "   kubectl apply -f secret.yaml"
    echo "   kubectl apply -f configmap.yaml"
    echo "   kubectl apply -f postgresql-pvc.yaml"
    echo "   kubectl apply -f postgresql-deployment.yaml"
    echo "   kubectl apply -f postgresql-service.yaml"
    echo "   kubectl apply -f rabbitmq-statefulset.yaml"
    echo "   kubectl apply -f rabbitmq-service.yaml"
    echo "   kubectl apply -f app-deployment.yaml"
    echo "   kubectl apply -f app-service.yaml"
    echo "   kubectl apply -f ingress-nginx.yaml"
else
    echo -e "${YELLOW}⚠️  Deployment bank-app не знайдено${NC}"
    echo -e "${YELLOW}💡 Застосуйте манифести:${NC}"
    echo "   cd backend/k8s"
    echo "   ./apply-all.sh"
    echo "   # або застосуйте окремо кожен файл"
fi

# Відновити поточний контекст
if [ -n "$CURRENT_CONTEXT" ]; then
    kubectl config use-context "$CURRENT_CONTEXT" > /dev/null 2>&1
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ ВИПРАВЛЕННЯ ЗАВЕРШЕНО"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📋 НАСТУПНІ КРОКИ:"
echo ""
echo "1. Закоммітити зміни:"
echo "   git commit -m \"feat: add CI/CD pipeline for multi-cloud\""
echo "   git push origin main"
echo ""
echo "2. Перевірити GitHub Actions:"
echo "   https://github.com/AndreychykViktor/Spring_BANK_DAN-IT/actions"
echo ""
echo "3. Якщо потрібно налаштувати AWS EKS:"
echo "   cd backend/k8s"
echo "   kubectl config use-context <aws-context>"
echo "   ./apply-all.sh"

