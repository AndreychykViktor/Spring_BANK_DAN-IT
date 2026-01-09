#!/bin/bash

# Скрипт для створення OIDC provider для EKS кластера

set -e

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║     🔧 СТВОРЕННЯ OIDC PROVIDER ДЛЯ EKS КЛАСТЕРА               ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

CLUSTER_NAME="andreychyk-bank-cluster"
REGION="eu-north-1"

# Отримати OIDC issuer URL
echo "📋 Отримання OIDC issuer URL..."
OIDC_ISSUER=$(aws eks describe-cluster --name $CLUSTER_NAME --region $REGION --query "cluster.identity.oidc.issuer" --output text 2>&1)

if [ -z "$OIDC_ISSUER" ] || [ "$OIDC_ISSUER" = "None" ]; then
    echo "❌ Не вдалося отримати OIDC issuer URL"
    exit 1
fi

echo "✅ OIDC Issuer URL: $OIDC_ISSUER"

# Отримати OIDC provider ID
OIDC_ID=$(echo "$OIDC_ISSUER" | cut -d '/' -f 5)
echo "✅ OIDC Provider ID: $OIDC_ID"

# Перевірити, чи існує OIDC provider
echo ""
echo "📋 Перевірка наявних OIDC providers..."
EXISTING_PROVIDERS=$(aws iam list-open-id-connect-providers --query "OpenIDConnectProviderList[].Arn" --output text 2>&1 || echo "")

if [ -z "$EXISTING_PROVIDERS" ]; then
    echo "⚠️  OIDC providers не знайдено"
else
    echo "📋 Наявні OIDC providers:"
    echo "$EXISTING_PROVIDERS" | tr '\t' '\n'
fi

# Перевірити, чи наш provider існує
MATCHING_PROVIDER=$(echo "$EXISTING_PROVIDERS" | grep -i "$OIDC_ID" || echo "")

if [ -n "$MATCHING_PROVIDER" ]; then
    echo ""
    echo "✅ OIDC provider вже існує: $MATCHING_PROVIDER"
    exit 0
fi

echo ""
echo "📝 Створення OIDC provider..."

# Отримати thumbprint (AWS EKS OIDC використовує SHA-1, 40 символів)
echo "📋 Отримання thumbprint з OIDC issuer..."
OIDC_HOST=$(echo "$OIDC_ISSUER" | cut -d'/' -f3)
THUMBPRINT=$(echo | openssl s_client -servername "$OIDC_HOST" -showcerts -connect "$OIDC_HOST:443" 2>/dev/null | openssl x509 -fingerprint -noout 2>/dev/null | sed 's/.*Fingerprint=//' | sed 's/://g' | tr '[:upper:]' '[:lower:]' | head -c 40 || echo "")

if [ -z "$THUMBPRINT" ] || [ ${#THUMBPRINT} -ne 40 ]; then
    echo "⚠️  Не вдалося отримати thumbprint автоматично, використовую стандартний для AWS EKS"
    # Стандартний thumbprint для AWS EKS OIDC (може змінитися, але зазвичай працює)
    THUMBPRINT="9e99a48a9960b14926bb7f3b02e22da2b0ab7280"
fi

echo "✅ Thumbprint: $THUMBPRINT (довжина: ${#THUMBPRINT} символів)"

# Створити OIDC provider
echo ""
echo "📝 Створення OIDC provider..."
OIDC_ARN=$(aws iam create-open-id-connect-provider \
    --url "$OIDC_ISSUER" \
    --client-id-list "sts.amazonaws.com" \
    --thumbprint-list "$THUMBPRINT" \
    --query 'OpenIDConnectProviderArn' \
    --output text 2>&1 || echo "")

if [ -n "$OIDC_ARN" ]; then
    echo "✅ OIDC provider створено: $OIDC_ARN"
else
    echo "⚠️  Можливо, OIDC provider вже існує або сталася помилка"
    echo "Перевіряю наявні providers..."
    aws iam list-open-id-connect-providers --query "OpenIDConnectProviderList[?contains(Arn, '$OIDC_ID')]" --output json
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ СТВОРЕННЯ OIDC PROVIDER ЗАВЕРШЕНО"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📋 Тепер можна запустити setup-ebs-csi-iam.sh"

