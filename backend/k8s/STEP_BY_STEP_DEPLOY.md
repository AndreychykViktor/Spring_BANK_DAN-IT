# 📝 Покрокова інструкція з деплою на AWS + Azure

Ця інструкція містить **конкретні кроки** з усіма необхідними змінами та даними для вставки.

## ⚠️ ВАЖЛИВО: Перед початком

1. Маєте AWS акаунт з $200 кредитами
2. Маєте Azure акаунт з $200 кредитами
3. Маєте домен (наприклад, `mybankapp.com`)
4. Маєте Neon базу даних (connection string)
5. Встановлено: `kubectl`, `aws cli`, `az cli`, `eksctl`, `docker`

---

## 📋 КРОК 1: Підготовка даних

### 1.1. Зберіть всі необхідні дані

Відкрийте блокнот і запишіть:

```
[NEON DATABASE]
Connection String: postgres://user:password@host.neon.tech:5432/database?sslmode=require
DB Name: ________________
DB User: ________________
DB Password: ________________

[JWT SECRET]
JWT Secret (Base64): ________________
(Генеруйте: echo -n "your-secret-key" | base64)

[RABBITMQ]
RabbitMQ Username: ________________
RabbitMQ Password: ________________
(Або використайте CloudAMQP)

[DOMAIN]
Ваш домен: ________________
(Наприклад: mybankapp.com)

[AWS]
AWS Account ID: ________________
AWS Region: ________________ (наприклад: us-east-1)

[AZURE]
Azure Resource Group: ________________ (наприклад: bank-rg)
Azure Location: ________________ (наприклад: eastus)
```

---

## 🚀 КРОК 2: Створення кластерів

### 2.1. AWS EKS кластер

```bash
# Замініть bank-cluster на вашу назву, us-east-1 на ваш регіон
eksctl create cluster \
  --name bank-cluster \
  --region us-east-1 \
  --nodegroup-name bank-nodes \
  --node-type t3.medium \
  --nodes 2 \
  --nodes-min 1 \
  --nodes-max 3 \
  --managed

# Налаштуйте kubectl
aws eks update-kubeconfig --name bank-cluster --region us-east-1

# Перевірте
kubectl get nodes
```

**Запишіть:**
- Cluster name: `bank-cluster`
- Region: `us-east-1`

### 2.2. Azure AKS кластер

```bash
# Увійдіть в Azure
az login

# Створіть resource group (замініть bank-rg на вашу назву, eastus на ваш location)
az group create --name bank-rg --location eastus

# Створіть AKS кластер
az aks create \
  --resource-group bank-rg \
  --name bank-cluster \
  --node-count 2 \
  --node-vm-size Standard_B2s \
  --enable-managed-identity \
  --enable-addons monitoring

# Налаштуйте kubectl
az aks get-credentials --resource-group bank-rg --name bank-cluster

# Перевірте
kubectl get nodes
```

**Запишіть:**
- Resource Group: `bank-rg`
- Cluster name: `bank-cluster`
- Location: `eastus`

---

## 🔧 КРОК 3: Встановлення Ingress Controller

### 3.1. AWS EKS - Nginx Ingress

```bash
# Переключіться на AWS
aws eks update-kubeconfig --name bank-cluster --region us-east-1

# Встановіть Nginx Ingress
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.1/deploy/static/provider/aws/deploy.yaml

# Дочекайтеся готовності (2-3 хвилини)
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=300s

# Отримайте external IP/hostname
kubectl get svc -n ingress-nginx
```

**Запишіть AWS endpoint:**
```
AWS Endpoint: ________________
(Наприклад: a1b2c3d4e5f6g7h8-1234567890.us-east-1.elb.amazonaws.com)
```

### 3.2. Azure AKS - Nginx Ingress

```bash
# Переключіться на Azure
az aks get-credentials --resource-group bank-rg --name bank-cluster

# Встановіть Nginx Ingress
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.1/deploy/static/provider/cloud/deploy.yaml

# Дочекайтеся готовності (2-3 хвилини)
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=300s

# Отримайте external IP
kubectl get svc -n ingress-nginx
```

