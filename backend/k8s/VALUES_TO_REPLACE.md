# 🔄 Значення для заміни

Скопіюйте цю таблицю, заповніть свої значення та використовуйте для заміни в файлах.
jqvXsm6vKGnfIsZMa7nBAbzUhOts1AUJ7ocLtbDO  Secret access key
AKIAY6O32PWDPC6SMPUI   Access key
## 📋 Таблиця значень

| Змінна | Ваше значення       | Де використовується |
|--------|---------------------|---------------------|
| **NEON_DB_NAME** | `________________`  | `secret.yaml` (db-name) |
| **NEON_DB_USER** | `________________`  | `secret.yaml` (db-user) |
| **NEON_DB_PASSWORD** | `________________`  | `secret.yaml` (db-password) |
| **JWT_SECRET_BASE64** | `________________`  | `secret.yaml` (jwt-secret) |
| **RABBITMQ_USERNAME** | `________________`  | `secret.yaml` (rabbitmq-username) |
| **RABBITMQ_PASSWORD** | `________________`  | `secret.yaml` (rabbitmq-password) |
| **YOUR_DOMAIN** | `________________`  | `configmap.yaml`, `ingress.yaml` |
| **AWS_ACCOUNT_ID** | `___615178206598__` | `app-deployment.yaml`, ECR commands |
| **AWS_REGION** | `_____eu-north-1__` | EKS commands, ECR commands |
| **AWS_CLUSTER_NAME** | `________________`  | EKS commands |
| **AZURE_RG** | `_andreychyk-bank___`  | AKS commands |
| **AZURE_CLUSTER_NAME** | `________________`  | AKS commands |
| **AZURE_LOCATION** | `__Poland Central __`  | AKS commands |
| **AZURE_ACR_NAME** | `________________`  | `app-deployment.yaml`, ACR commands |
| **AWS_ENDPOINT** | `________________`  | DNS налаштування |
| **AZURE_ENDPOINT_IP** | `________________`  | DNS налаштування |

---

## 📝 Як заповнити

### 1. Neon Database

**Де знайти:**
- Увійдіть на [console.neon.tech](https://console.neon.tech)
- Виберіть ваш проект
- Скопіюйте Connection String
- Розберіть на компоненти:
  ```
  postgres://USER:PASSWORD@HOST:5432/DATABASE?sslmode=require
  ```

**Приклад:**
```
NEON_DB_NAME: neondb
NEON_DB_USER: neondb_owner
NEON_DB_PASSWORD: npg_W9IOFPx0EusK
```

---

### 2. JWT Secret

**Як згенерувати:**
```bash
echo -n "your-super-secret-key-here" | base64
```

**Приклад:**
```
JWT_SECRET_BASE64: eW91ci1zdXBlci1zZWNyZXQta2V5LWhlcmU=
```

---

### 3. RabbitMQ

**Якщо використовуєте локальний RabbitMQ:**
```
RABBITMQ_USERNAME: guest
RABBITMQ_PASSWORD: your-secure-password
```

**Якщо використовуєте CloudAMQP:**
- Отримайте credentials з CloudAMQP dashboard
- Або використайте безкоштовний план

---

### 4. Domain

**Ваш домен:**
```
YOUR_DOMAIN: mybankapp.com
```

**Без www:**
```
YOUR_DOMAIN: mybankapp.com
```

**З www:**
```
YOUR_DOMAIN: www.mybankapp.com
```

---

### 5. AWS

**Account ID:**
```bash
aws sts get-caller-identity --query Account --output text
```

**Region:**
- Виберіть найближчий регіон
- Приклад: `us-east-1`, `eu-west-1`, `ap-southeast-1`

**Cluster Name:**
```
AWS_CLUSTER_NAME: bank-cluster
```

---

### 6. Azure

**Resource Group:**
```
AZURE_RG: bank-rg
```

**Cluster Name:**
```
AZURE_CLUSTER_NAME: bank-cluster
```

**Location:**
- Виберіть найближчий location
- Приклад: `eastus`, `westeurope`, `southeastasia`

**ACR Name:**
- Має бути унікальним (тільки маленькі літери та цифри)
- Приклад: `bankregistry123`

---

### 7. Endpoints (після деплою)

**AWS Endpoint:**
```bash
kubectl get svc -n ingress-nginx
# Запишіть EXTERNAL-IP або EXTERNAL-HOSTNAME
```

**Azure Endpoint:**
```bash
kubectl get svc -n ingress-nginx
# Запишіть EXTERNAL-IP
```

---

## 🔍 Швидка перевірка

Після заповнення перевірте:

```bash
# Перевірте, що всі змінні заповнені
cat VALUES_TO_REPLACE.md | grep "________________"

# Якщо є порожні - заповніть їх!
```

---

## 📚 Використання

1. Заповніть таблицю вище
2. Використовуйте значення для заміни в файлах (див. `FILES_TO_EDIT.md`)
3. Використовуйте значення в командах (див. `COMMANDS_TEMPLATE.sh`)

---

## ⚠️ БЕЗПЕКА

**НЕ комітьте цей файл з реальними паролями в Git!**

Після заповнення:
```bash
# Додайте в .gitignore
echo "VALUES_TO_REPLACE.md" >> .gitignore
```

Або використовуйте окремий файл:
```bash
cp VALUES_TO_REPLACE.md MY_VALUES.md
# Заповніть MY_VALUES.md
# MY_VALUES.md вже в .gitignore
```

