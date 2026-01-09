#!/bin/bash

# Скрипт для швидкої перевірки CI/CD та моніторингу

set -e

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║     🔍 ШВИДКА ПЕРЕВІРКА CI/CD ТА МОНІТОРИНГУ                 ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# Кольори
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

check_command() {
    if ! command -v $1 &> /dev/null; then
        echo -e "${RED}❌ $1 не встановлено${NC}"
        return 1
    fi
    return 0
}

echo "🔧 Перевірка інструментів..."
check_command kubectl || exit 1
check_command curl || exit 1
echo -e "${GREEN}✅ Всі інструменти встановлені${NC}"
echo ""

# Зберегти поточний контекст
CURRENT_CONTEXT=$(kubectl config current-context 2>/dev/null || echo "")

# Перевірка Azure AKS
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📦 ПЕРЕВІРКА AZURE AKS"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if kubectl config get-contexts -o name | grep -q "andreychyk-bank-cluster"; then
    kubectl config use-context andreychyk-bank-cluster > /dev/null 2>&1
    
    echo "📊 Deployment:"
    kubectl get deployment bank-app -n bank-system 2>/dev/null && echo -e "${GREEN}✅ Deployment знайдено${NC}" || echo -e "${RED}❌ Deployment не знайдено${NC}"
    
    echo ""
    echo "🟢 Поди:"
    PODS=$(kubectl get pods -n bank-system -l app=bank-app -o jsonpath='{.items[*].metadata.name}' 2>/dev/null || echo "")
    if [ -n "$PODS" ]; then
        for pod in $PODS; do
            STATUS=$(kubectl get pod $pod -n bank-system -o jsonpath='{.status.phase}' 2>/dev/null || echo "Unknown")
            if [ "$STATUS" = "Running" ]; then
                echo -e "   ${GREEN}✅ $pod: Running${NC}"
            else
                echo -e "   ${YELLOW}⚠️  $pod: $STATUS${NC}"
            fi
        done
    else
        echo -e "${RED}❌ Поди не знайдено${NC}"
    fi
    
    echo ""
    echo "🌐 Ingress:"
    INGRESS=$(kubectl get ingress -n bank-system -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")
    if [ -n "$INGRESS" ]; then
        HOST=$(kubectl get ingress $INGRESS -n bank-system -o jsonpath='{.spec.rules[0].host}' 2>/dev/null || echo "")
        echo -e "   ${GREEN}✅ $INGRESS: $HOST${NC}"
    else
        echo -e "${YELLOW}⚠️  Ingress не знайдено${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  Azure контекст не знайдено${NC}"
fi

echo ""

# Перевірка AWS EKS
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📦 ПЕРЕВІРКА AWS EKS"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

AWS_CTX=$(kubectl config get-contexts -o name | grep -i eks | head -1 || echo "")
if [ -n "$AWS_CTX" ]; then
    kubectl config use-context "$AWS_CTX" > /dev/null 2>&1
    
    echo "📊 Deployment:"
    kubectl get deployment bank-app -n bank-system 2>/dev/null && echo -e "${GREEN}✅ Deployment знайдено${NC}" || echo -e "${RED}❌ Deployment не знайдено${NC}"
    
    echo ""
    echo "🟢 Поди:"
    PODS=$(kubectl get pods -n bank-system -l app=bank-app -o jsonpath='{.items[*].metadata.name}' 2>/dev/null || echo "")
    if [ -n "$PODS" ]; then
        for pod in $PODS; do
            STATUS=$(kubectl get pod $pod -n bank-system -o jsonpath='{.status.phase}' 2>/dev/null || echo "Unknown")
            if [ "$STATUS" = "Running" ]; then
                echo -e "   ${GREEN}✅ $pod: Running${NC}"
            else
                echo -e "   ${YELLOW}⚠️  $pod: $STATUS${NC}"
            fi
        done
    else
        echo -e "${RED}❌ Поди не знайдено${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  AWS контекст не знайдено${NC}"
fi

echo ""

# Перевірка моніторингу
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 ПЕРЕВІРКА МОНІТОРИНГУ"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Використати будь-який доступний контекст
if [ -n "$AWS_CTX" ]; then
    kubectl config use-context "$AWS_CTX" > /dev/null 2>&1
elif kubectl config get-contexts -o name | grep -q "andreychyk-bank-cluster"; then
    kubectl config use-context andreychyk-bank-cluster > /dev/null 2>&1
fi

PROM_POD=$(kubectl get pods -n bank-system -l app=prometheus -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")
GRAFANA_POD=$(kubectl get pods -n bank-system -l app=grafana -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")

if [ -n "$PROM_POD" ]; then
    PROM_STATUS=$(kubectl get pod $PROM_POD -n bank-system -o jsonpath='{.status.phase}' 2>/dev/null || echo "Unknown")
    if [ "$PROM_STATUS" = "Running" ]; then
        echo -e "${GREEN}✅ Prometheus: Running ($PROM_POD)${NC}"
    else
        echo -e "${YELLOW}⚠️  Prometheus: $PROM_STATUS${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  Prometheus не знайдено (може не встановлено)${NC}"
fi

if [ -n "$GRAFANA_POD" ]; then
    GRAFANA_STATUS=$(kubectl get pod $GRAFANA_POD -n bank-system -o jsonpath='{.status.phase}' 2>/dev/null || echo "Unknown")
    if [ "$GRAFANA_STATUS" = "Running" ]; then
        echo -e "${GREEN}✅ Grafana: Running ($GRAFANA_POD)${NC}"
    else
        echo -e "${YELLOW}⚠️  Grafana: $GRAFANA_STATUS${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  Grafana не знайдено (може не встановлено)${NC}"
fi

echo ""

# Перевірка додатку
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🌐 ПЕРЕВІРКА ДОДАТКУ"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

APP_URL="https://andreychyk-bank.duckdns.org"

echo "Перевірка доступності..."
if curl -s -o /dev/null -w "%{http_code}" --max-time 10 "$APP_URL" | grep -q "200\|301\|302"; then
    echo -e "${GREEN}✅ Додаток доступний: $APP_URL${NC}"
else
    echo -e "${YELLOW}⚠️  Додаток може бути недоступний: $APP_URL${NC}"
fi

echo ""
echo "Перевірка health endpoint..."
HEALTH=$(curl -s --max-time 5 "$APP_URL/actuator/health" 2>/dev/null || echo "ERROR")
if echo "$HEALTH" | grep -q "UP\|{\"status\":\"UP\""; then
    echo -e "${GREEN}✅ Health check: UP${NC}"
else
    echo -e "${YELLOW}⚠️  Health check: $HEALTH${NC}"
fi

echo ""
echo "Перевірка метрик..."
METRICS=$(curl -s --max-time 5 "$APP_URL/actuator/prometheus" 2>/dev/null | head -1 || echo "")
if [ -n "$METRICS" ] && [ "$METRICS" != "ERROR" ]; then
    echo -e "${GREEN}✅ Метрики експортуються${NC}"
else
    echo -e "${YELLOW}⚠️  Метрики можуть бути недоступні${NC}"
fi

echo ""

# Відновити поточний контекст
if [ -n "$CURRENT_CONTEXT" ]; then
    kubectl config use-context "$CURRENT_CONTEXT" > /dev/null 2>&1
fi

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ ПЕРЕВІРКА ЗАВЕРШЕНА"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📋 Наступні кроки:"
echo "1. Перевірте GitHub Actions: https://github.com/<your-repo>/actions"
echo "2. Перевірте Docker Hub: https://hub.docker.com/r/andreychykviktor/bank-app/tags"
echo "3. Відкрийте додаток: $APP_URL"
echo ""
echo "📖 Детальна інструкція: backend/k8s/HOW_TO_VERIFY.md"

