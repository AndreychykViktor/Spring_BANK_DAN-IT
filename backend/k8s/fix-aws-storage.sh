#!/bin/bash

# Скрипт для виправлення проблем зі Storage в AWS EKS

set -e

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║     🔧 ВИПРАВЛЕННЯ STORAGE ДЛЯ AWS EKS                        ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# Кольори
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Перевірити, чи ми в AWS контексті
CURRENT_CTX=$(kubectl config current-context 2>/dev/null || echo "")
if ! echo "$CURRENT_CTX" | grep -qi eks; then
    echo -e "${YELLOW}⚠️  Поточний контекст не AWS EKS${NC}"
    echo "Перемкніть на AWS контекст:"
    echo "  kubectl config use-context <aws-eks-context>"
    exit 1
fi

echo -e "${GREEN}✅ Поточний контекст: $CURRENT_CTX${NC}"
echo ""

# Перевірити StorageClass
echo "📋 Доступні StorageClass:"
kubectl get storageclass
echo ""

STORAGE_CLASS=$(kubectl get storageclass -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "gp2")
echo -e "${YELLOW}💡 Використовуватиметься StorageClass: $STORAGE_CLASS${NC}"
echo ""

# Видалити старі PVC
echo "🗑️  Видалення старих PVC..."
if kubectl get pvc postgresql-pvc -n bank-system > /dev/null 2>&1; then
    kubectl delete pvc postgresql-pvc -n bank-system
    echo -e "${GREEN}✅ PostgreSQL PVC видалено${NC}"
fi

if kubectl get pvc rabbitmq-storage-rabbitmq-0 -n bank-system > /dev/null 2>&1; then
    kubectl delete pvc rabbitmq-storage-rabbitmq-0 -n bank-system
    echo -e "${GREEN}✅ RabbitMQ PVC видалено${NC}"
fi

echo ""

# Створити тимчасові файли з правильним StorageClass
echo "📝 Створення тимчасових файлів з StorageClass: $STORAGE_CLASS"

# PostgreSQL PVC
cat > /tmp/postgresql-pvc-aws.yaml <<EOF
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgresql-pvc
  namespace: bank-system
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 10Gi
  storageClassName: $STORAGE_CLASS
EOF

# RabbitMQ StatefulSet (потрібно оновити volumeClaimTemplates)
if [ -f "rabbitmq-deployment.yaml" ]; then
    sed "s/storageClassName: managed-csi/storageClassName: $STORAGE_CLASS/g" rabbitmq-deployment.yaml > /tmp/rabbitmq-deployment-aws.yaml
    echo -e "${GREEN}✅ Тимчасові файли створено${NC}"
else
    echo -e "${RED}❌ Файл rabbitmq-deployment.yaml не знайдено${NC}"
    exit 1
fi

echo ""

# Застосувати оновлені манифести
echo "🚀 Застосування оновлених манифестів..."

kubectl apply -f /tmp/postgresql-pvc-aws.yaml
echo -e "${GREEN}✅ PostgreSQL PVC створено${NC}"

kubectl apply -f /tmp/rabbitmq-deployment-aws.yaml
echo -e "${GREEN}✅ RabbitMQ StatefulSet оновлено${NC}"

echo ""

# Очистити тимчасові файли
rm -f /tmp/postgresql-pvc-aws.yaml /tmp/rabbitmq-deployment-aws.yaml

echo "⏳ Очікування на готовність PVC (10 секунд)..."
sleep 10

echo ""
echo "📊 Статус PVC:"
kubectl get pvc -n bank-system

echo ""
echo "📊 Статус подів:"
kubectl get pods -n bank-system

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${GREEN}✅ ВИПРАВЛЕННЯ ЗАВЕРШЕНО${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "💡 Якщо поди все ще в статусі Pending, перевірте:"
echo "   kubectl describe pod <pod-name> -n bank-system"
echo "   kubectl get events -n bank-system --sort-by='.lastTimestamp'"

