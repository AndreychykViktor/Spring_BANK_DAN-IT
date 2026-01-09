# Налаштування Multi-Cloud (Azure + AWS)

## Огляд

Цей гайд описує налаштування CI/CD для деплою в обидва кластери Kubernetes: Azure AKS та AWS EKS.

## Структура

```
┌─────────────┐
│   Push      │
└──────┬──────┘
       │
       ▼
┌─────────────┐     ┌─────────────┐
│ Build Image │ ───▶│  Docker Hub │
└──────┬──────┘     └─────────────┘
       │
       ├─────────────┐
       ▼             ▼
┌─────────────┐  ┌─────────────┐
│Deploy Azure │  │ Deploy AWS  │
│    AKS      │  │    EKS      │
└─────────────┘  └─────────────┘
```

## Налаштування Secrets в GitHub

### Обов'язкові Secrets (для обох кластерів)

```
DOCKER_USERNAME          - ваш Docker Hub username
DOCKER_PASSWORD          - ваш Docker Hub password/token
```

### Secrets для Azure AKS

**Варіант 1: Окремий KUBECONFIG для Azure**
```
KUBECONFIG_AZURE         - base64 encoded kubeconfig (тільки Azure)
```

**Варіант 2: Один KUBECONFIG з обома контекстами**
```
KUBECONFIG               - base64 encoded kubeconfig (Azure + AWS)
AZURE_KUBERNETES_CONTEXT - назва контексту Azure (наприклад: andreychyk-bank-cluster)
```

### Secrets для AWS EKS

**Варіант 1: Окремий KUBECONFIG для AWS**
```
KUBECONFIG_AWS           - base64 encoded kubeconfig (тільки AWS)
```

**Варіант 2: AWS Credentials (альтернатива)**
```
AWS_ACCESS_KEY_ID        - AWS Access Key ID
AWS_SECRET_ACCESS_KEY    - AWS Secret Access Key
AWS_REGION               - AWS Region (наприклад: eu-central-1)
AWS_EKS_CLUSTER_NAME     - назва EKS кластера
```

**Варіант 3: Один KUBECONFIG з обома контекстами**
```
KUBECONFIG               - base64 encoded kubeconfig (Azure + AWS)
AWS_KUBERNETES_CONTEXT   - назва контексту AWS
```

## Отримання KUBECONFIG

### Azure AKS

```bash
# Отримати credentials для Azure
az aks get-credentials --resource-group andreychyk-bank --name andreychyk-bank-cluster

# Створити окремий файл для Azure
mkdir -p ~/.kube/contexts
kubectl config view --minify --context=andreychyk-bank-cluster > ~/.kube/contexts/azure-config.yaml

# Encode в base64
cat ~/.kube/contexts/azure-config.yaml | base64 -w 0
```

### AWS EKS

```bash
# Отримати credentials для AWS
aws eks update-kubeconfig --name your-eks-cluster --region eu-central-1

# Створити окремий файл для AWS
kubectl config view --minify --context=arn:aws:eks:region:account:cluster/your-eks-cluster > ~/.kube/contexts/aws-config.yaml

# Encode в base64
cat ~/.kube/contexts/aws-config.yaml | base64 -w 0
```

### Комбінований KUBECONFIG (обидва контексти)

```bash
# Якщо ви використовуєте обидва кластери локально
cat ~/.kube/config | base64 -w 0

# Перевірити доступні контексти
kubectl config get-contexts
```

## Налаштування через GitHub CLI

```bash
# Azure
gh secret set KUBECONFIG_AZURE < ~/.kube/contexts/azure-config.yaml.base64

# AWS
gh secret set KUBECONFIG_AWS < ~/.kube/contexts/aws-config.yaml.base64

# Або комбінований
gh secret set KUBECONFIG < ~/.kube/config.base64
gh secret set AZURE_KUBERNETES_CONTEXT --body "andreychyk-bank-cluster"
gh secret set AWS_KUBERNETES_CONTEXT --body "arn:aws:eks:region:account:cluster/your-eks-cluster"
```

## Створення KUBECONFIG файлів

### Скрипт для створення окремих config файлів

