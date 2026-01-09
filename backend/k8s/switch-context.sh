#!/bin/bash

# Скрипт для швидкого переключення між AWS EKS та Azure AKS

set -e

echo "🌍 Multi-Cloud Kubernetes Context Switcher"
echo ""

case "$1" in
  aws)
    echo "📦 Переключення на AWS EKS..."
    if [ -z "$2" ]; then
      echo "Використання: ./switch-context.sh aws <cluster-name> <region>"
      echo "Приклад: ./switch-context.sh aws bank-cluster us-east-1"
      exit 1
    fi
    CLUSTER_NAME=${2:-bank-cluster}
    REGION=${3:-us-east-1}
    aws eks update-kubeconfig --name $CLUSTER_NAME --region $REGION
    echo "✅ Переключено на AWS EKS: $CLUSTER_NAME в $REGION"
    ;;
  azure)
    echo "📦 Переключення на Azure AKS..."
    if [ -z "$2" ]; then
      echo "Використання: ./switch-context.sh azure <resource-group> <cluster-name>"
      echo "Приклад: ./switch-context.sh azure bank-rg bank-cluster"
      exit 1
    fi
    RESOURCE_GROUP=${2:-bank-rg}
    CLUSTER_NAME=${3:-bank-cluster}
    az aks get-credentials --resource-group $RESOURCE_GROUP --name $CLUSTER_NAME
    echo "✅ Переключено на Azure AKS: $CLUSTER_NAME в $RESOURCE_GROUP"
    ;;
  current)
    echo "📊 Поточний контекст:"
    kubectl config current-context
    echo ""
    echo "🌐 Доступні контексти:"
    kubectl config get-contexts
    ;;
  *)
    echo "Використання: ./switch-context.sh {aws|azure|current}"
    echo ""
    echo "Приклади:"
    echo "  ./switch-context.sh aws bank-cluster us-east-1"
    echo "  ./switch-context.sh azure bank-rg bank-cluster"
    echo "  ./switch-context.sh current"
    exit 1
    ;;
esac

