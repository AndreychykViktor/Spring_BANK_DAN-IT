#!/bin/bash

# ============================================
# ШАБЛОН КОМАНД ДЛЯ ДЕПЛОЮ
# Замініть всі ЗАГЛАВНІ_ЗНАЧЕННЯ на ваші дані
# ============================================

# ============================================
# КРОК 1: ЗМІННІ СЕРЕДОВИЩА
# ============================================

# Neon Database
export NEON_DB_NAME="ВАШ_NEON_DB_NAME"
export NEON_DB_USER="ВАШ_NEON_DB_USER"
export NEON_DB_PASSWORD="ВАШ_NEON_DB_PASSWORD"
export NEON_CONNECTION_STRING="postgres://ВАШ_USER:ВАШ_PASSWORD@ВАШ_HOST:5432/ВАШ_DB?sslmode=require"

# JWT Secret (генеруйте: echo -n "your-secret" | base64)
export JWT_SECRET="ВАШ_JWT_SECRET_BASE64"

# RabbitMQ
export RABBITMQ_USERNAME="ВАШ_RABBITMQ_USERNAME"
export RABBITMQ_PASSWORD="ВАШ_RABBITMQ_PASSWORD"

# Domain
export YOUR_DOMAIN="ВАШ_ДОМЕН.com"

# AWS
export AWS_CLUSTER_NAME="bank-cluster"
export AWS_REGION="eu-north-1"
export AWS_ACCOUNT_ID="615178206598"

# Azure
export AZURE_RG="bank-rg"
export AZURE_CLUSTER_NAME="bank-cluster"
export AZURE_LOCATION="eastus"
export AZURE_ACR_NAME="ВАШ_ACR_NAME"

# ============================================
# КРОК 2: СТВОРЕННЯ КЛАСТЕРІВ
# ============================================

# AWS EKS
echo "Створення AWS EKS кластера..."
eksctl create cluster \
  --name $AWS_CLUSTER_NAME \
  --region $AWS_REGION \
  --nodegroup-name bank-nodes \
  --node-type t3.medium \
  --nodes 2 \
  --nodes-min 1 \
  --nodes-max 3 \
  --managed

aws eks update-kubeconfig --name $AWS_CLUSTER_NAME --region $AWS_REGION

# Azure AKS
echo "Створення Azure AKS кластера..."
az login
az group create --name $AZURE_RG --location $AZURE_LOCATION
az aks create \
  --resource-group $AZURE_RG \
  --name $AZURE_CLUSTER_NAME \
  --node-count 2 \
  --node-vm-size Standard_B2s \
  --enable-managed-identity \
  --enable-addons monitoring

az aks get-credentials --resource-group $AZURE_RG --name $AZURE_CLUSTER_NAME

# ============================================
# КРОК 3: INGRESS CONTROLLER
# ============================================

# AWS
echo "Встановлення Ingress в AWS..."
aws eks update-kubeconfig --name $AWS_CLUSTER_NAME --region $AWS_REGION
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.1/deploy/static/provider/aws/deploy.yaml
kubectl wait --namespace ingress-nginx --for=condition=ready pod --selector=app.kubernetes.io/component=controller --timeout=300s
echo "AWS Endpoint:"
kubectl get svc -n ingress-nginx

# Azure
echo "Встановлення Ingress в Azure..."
az aks get-credentials --resource-group $AZURE_RG --name $AZURE_CLUSTER_NAME
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.1/deploy/static/provider/cloud/deploy.yaml
kubectl wait --namespace ingress-nginx --for=condition=ready pod --selector=app.kubernetes.io/component=controller --timeout=300s
echo "Azure Endpoint:"
kubectl get svc -n ingress-nginx

# ============================================
# КРОК 4: DOCKER ОБРАЗИ
# ============================================

cd backend

# Збірка
echo "Збірка Docker образу..."
docker build -t bank-app:latest .

# AWS ECR
echo "Публікація в AWS ECR..."
aws ecr create-repository --repository-name bank-app --region $AWS_REGION
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com
docker tag bank-app:latest $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/bank-app:latest
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/bank-app:latest

# Azure ACR
echo "Публікація в Azure ACR..."
az acr create --resource-group $AZURE_RG --name $AZURE_ACR_NAME --sku Basic
az acr login --name $AZURE_ACR_NAME
docker tag bank-app:latest $AZURE_ACR_NAME.azurecr.io/bank-app:latest
docker push $AZURE_ACR_NAME.azurecr.io/bank-app:latest

