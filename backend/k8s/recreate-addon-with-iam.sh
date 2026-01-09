#!/bin/bash

# Скрипт для видалення та перестворення EBS CSI addon з IAM role

set -e

CLUSTER_NAME="andreychyk-bank-cluster"
REGION="eu-north-1"
ADDON_NAME="aws-ebs-csi-driver"
ROLE_ARN="arn:aws:iam::615178206598:role/AmazonEKS_EBS_CSI_DriverRole_andreychyk-bank-cluster"

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║     🔄 ПЕРЕСТВОРЕННЯ EBS CSI ADDON З IAM ROLE                  ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# Перевірити поточний статус
CURRENT_STATUS=$(aws eks describe-addon \
    --cluster-name $CLUSTER_NAME \
    --addon-name $ADDON_NAME \
    --region $REGION \
    --query 'addon.status' \
    --output text 2>&1 || echo "NOT_FOUND")

echo "📋 Поточний статус addon: $CURRENT_STATUS"
echo ""

# Видалити addon якщо існує
if [ "$CURRENT_STATUS" != "NOT_FOUND" ]; then
    echo "🗑️  Видалення існуючого addon..."
    aws eks delete-addon \
        --cluster-name $CLUSTER_NAME \
        --addon-name $ADDON_NAME \
        --region $REGION \
        --output text > /dev/null 2>&1 || {
        echo "⚠️  Помилка видалення addon (може не існувати або в стані DELETING)"
    }
    
    echo "⏳ Очікування видалення addon (30 секунд)..."
    sleep 30
    
    # Перевірити, чи видалено
    DELETE_STATUS=$(aws eks describe-addon \
        --cluster-name $CLUSTER_NAME \
        --addon-name $ADDON_NAME \
        --region $REGION \
        --query 'addon.status' \
        --output text 2>&1 || echo "NOT_FOUND")
    
    if [ "$DELETE_STATUS" != "NOT_FOUND" ]; then
        echo "⚠️  Addon все ще існує (status: $DELETE_STATUS), дочекаємося..."
        echo "⏳ Очікування додаткових 30 секунд..."
        sleep 30
    fi
    
    echo "✅ Addon видалено або не існує"
fi

echo ""
echo "📝 Створення addon з IAM role..."
echo "   Role ARN: $ROLE_ARN"

aws eks create-addon \
    --cluster-name $CLUSTER_NAME \
    --addon-name $ADDON_NAME \
    --service-account-role-arn "$ROLE_ARN" \
    --region $REGION \
    --output json > /dev/null 2>&1 || {
    echo "❌ Помилка створення addon"
    exit 1
}

echo "✅ Addon створено з IAM role!"

echo ""
echo "⏳ Очікування завершення встановлення addon..."
MAX_WAIT=600  # 10 хвилин
WAITED=0

while [ $WAITED -lt $MAX_WAIT ]; do
    STATUS=$(aws eks describe-addon \
        --cluster-name $CLUSTER_NAME \
        --addon-name $ADDON_NAME \
        --region $REGION \
        --query 'addon.status' \
        --output text 2>&1 || echo "ERROR")
    
    # Показувати прогрес кожні 30 секунд
    if [ $((WAITED % 30)) -eq 0 ]; then
        echo "   Статус: $STATUS (чекаємо... ${WAITED}s/${MAX_WAIT}s)"
    fi
    
    if [ "$STATUS" = "ACTIVE" ]; then
        echo ""
        echo "✅ Addon активний!"
        break
    elif [ "$STATUS" = "CREATE_FAILED" ] || [ "$STATUS" = "UPDATE_FAILED" ]; then
        echo ""
        echo "❌ Addon не вдався встановити (status: $STATUS)"
        echo "Перевірте health issues:"
        aws eks describe-addon \
            --cluster-name $CLUSTER_NAME \
            --addon-name $ADDON_NAME \
            --region $REGION \
            --query 'addon.health' \
            --output json
        exit 1
    fi
    
    sleep 5
    WAITED=$((WAITED + 5))
done

if [ $WAITED -ge $MAX_WAIT ]; then
    echo ""
    echo "⚠️  Timeout: addon не завершив встановлення за 10 хвилин"
    echo "Але продовжуємо - перевіримо статус CSI controller подів"
fi

echo ""
echo "⏳ Очікування запуску CSI controller подів (30 секунд)..."
sleep 30

echo ""
echo "📊 Статус CSI controller подів:"
kubectl get pods -n kube-system | grep ebs-csi-controller || echo "Поди ще не запустилися"

echo ""
echo "📊 Статус PVC:"
kubectl get pvc -n bank-system 2>/dev/null || echo "Namespace bank-system не знайдено"

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