**Запишіть Azure endpoint:**
```
Azure Endpoint IP: ________________
(Наприклад: 20.123.45.67)
```

---

## 📝 КРОК 4: Зміни в файлах конфігурації

### 4.1. Файл: `k8s/secret.yaml`

**Відкрийте:** `backend/k8s/secret.yaml`

**Замініть:**

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: bank-secrets
  namespace: bank-system
type: Opaque
stringData:
  # Database credentials (NEON)
  db-name: "ВАШ_NEON_DB_NAME"           # ← ЗАМІНІТЬ
  db-user: "ВАШ_NEON_DB_USER"           # ← ЗАМІНІТЬ
  db-password: "ВАШ_NEON_DB_PASSWORD"   # ← ЗАМІНІТЬ
  
  # RabbitMQ credentials
  rabbitmq-username: "ВАШ_RABBITMQ_USERNAME"  # ← ЗАМІНІТЬ (наприклад: guest)
  rabbitmq-password: "ВАШ_RABBITMQ_PASSWORD" # ← ЗАМІНІТЬ
  
  # JWT Secret (Base64 encoded)
  jwt-secret: "ВАШ_JWT_SECRET_BASE64"   # ← ЗАМІНІТЬ
```

**Приклад:**
```yaml
stringData:
  db-name: "neondb"
  db-user: "neondb_owner"
  db-password: "npg_W9IOFPx0EusK"
  rabbitmq-username: "guest"
  rabbitmq-password: "mySecurePassword123"
  jwt-secret: "eW91ci1zdXBlci1zZWNyZXQta2V5LWhlcmU="
```

### 4.2. Файл: `k8s/configmap.yaml`

**Відкрийте:** `backend/k8s/configmap.yaml`

**Замініть:**

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: bank-config
  namespace: bank-system
data:
  jwt-expiration: "3600000"
  cors-allowed-origins: "https://ВАШ_ДОМЕН.com,https://www.ВАШ_ДОМЕН.com"  # ← ЗАМІНІТЬ
  app-port: "9000"
```

**Приклад:**
```yaml
data:
  jwt-expiration: "3600000"
  cors-allowed-origins: "https://mybankapp.com,https://www.mybankapp.com"
  app-port: "9000"
```

### 4.3. Файл: `k8s/app-deployment.yaml`

**Відкрийте:** `backend/k8s/app-deployment.yaml`

**Знайдіть рядок 20:**
```yaml
image: your-registry/bank-app:latest
```

**Замініть на ваш Docker registry:**

**Для AWS ECR:**
```yaml
image: ВАШ_AWS_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/bank-app:latest
```

**Для Azure ACR:**
```yaml
image: вашregistry.azurecr.io/bank-app:latest
```

**Приклад для AWS:**
```yaml
image: 123456789012.dkr.ecr.us-east-1.amazonaws.com/bank-app:latest
```

### 4.4. Файл: `k8s/aws/ingress.yaml`

**Відкрийте:** `backend/k8s/aws/ingress.yaml`

**Замініть:**

1. **Рядок 12** - Certificate ARN (якщо використовуєте ALB):
```yaml
alb.ingress.kubernetes.io/certificate-arn: "arn:aws:acm:ВАШ_РЕГІОН:ВАШ_ACCOUNT_ID:certificate/ВАШ_CERT_ID"
```

2. **Рядок 25** - Hostname:
```yaml
- host: aws.ВАШ_ДОМЕН.com  # ← ЗАМІНІТЬ
```

3. **Рядок 33** - TLS hosts:
```yaml
- aws.ВАШ_ДОМЕН.com  # ← ЗАМІНІТЬ
```

**Приклад:**
```yaml
- host: aws.mybankapp.com
# ...
- aws.mybankapp.com
```

**Якщо використовуєте Nginx Ingress (не ALB):**

Замініть `ingressClassName: alb` на `ingressClassName: nginx` та видаліть ALB annotations.

### 4.5. Файл: `k8s/azure/ingress.yaml`

**Відкрийте:** `backend/k8s/azure/ingress.yaml`