```bash
#!/bin/bash
# save-configs.sh

# Azure
az aks get-credentials --resource-group andreychyk-bank --name andreychyk-bank-cluster --file ~/.kube/azure-config.yaml
kubectl config view --kubeconfig=~/.kube/azure-config.yaml --minify --flatten | base64 -w 0 > azure-config.base64.txt

# AWS
aws eks update-kubeconfig --name your-eks-cluster --region eu-central-1 --kubeconfig ~/.kube/aws-config.yaml
kubectl config view --kubeconfig=~/.kube/aws-config.yaml --minify --flatten | base64 -w 0 > aws-config.base64.txt

echo "✅ Config файли створені:"
echo "   - azure-config.base64.txt"
echo "   - aws-config.base64.txt"
```

## Варіанти деплою

### 1. Деплой в обидва кластери (за замовчуванням)

Pipeline автоматично деплоїть в обидва кластери при push в `main`.

### 2. Деплой тільки в один кластер

Змініть умову в `.github/workflows/ci-cd.yml`:

```yaml
deploy-azure:
  if: github.ref == 'refs/heads/main' && github.event.inputs.deploy_azure == 'true'
```

### 3. Ручний вибір кластера

Додайте `workflow_dispatch` в `on:` секцію:

```yaml
on:
  workflow_dispatch:
    inputs:
      deploy_to_azure:
        type: boolean
        default: true
      deploy_to_aws:
        type: boolean
        default: true
```

## Перевірка налаштувань

### Перевірка Azure

```bash
# Локально
kubectl --context=andreychyk-bank-cluster get pods -n bank-system

# В GitHub Actions - перевірте логи job "Deploy to Azure AKS"
```

### Перевірка AWS

```bash
# Локально
kubectl --context=<aws-context> get pods -n bank-system

# В GitHub Actions - перевірте логи job "Deploy to AWS EKS"
```

## Troubleshooting

### ❌ Помилка: "KUBECONFIG secret не знайдено"

**Рішення:** 
- Перевірте, що додано принаймні один з: `KUBECONFIG_AZURE`, `KUBECONFIG_AWS`, або `KUBECONFIG`
- Якщо використовуєте комбінований `KUBECONFIG`, додайте також `AZURE_KUBERNETES_CONTEXT` або `AWS_KUBERNETES_CONTEXT`

### ❌ Помилка: "context not found"

**Рішення:**
- Перевірте правильність назви контексту: `kubectl config get-contexts`
- Переконайтеся, що context існує в kubeconfig

### ❌ Помилка AWS: "Unable to locate credentials"

**Рішення:**
- Перевірте `AWS_ACCESS_KEY_ID` та `AWS_SECRET_ACCESS_KEY`
- Перевірте `AWS_REGION`
- Переконайтеся, що credentials мають права доступу до EKS

### ❌ Deployment не знайдено в кластері

**Рішення:**
- Переконайтеся, що deployment існує: `kubectl get deployment -n bank-system`
- Перевірте namespace: `kubectl get namespaces`
- Можливо потрібно спочатку застосувати манифести: `kubectl apply -f k8s/`

## Best Practices

1. ✅ Використовуйте окремі KUBECONFIG для кожного кластера (безпечніше)
2. ✅ Додайте AWS credentials як backup метод
3. ✅ Регулярно оновлюйте secrets (токени можуть прострочитися)
4. ✅ Використовуйте environment-specific secrets для різних середовищ
5. ✅ Додайте алерти для моніторингу деплоїв

## Приклад конфігурації

### Мінімальна конфігурація (рекомендована)

```
Secrets в GitHub:
├── DOCKER_USERNAME
├── DOCKER_PASSWORD
├── KUBECONFIG_AZURE (base64)
└── KUBECONFIG_AWS (base64)
```

### Альтернативна конфігурація

```
Secrets в GitHub:
├── DOCKER_USERNAME
├── DOCKER_PASSWORD
├── KUBECONFIG (base64 - обидва контексти)
├── AZURE_KUBERNETES_CONTEXT
├── AWS_KUBERNETES_CONTEXT
├── AWS_ACCESS_KEY_ID
├── AWS_SECRET_ACCESS_KEY
└── AWS_REGION
```

## Додаткові ресурси

- [Azure AKS Documentation](https://docs.microsoft.com/en-us/azure/aks/)
- [AWS EKS Documentation](https://docs.aws.amazon.com/eks/)
- [Kubernetes Contexts](https://kubernetes.io/docs/tasks/access-application-cluster/configure-access-multiple-clusters/)

