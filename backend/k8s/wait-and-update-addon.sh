#!/bin/bash

# Скрипт для очікування завершення встановлення addon та оновлення його з IAM role

set -e

CLUSTER_NAME="andreychyk-bank-cluster"
REGION="eu-north-1"
ADDON_NAME="aws-ebs-csi-driver"
ROLE_ARN="arn:aws:iam::615178206598:role/AmazonEKS_EBS_CSI_DriverRole_andreychyk-bank-cluster"

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║     ⏳ ОЧІКУВАННЯ ТА ОНОВЛЕННЯ EBS CSI ADDON                  ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# Очікувати, поки addon стане ACTIVE
echo "⏳ Очікування завершення встановлення addon..."
MAX_WAIT=600  # 10 хвилин максимум
WAITED=0

while [ $WAITED -lt $MAX_WAIT ]; do
    STATUS=$(aws eks describe-addon \
        --cluster-name $CLUSTER_NAME \
        --addon-name $ADDON_NAME \
        --region $REGION \
        --query 'addon.status' \
        --output text 2>&1 || echo "ERROR")
    
    echo "   Статус: $STATUS (чекаємо...)"
    
    if [ "$STATUS" = "ACTIVE" ]; then
        echo "✅ Addon активний!"
        break
    elif [ "$STATUS" = "CREATE_FAILED" ] || [ "$STATUS" = "UPDATE_FAILED" ]; then
        echo "❌ Addon не вдався встановити (status: $STATUS)"
        echo "Перевірте логи:"
        aws eks describe-addon \
            --cluster-name $CLUSTER_NAME \
            --addon-name $ADDON_NAME \
            --region $REGION \
            --query 'addon.health' \
            --output json
        exit 1
    fi
    
    sleep 10
    WAITED=$((WAITED + 10))
done

if [ $WAITED -ge $MAX_WAIT ]; then
    echo "❌ Timeout: addon не завершив встановлення за 10 хвилин"
    exit 1
fi

echo ""
echo "📋 Оновлення addon з IAM role..."
echo "   Role ARN: $ROLE_ARN"

aws eks update-addon \
    --cluster-name $CLUSTER_NAME \
    --addon-name $ADDON_NAME \
    --service-account-role-arn "$ROLE_ARN" \
    --region $REGION \
    --output json > /dev/null 2>&1 || {
    CURRENT_ROLE=$(aws eks describe-addon \
        --cluster-name $CLUSTER_NAME \
        --addon-name $ADDON_NAME \
        --region $REGION \
        --query 'addon.serviceAccountRoleArn' \
        --output text 2>&1 || echo "")
    
    if [ "$CURRENT_ROLE" = "$ROLE_ARN" ]; then
        echo "✅ Addon вже має правильний IAM role"
    else
        echo "❌ Помилка оновлення addon"
        exit 1
    fi
}

echo "✅ Addon оновлено!"

echo ""
echo "⏳ Очікування завершення оновлення..."
sleep 10

echo ""
echo "🔄 Перезапуск CSI controller подів..."
kubectl delete pods -n kube-system -l app=ebs-csi-controller --wait=false 2>/dev/null || true

echo "⏳ Очікування запуску CSI controller подів (30 секунд)..."
sleep 30

echo ""
echo "📊 Статус CSI controller подів:"
kubectl get pods -n kube-system | grep ebs-csi-controller || echo "Поди ще не запустилися"

echo ""
echo "📊 Статус PVC:"
kubectl get pvc -n bank-system || echo "Namespace bank-system не знайдено"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ ПРОЦЕС ЗАВЕРШЕНО"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "💡 Якщо CSI controller поди все ще падають, перевірте:"
echo "   kubectl logs -n kube-system -l app=ebs-csi-controller --tail=50"
echo ""
echo "💡 Якщо PVC все ще Pending, дочекайтеся 1-2 хвилини:"
echo "   kubectl get pvc -n bank-system -w"