**Замініть:**

1. **Рядок 20** - Hostname:
```yaml
- host: azure.ВАШ_ДОМЕН.com  # ← ЗАМІНІТЬ
```

2. **Рядок 28** - TLS hosts:
```yaml
- azure.ВАШ_ДОМЕН.com  # ← ЗАМІНІТЬ
```

**Приклад:**
```yaml
- host: azure.mybankapp.com
# ...
- azure.mybankapp.com
```

---

## 🐳 КРОК 5: Підготовка Docker образу

### 5.1. Збірка образу

```bash
cd backend

# Збірка
docker build -t bank-app:latest .
```

### 5.2. Публікація в AWS ECR

```bash
# Створіть ECR repository
aws ecr create-repository --repository-name bank-app --region us-east-1

# Отримайте login token
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin ВАШ_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com

# Тег та push
docker tag bank-app:latest ВАШ_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/bank-app:latest
docker push ВАШ_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/bank-app:latest
```

**Замініть:**
- `ВАШ_ACCOUNT_ID` - ваш AWS Account ID
- `us-east-1` - ваш AWS регіон

**Приклад:**
```bash
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 123456789012.dkr.ecr.us-east-1.amazonaws.com
docker tag bank-app:latest 123456789012.dkr.ecr.us-east-1.amazonaws.com/bank-app:latest
docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/bank-app:latest
```

### 5.3. Публікація в Azure ACR

```bash
# Створіть ACR
az acr create --resource-group bank-rg --name вашregistry --sku Basic

# Login
az acr login --name вашregistry

# Тег та push
docker tag bank-app:latest вашregistry.azurecr.io/bank-app:latest
docker push вашregistry.azurecr.io/bank-app:latest
```

**Замініть:**
- `вашregistry` - назва вашого Azure Container Registry (має бути унікальною, тільки маленькі літери та цифри)

**Приклад:**
```bash
az acr create --resource-group bank-rg --name bankregistry123 --sku Basic
az acr login --name bankregistry123
docker tag bank-app:latest bankregistry123.azurecr.io/bank-app:latest
docker push bankregistry123.azurecr.io/bank-app:latest
```

**ВАЖЛИВО:** Після цього оновіть `k8s/app-deployment.yaml` з правильним шляхом до образу!

---

## 🚀 КРОК 6: Деплой в AWS EKS

### 6.1. Переключення на AWS

```bash
cd backend/k8s
./switch-context.sh aws bank-cluster us-east-1
```

**Або вручну:**
```bash
aws eks update-kubeconfig --name bank-cluster --region us-east-1
```

### 6.2. Створення namespace та secrets

```bash
# Namespace
kubectl apply -f namespace.yaml

# Secrets (перевірте, що всі дані замінені!)
kubectl apply -f secret.yaml

# ConfigMap
kubectl apply -f configmap.yaml
```

### 6.3. Деплой PostgreSQL (якщо потрібно локальний)

**Якщо використовуєте Neon - пропустіть цей крок!**

```bash
kubectl apply -f postgresql-pvc.yaml
kubectl apply -f postgresql-service.yaml
kubectl apply -f postgresql-deployment.yaml
```

### 6.4. Деплой RabbitMQ

**Примітка:** RabbitMQ використовує StatefulSet, який автоматично створює PVC через volumeClaimTemplates, тому не потрібно створювати PVC окремо.

```bash
kubectl apply -f rabbitmq-service.yaml
kubectl apply -f rabbitmq-deployment.yaml

# Дочекайтеся готовності
kubectl wait --for=condition=ready pod -l app=rabbitmq -n bank-system --timeout=300s
```

### 6.5. Деплой додатку

```bash
kubectl apply -f app-service.yaml
kubectl apply -f app-deployment.yaml

# Дочекайтеся готовності
kubectl wait --for=condition=ready pod -l app=bank-app -n bank-system --timeout=300s
```

### 6.6. Деплой Ingress

```bash
kubectl apply -f aws/ingress.yaml

# Перевірте
kubectl get ingress -n bank-system
```

### 6.7. Отримання AWS endpoint

