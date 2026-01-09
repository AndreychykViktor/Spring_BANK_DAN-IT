#!/bin/bash

# Скрипт для застосування всіх Kubernetes маніфестів
# Використання: ./apply-all.sh

set -e

echo "🚀 Початок деплою банківського додатку на Kubernetes..."

# Перевірка наявності kubectl
if ! command -v kubectl &> /dev/null; then
    echo "❌ kubectl не знайдено. Будь ласка, встановіть kubectl."
    exit 1
fi

# Створення namespace
echo "📦 Створення namespace..."
kubectl apply -f namespace.yaml

# Створення secrets та configmap
echo "🔐 Створення secrets та configmap..."
kubectl apply -f secret.yaml
kubectl apply -f configmap.yaml

# Створення PersistentVolumeClaims
echo "💾 Створення PersistentVolumeClaims..."
kubectl apply -f postgresql-pvc.yaml
# Примітка: RabbitMQ використовує StatefulSet, який автоматично створює PVC через volumeClaimTemplates

# Деплой PostgreSQL
echo "🗄️  Деплой PostgreSQL..."
kubectl apply -f postgresql-service.yaml
kubectl apply -f postgresql-deployment.yaml

# Деплой RabbitMQ
echo "🐰 Деплой RabbitMQ..."
kubectl apply -f rabbitmq-service.yaml
kubectl apply -f rabbitmq-deployment.yaml

# Очікування готовності PostgreSQL та RabbitMQ
echo "⏳ Очікування готовності PostgreSQL та RabbitMQ..."
kubectl wait --for=condition=ready pod -l app=postgresql -n bank-system --timeout=300s
kubectl wait --for=condition=ready pod -l app=rabbitmq -n bank-system --timeout=300s

# Деплой додатку
echo "🚀 Деплой додатку..."
kubectl apply -f app-service.yaml
kubectl apply -f app-deployment.yaml

# Деплой Ingress
echo "🌐 Деплой Ingress..."
# Примітка: Використайте один з варіантів:
# - ingress-nginx.yaml (для Nginx Ingress з cert-manager/Let's Encrypt)
# - aws/ingress.yaml (для AWS ALB)
# - azure/ingress.yaml (для Azure Application Gateway)
if [ -f "ingress-nginx.yaml" ]; then
    kubectl apply -f ingress-nginx.yaml
elif [ -f "ingress.yaml" ]; then
    kubectl apply -f ingress.yaml
else
    echo "⚠️  Ingress файл не знайдено. Будь ласка, використайте один з: ingress-nginx.yaml, aws/ingress.yaml, або azure/ingress.yaml"
fi

echo "✅ Деплой завершено!"
echo ""
echo "📊 Статус подів:"
kubectl get pods -n bank-system

echo ""
echo "🌐 Статус сервісів:"
kubectl get svc -n bank-system

echo ""
echo "🔗 Для перевірки логів використайте:"
echo "   kubectl logs -f deployment/bank-app -n bank-system"