# ============================================
# КРОК 5: ДЕПЛОЙ В AWS
# ============================================

cd k8s

echo "Деплой в AWS..."
aws eks update-kubeconfig --name $AWS_CLUSTER_NAME --region $AWS_REGION

# Створення secrets
kubectl create namespace bank-system
kubectl create secret generic bank-secrets \
  --from-literal=db-name="$NEON_DB_NAME" \
  --from-literal=db-user="$NEON_DB_USER" \
  --from-literal=db-password="$NEON_DB_PASSWORD" \
  --from-literal=rabbitmq-username="$RABBITMQ_USERNAME" \
  --from-literal=rabbitmq-password="$RABBITMQ_PASSWORD" \
  --from-literal=jwt-secret="$JWT_SECRET" \
  --namespace=bank-system

# ConfigMap
kubectl create configmap bank-config \
  --from-literal=jwt-expiration="3600000" \
  --from-literal=cors-allowed-origins="https://$YOUR_DOMAIN,https://www.$YOUR_DOMAIN" \
  --from-literal=app-port="9000" \
  --namespace=bank-system

# Оновіть app-deployment.yaml з AWS ECR образом перед застосуванням!
# sed -i '' "s|your-registry/bank-app:latest|$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/bank-app:latest|g" app-deployment.yaml

kubectl apply -f namespace.yaml
# Примітка: RabbitMQ використовує StatefulSet, який автоматично створює PVC через volumeClaimTemplates
# kubectl apply -f rabbitmq-pvc.yaml  # Не потрібно для StatefulSet
kubectl apply -f rabbitmq-service.yaml
kubectl apply -f rabbitmq-deployment.yaml
kubectl apply -f app-service.yaml
kubectl apply -f app-deployment.yaml
kubectl apply -f aws/ingress.yaml

# ============================================
# КРОК 6: ДЕПЛОЙ В AZURE
# ============================================

echo "Деплой в Azure..."
az aks get-credentials --resource-group $AZURE_RG --name $AZURE_CLUSTER_NAME

# Створення secrets
kubectl create namespace bank-system
kubectl create secret generic bank-secrets \
  --from-literal=db-name="$NEON_DB_NAME" \
  --from-literal=db-user="$NEON_DB_USER" \
  --from-literal=db-password="$NEON_DB_PASSWORD" \
  --from-literal=rabbitmq-username="$RABBITMQ_USERNAME" \
  --from-literal=rabbitmq-password="$RABBITMQ_PASSWORD" \
  --from-literal=jwt-secret="$JWT_SECRET" \
  --namespace=bank-system

# ConfigMap
kubectl create configmap bank-config \
  --from-literal=jwt-expiration="3600000" \
  --from-literal=cors-allowed-origins="https://$YOUR_DOMAIN,https://www.$YOUR_DOMAIN" \
  --from-literal=app-port="9000" \
  --namespace=bank-system

# Оновіть app-deployment.yaml з Azure ACR образом перед застосуванням!
# sed -i '' "s|your-registry/bank-app:latest|$AZURE_ACR_NAME.azurecr.io/bank-app:latest|g" app-deployment.yaml

kubectl apply -f namespace.yaml
# Примітка: RabbitMQ використовує StatefulSet, який автоматично створює PVC через volumeClaimTemplates
# kubectl apply -f rabbitmq-pvc.yaml  # Не потрібно для StatefulSet
kubectl apply -f rabbitmq-service.yaml
kubectl apply -f rabbitmq-deployment.yaml
kubectl apply -f app-service.yaml
kubectl apply -f app-deployment.yaml
kubectl apply -f azure/ingress.yaml

# ============================================
# КРОК 7: ПЕРЕВІРКА
# ============================================

echo "Перевірка AWS..."
aws eks update-kubeconfig --name $AWS_CLUSTER_NAME --region $AWS_REGION
kubectl get pods -n bank-system
kubectl get ingress -n bank-system

echo "Перевірка Azure..."
az aks get-credentials --resource-group $AZURE_RG --name $AZURE_CLUSTER_NAME
kubectl get pods -n bank-system
kubectl get ingress -n bank-system

echo "Готово! Тепер налаштуйте DNS в Cloudflare."