```bash
# Для Nginx Ingress
kubectl get svc -n ingress-nginx

# Запишіть EXTERNAL-IP або EXTERNAL-HOSTNAME
```

**Запишіть:**
```
AWS Endpoint: ________________
```

---

## 🚀 КРОК 7: Деплой в Azure AKS

### 7.1. Переключення на Azure

```bash
cd backend/k8s
./switch-context.sh azure bank-rg bank-cluster
```

**Або вручну:**
```bash
az aks get-credentials --resource-group bank-rg --name bank-cluster
```

### 7.2. Створення namespace та secrets

```bash
# Namespace
kubectl apply -f namespace.yaml

# Secrets (перевірте, що всі дані замінені!)
kubectl apply -f secret.yaml

# ConfigMap
kubectl apply -f configmap.yaml
```

### 7.3. Деплой PostgreSQL (якщо потрібно локальний)

**Якщо використовуєте Neon - пропустіть цей крок!**

```bash
kubectl apply -f postgresql-pvc.yaml
kubectl apply -f postgresql-service.yaml
kubectl apply -f postgresql-deployment.yaml
```

### 7.4. Деплой RabbitMQ

**Примітка:** RabbitMQ використовує StatefulSet, який автоматично створює PVC через volumeClaimTemplates, тому не потрібно створювати PVC окремо.

```bash
kubectl apply -f rabbitmq-service.yaml
kubectl apply -f rabbitmq-deployment.yaml

# Дочекайтеся готовності
kubectl wait --for=condition=ready pod -l app=rabbitmq -n bank-system --timeout=300s
```

### 7.5. Деплой додатку

**ВАЖЛИВО:** Оновіть `app-deployment.yaml` з Azure ACR образом!

```bash
# Оновіть образ в app-deployment.yaml
# Замініть AWS ECR на Azure ACR

kubectl apply -f app-service.yaml
kubectl apply -f app-deployment.yaml

# Дочекайтеся готовності
kubectl wait --for=condition=ready pod -l app=bank-app -n bank-system --timeout=300s
```

### 7.6. Деплой Ingress

```bash
kubectl apply -f azure/ingress.yaml

# Перевірте
kubectl get ingress -n bank-system
```

### 7.7. Отримання Azure endpoint

```bash
# Для Nginx Ingress
kubectl get svc -n ingress-nginx

# Запишіть EXTERNAL-IP
```

**Запишіть:**
```
Azure Endpoint IP: ________________
```

---

## 🌐 КРОК 8: Налаштування DNS (Cloudflare)

### 8.1. Додавання DNS записів

