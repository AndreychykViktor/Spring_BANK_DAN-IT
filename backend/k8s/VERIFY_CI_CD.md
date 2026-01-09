# Перевірка та налаштування CI/CD для Multi-Cloud

## ✅ Статус перевірки

### 1. Розмір KUBECONFIG - ЦЕ НОРМАЛЬНО! ✅

**Ваш KUBECONFIG:**
- Оригінальний розмір: ~12KB
- Base64 розмір: ~16KB (~5000 символів)

**Чому це нормально:**
- KUBECONFIG містить сертифікати та credentials для обох кластерів (Azure + AWS)
- Base64 кодування збільшує розмір на ~33%
- Розмір 3-20KB для multi-cluster config - це стандарт

**GitHub Secrets підтримує до 64KB**, тому ваш config повністю поміщається.

### 2. Наявні контексти Kubernetes

З вашого виводу видно:
- ✅ Azure AKS: `andreychyk-bank-cluster` (поточний)
- ✅ AWS EKS: `andreychik.viktor@andreychyk-bank-cluster.eu-north-1.eksctl.io`

## 🔧 Виправлення в CI/CD Pipeline

Виправлено помилки:
1. ✅ Замінено `azure/setup-kubectl@v3` на `kubernetes/setup-kubectl@v3` для AWS
2. ✅ Додано кращу обробку помилок та логування
3. ✅ Додано автоматичне визначення AWS контексту

## 📋 Необхідні GitHub Secrets

### Мінімальна конфігурація (рекомендована):

#### Docker (вже додано ✅):
- `DOCKER_USERNAME`
- `DOCKER_PASSWORD`

#### Kubernetes - Варіант 1: Окремі configs (найбезпечніше):
- `KUBECONFIG_AZURE` - base64 encoded config тільки для Azure
- `KUBECONFIG_AWS` - base64 encoded config тільки для AWS

#### Kubernetes - Варіант 2: Один config з обома контекстами:
- `KUBECONFIG` - base64 encoded config з обома кластерами
- `AZURE_KUBERNETES_CONTEXT` = `andreychyk-bank-cluster`
- `AWS_KUBERNETES_CONTEXT` = `andreychik.viktor@andreychyk-bank-cluster.eu-north-1.eksctl.io`

#### AWS - Варіант 3: AWS Credentials (опціонально, як резерв):
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_REGION` = `eu-north-1`
- `AWS_EKS_CLUSTER_NAME` = `andreychyk-bank-cluster` (назва вашого EKS кластера)

## 🚀 Швидке налаштування

### Крок 1: Отримати контексти

```bash
kubectl config get-contexts
```

Ви побачите:
```
CURRENT   NAME                                                             
          andreychik.viktor@andreychyk-bank-cluster.eu-north-1.eksctl.io   # AWS
*         andreychyk-bank-cluster                                          # Azure
```

### Крок 2: Додати в GitHub Secrets

**Варіант A: Один KUBECONFIG (простіше):**

```bash
# Отримати base64
cat ~/.kube/config | base64

# Додати в GitHub:
# 1. Settings → Secrets → Actions → New repository secret
#    Name: KUBECONFIG
#    Value: <вставте base64 строку>

# 2. Додати назви контекстів:
#    Name: AZURE_KUBERNETES_CONTEXT
#    Value: andreychyk-bank-cluster

#    Name: AWS_KUBERNETES_CONTEXT  
#    Value: andreychik.viktor@andreychyk-bank-cluster.eu-north-1.eksctl.io
```

**Варіант B: Окремі configs (безпечніше):**

```bash
# Для Azure
kubectl config view --minify --context=andreychyk-bank-cluster | base64
# Додати як: KUBECONFIG_AZURE

# Для AWS  
kubectl config view --minify --context=andreychik.viktor@andreychyk-bank-cluster.eu-north-1.eksctl.io | base64
# Додати як: KUBECONFIG_AWS
```

### Крок 3: Перевірити pipeline

```bash
# Створити тестовий commit
git commit --allow-empty -m "test: verify CI/CD multi-cloud"
git push origin main
```

Перевірити в: **Actions** → **CI/CD Pipeline** → переглянути логи jobs:
- ✅ `Build and Test`
- ✅ `Build Docker Image`
- ✅ `Deploy to Azure AKS`
- ✅ `Deploy to AWS EKS` (якщо налаштовано)
- ✅ `Security Scan`

## 🔍 Перевірка після деплою

### Azure AKS:
```bash
kubectl config use-context andreychyk-bank-cluster
kubectl get pods -n bank-system
kubectl get deployment bank-app -n bank-system
```

### AWS EKS:
```bash
kubectl config use-context andreychik.viktor@andreychyk-bank-cluster.eu-north-1.eksctl.io
kubectl get pods -n bank-system
kubectl get deployment bank-app -n bank-system
```

## ❗ Відомі проблеми та виправлення

### Проблема 1: "Set up kubectl" використовує Azure action для AWS
**Виправлено:** Замінено на `kubernetes/setup-kubectl@v3`

### Проблема 2: Немає автоматичного визначення AWS контексту
**Виправлено:** Додано автоматичне визначення за назвою (grep -i eks)

### Проблема 3: Недостатньо логування
**Виправлено:** Додано детальне логування на кожному кроці

## 📊 Структура деплою

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
       ├─────────────┬─────────────┐
       ▼             ▼             ▼
┌─────────────┐ ┌─────────────┐ ┌─────────────┐
│Deploy Azure │ │ Deploy AWS  │ │  Security   │
│    AKS      │ │    EKS      │ │    Scan     │
└─────────────┘ └─────────────┘ └─────────────┘
```

## ✅ Чеклист перевірки

- [ ] Додано `DOCKER_USERNAME` ✅
- [ ] Додано `DOCKER_PASSWORD` ✅
- [ ] Додано `KUBECONFIG` або `KUBECONFIG_AZURE` + `KUBECONFIG_AWS`
- [ ] Додано `AZURE_KUBERNETES_CONTEXT` (якщо використовуєте KUBECONFIG)
- [ ] Додано `AWS_KUBERNETES_CONTEXT` (якщо використовуєте KUBECONFIG)
- [ ] Перевірено, що обидва контексти доступні локально
- [ ] Протестовано pipeline (push в main)
- [ ] Перевірено деплой в Azure
- [ ] Перевірено деплой в AWS (якщо налаштовано)

## 🎉 Готово!

Після виконання всіх кроків ваш CI/CD pipeline буде працювати для обох кластерів!

