#!/bin/bash

# Скрипт для отримання KUBECONFIG для GitHub Secrets

set -e

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║     🔐 Отримання KUBECONFIG для GitHub Secrets               ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# Перевірка, чи встановлено необхідні інструменти
command -v kubectl >/dev/null 2>&1 || { echo "❌ kubectl не встановлено"; exit 1; }

# Вибір провайдера
echo "Виберіть провайдера:"
echo "1) Azure AKS"
echo "2) AWS EKS"
echo "3) Локальний kubeconfig (~/.kube/config)"
read -p "Ваш вибір (1-3): " provider

case $provider in
    1)
        echo ""
        echo "📦 Отримання credentials для Azure AKS..."
        
        # Перевірка Azure CLI
        command -v az >/dev/null 2>&1 || { echo "❌ Azure CLI не встановлено. Встановіть: https://docs.microsoft.com/en-us/cli/azure/install-azure-cli"; exit 1; }
        
        read -p "Resource Group: " resource_group
        read -p "Cluster Name: " cluster_name
        
        echo "🔐 Отримання credentials..."
        az aks get-credentials --resource-group "$resource_group" --name "$cluster_name" --overwrite-existing
        
        echo ""
        echo "📝 Створення base64 encoded config..."
        if [[ "$OSTYPE" == "darwin"* ]]; then
            # macOS
            base64_config=$(cat ~/.kube/config | base64)
        else
            # Linux
            base64_config=$(cat ~/.kube/config | base64 -w 0)
        fi
        
        echo ""
        echo "✅ KUBECONFIG готовий для GitHub Secret!"
        echo ""
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "📋 Додайте в GitHub Secrets:"
        echo ""
        echo "Name: KUBECONFIG_AZURE"
        echo "Value: (скопіюйте нижче)"
        echo ""
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "$base64_config"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo ""
        echo "💡 Зберегти в файл? (y/n)"
        read -p "> " save_file
        if [[ "$save_file" == "y" ]]; then
            echo "$base64_config" > azure-kubeconfig.base64.txt
            echo "✅ Збережено в: azure-kubeconfig.base64.txt"
        fi
        ;;
        
    2)
        echo ""
        echo "📦 Отримання credentials для AWS EKS..."
        
        # Перевірка AWS CLI
        command -v aws >/dev/null 2>&1 || { echo "❌ AWS CLI не встановлено. Встановіть: https://aws.amazon.com/cli/"; exit 1; }
        
        read -p "EKS Cluster Name: " cluster_name
        read -p "AWS Region (default: eu-central-1): " region
        region=${region:-eu-central-1}
        
        echo "🔐 Отримання credentials..."
        aws eks update-kubeconfig --name "$cluster_name" --region "$region"
        
        echo ""
        echo "📝 Створення base64 encoded config..."
        if [[ "$OSTYPE" == "darwin"* ]]; then
            # macOS
            base64_config=$(cat ~/.kube/config | base64)
        else
            # Linux
            base64_config=$(cat ~/.kube/config | base64 -w 0)
        fi
        
        echo ""
        echo "✅ KUBECONFIG готовий для GitHub Secret!"
        echo ""
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "📋 Додайте в GitHub Secrets:"
        echo ""
        echo "Name: KUBECONFIG_AWS"
        echo "Value: (скопіюйте нижче)"
        echo ""
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "$base64_config"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo ""
        echo "💡 Зберегти в файл? (y/n)"
        read -p "> " save_file
        if [[ "$save_file" == "y" ]]; then
            echo "$base64_config" > aws-kubeconfig.base64.txt
            echo "✅ Збережено в: aws-kubeconfig.base64.txt"
        fi
        ;;
        
    3)
        echo ""
        echo "📝 Читання локального kubeconfig..."
        
        if [ ! -f ~/.kube/config ]; then
            echo "❌ Файл ~/.kube/config не знайдено"
            exit 1
        fi
        
        echo "📋 Доступні контексти:"
        kubectl config get-contexts
        
        echo ""
        read -p "Використати поточний контекст? (y/n): " use_current
        if [[ "$use_current" != "y" ]]; then
            read -p "Введіть назву контексту: " context_name
            kubectl config use-context "$context_name"
        fi
        
        echo ""
        echo "📝 Створення base64 encoded config..."
        if [[ "$OSTYPE" == "darwin"* ]]; then
            # macOS
            base64_config=$(cat ~/.kube/config | base64)
        else
            # Linux
            base64_config=$(cat ~/.kube/config | base64 -w 0)
        fi
        
        echo ""
        echo "✅ KUBECONFIG готовий для GitHub Secret!"
        echo ""
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "📋 Додайте в GitHub Secrets:"
        echo ""
        echo "Name: KUBECONFIG (або KUBECONFIG_AZURE/KUBECONFIG_AWS)"
        echo "Value: (скопіюйте нижче)"
        echo ""
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "$base64_config"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo ""
        echo "💡 Зберегти в файл? (y/n)"
        read -p "> " save_file
        if [[ "$save_file" == "y" ]]; then
            echo "$base64_config" > kubeconfig.base64.txt
            echo "✅ Збережено в: kubeconfig.base64.txt"
        fi
        ;;
        
    *)
        echo "❌ Невірний вибір"
        exit 1
        ;;
esac

echo ""
echo "📖 Детальні інструкції: backend/k8s/NEXT_STEPS.md"
echo ""