1. Увійдіть в [Cloudflare Dashboard](https://dash.cloudflare.com)
2. Виберіть ваш домен
3. Перейдіть в **DNS** → **Records**
4. Додайте A записи:

**AWS Endpoint:**
- Type: `A`
- Name: `@` (або `aws`)
- IPv4: `ВАШ_AWS_ENDPOINT_IP`
- Proxy: Enabled ☁️
- TTL: Auto

**Azure Endpoint:**
- Type: `A`
- Name: `@` (або `azure`)
- IPv4: `ВАШ_AZURE_ENDPOINT_IP`
- Proxy: Enabled ☁️
- TTL: Auto

### 8.2. Налаштування Load Balancing

1. Перейдіть в **Traffic** → **Load Balancing**
2. Натисніть **Create**
3. Заповніть:
   - **Name**: `mybankapp-lb`
   - **Hostname**: `ВАШ_ДОМЕН.com`
   - **TTL**: `30`

4. **Create Pool:**
   - **Name**: `aws-pool`
   - **Origin**: `ВАШ_AWS_ENDPOINT` (IP або hostname)
   - **Health Check**: Enabled
     - **Path**: `/actuator/health`
     - **Interval**: `30s`
     - **Timeout**: `5s`
     - **Retries**: `3`

   - **Name**: `azure-pool`
   - **Origin**: `ВАШ_AZURE_ENDPOINT_IP`
   - **Health Check**: Enabled
     - **Path**: `/actuator/health`
     - **Interval**: `30s`
     - **Timeout**: `5s`
     - **Retries**: `3`

5. **Load Balancing Policy:**
   - **Method**: `Round Robin` (active-active) або `Failover` (primary/secondary)
   - **Weight**: `50/50` (для active-active)

6. **Save**

---

## ✅ КРОК 9: Перевірка та тестування

### 9.1. Перевірка Health Checks

```bash
# AWS
curl https://ВАШ_AWS_ENDPOINT/actuator/health

# Azure
curl https://ВАШ_AZURE_ENDPOINT_IP/actuator/health

# Глобальний домен
curl https://ВАШ_ДОМЕН.com/actuator/health
```

**Очікуваний результат:**
```json
{"status":"UP"}
```

### 9.2. Перевірка статусу подів

**AWS:**
```bash
./switch-context.sh aws bank-cluster us-east-1
kubectl get pods -n bank-system
kubectl get ingress -n bank-system
```

**Azure:**
```bash
./switch-context.sh azure bank-rg bank-cluster
kubectl get pods -n bank-system
kubectl get ingress -n bank-system
```

### 9.3. Перевірка логів

```bash
# AWS
kubectl logs -f deployment/bank-app -n bank-system

# Azure
kubectl logs -f deployment/bank-app -n bank-system
```

### 9.4. Тестування додатку

```bash
# Відкрийте в браузері
https://ВАШ_ДОМЕН.com/index.html
https://ВАШ_ДОМЕН.com/api/test/health
```

---

## 🐛 Вирішення проблем

### Проблема: Поди не запускаються

```bash
# Перевірте опис поду
kubectl describe pod <pod-name> -n bank-system

# Перевірте події
kubectl get events -n bank-system --sort-by='.lastTimestamp'
```

### Проблема: Помилка підключення до БД

```bash
# Перевірте secrets
kubectl get secret bank-secrets -n bank-system -o yaml

# Перевірте логи
kubectl logs deployment/bank-app -n bank-system | grep -i database
```

### Проблема: Ingress не отримує external IP

```bash
# Перевірте Ingress Controller
kubectl get pods -n ingress-nginx
kubectl describe ingress -n bank-system
```

### Проблема: Health check не працює

```bash
# Перевірте endpoint
curl http://your-endpoint/actuator/health

# Перевірте, що SecurityConfig дозволяє /actuator/health
```

---

## 📋 Чеклист

- [ ] Крок 1: Зібрано всі дані
- [ ] Крок 2: Створено AWS EKS кластер
- [ ] Крок 2: Створено Azure AKS кластер
- [ ] Крок 3: Встановлено Ingress в AWS
- [ ] Крок 3: Встановлено Ingress в Azure
- [ ] Крок 4: Змінено `secret.yaml` з правильними даними
- [ ] Крок 4: Змінено `configmap.yaml` з правильним доменом
- [ ] Крок 4: Змінено `app-deployment.yaml` з правильним Docker образом
- [ ] Крок 4: Змінено `aws/ingress.yaml` з правильним hostname
- [ ] Крок 4: Змінено `azure/ingress.yaml` з правильним hostname
- [ ] Крок 5: Зібрано Docker образ
- [ ] Крок 5: Опубліковано образ в AWS ECR
- [ ] Крок 5: Опубліковано образ в Azure ACR
- [ ] Крок 6: Задеплоєно в AWS EKS
- [ ] Крок 7: Задеплоєно в Azure AKS
- [ ] Крок 8: Налаштовано DNS в Cloudflare
- [ ] Крок 9: Перевірено health checks
- [ ] Крок 9: Перевірено роботу додатку

---

## 🎉 Готово!

Ваш додаток тепер працює на AWS та Azure з глобальним DNS балансировщиком!

**URL:** `https://ВАШ_ДОМЕН.com`

---

## 📞 Додаткова допомога

Якщо щось не працює:
1. Перевірте логи: `kubectl logs -f deployment/bank-app -n bank-system`
2. Перевірте статус: `kubectl get pods -n bank-system`
3. Перевірте події: `kubectl get events -n bank-system`

