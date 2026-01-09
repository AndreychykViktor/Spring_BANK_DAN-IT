# 📝 Файли, які потрібно змінити

Цей документ містить список всіх файлів, які потрібно відредагувати перед деплоєм.

## 🔴 ОБОВ'ЯЗКОВО ЗМІНИТИ

### 1. `k8s/secret.yaml`

**Що змінити:**
- `db-name` - назва вашої Neon бази даних
- `db-user` - користувач Neon бази даних
- `db-password` - пароль Neon бази даних
- `rabbitmq-username` - користувач RabbitMQ
- `rabbitmq-password` - пароль RabbitMQ
- `jwt-secret` - JWT secret (Base64 encoded)

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

---

### 2. `k8s/configmap.yaml`

**Що змінити:**
- `cors-allowed-origins` - ваш домен

**Приклад:**
```yaml
data:
  cors-allowed-origins: "https://mybankapp.com,https://www.mybankapp.com"
```

---

### 3. `k8s/app-deployment.yaml`

**Що змінити:**
- Рядок 20: `image: your-registry/bank-app:latest`

**Для AWS:**
```yaml
image: 123456789012.dkr.ecr.us-east-1.amazonaws.com/bank-app:latest
```

**Для Azure:**
```yaml
image: bankregistry123.azurecr.io/bank-app:latest
```

**ВАЖЛИВО:** Після публікації образу в ECR/ACR оновіть цей файл!

---

### 4. `k8s/aws/ingress.yaml`

**Що змінити:**

1. **Рядок 12** (якщо використовуєте ALB):
```yaml
alb.ingress.kubernetes.io/certificate-arn: "arn:aws:acm:us-east-1:123456789012:certificate/abc123..."
```

2. **Рядок 25** - hostname:
```yaml
- host: aws.mybankapp.com
```

3. **Рядок 33** - TLS hosts:
```yaml
- aws.mybankapp.com
```

**Якщо використовуєте Nginx Ingress (не ALB):**
- Видаліть всі `alb.ingress.kubernetes.io/*` annotations
- Змініть `ingressClassName: alb` на `ingressClassName: nginx`

---

### 5. `k8s/azure/ingress.yaml`

**Що змінити:**

1. **Рядок 20** - hostname:
```yaml
- host: azure.mybankapp.com
```

2. **Рядок 28** - TLS hosts:
```yaml
- azure.mybankapp.com
```

---

## 🟡 ОПЦІОНАЛЬНО (якщо потрібно)

### 6. `k8s/postgresql-deployment.yaml`

**Якщо НЕ використовуєте Neon**, а локальний PostgreSQL:
- Залиште як є

**Якщо використовуєте Neon:**
- Пропустіть деплой PostgreSQL

---

### 7. `k8s/rabbitmq-deployment.yaml` (StatefulSet)

**Примітка:** Цей файл використовує StatefulSet замість Deployment для кращої підтримки RabbitMQ:
- Стабільні імена подів (rabbitmq-0, rabbitmq-1, ...)
- Автоматичне створення PVC через volumeClaimTemplates
- Підтримка кластеризації RabbitMQ

**Якщо використовуєте CloudAMQP замість локального RabbitMQ:**
- Пропустіть деплой RabbitMQ
- Додайте `CLOUDAMQP_URL` в secrets

**Для кластеризації RabbitMQ:**
- Додайте `rabbitmq-erlang-cookie` в `k8s/secret.yaml`
- Збільште `replicas` в `rabbitmq-deployment.yaml` (наприклад, до 3)

---

### 8. `k8s/cert-manager-clusterissuer.yaml` (для HTTPS з Let's Encrypt)

**Якщо використовуєте Nginx Ingress з cert-manager для HTTPS:**

**Що змінити:**
- Рядок 12: `email: your-email@example.com` → ваша реальна email адреса

**Приклад:**
```yaml
email: myemail@gmail.com
```

**Дивіться:** [HTTPS_SETUP.md](./HTTPS_SETUP.md) для повної інструкції з налаштування HTTPS.

---

### 9. `k8s/ingress-nginx.yaml` (для Nginx Ingress з HTTPS)

**Якщо використовуєте Nginx Ingress з cert-manager:**

**Що змінити:**
- Рядок 20: `host: your-domain.duckdns.org` → ваш DuckDNS домен
- Рядок 37: `- your-domain.duckdns.org` → ваш DuckDNS домен

**Приклад:**
```yaml
- host: mybank.duckdns.org
# ...
- mybank.duckdns.org
```

**Дивіться:** [HTTPS_SETUP.md](./HTTPS_SETUP.md) для повної інструкції.

---

## ✅ Чеклист змін

- [ ] `k8s/secret.yaml` - замінено всі credentials
- [ ] `k8s/configmap.yaml` - замінено CORS allowed origins
- [ ] `k8s/app-deployment.yaml` - замінено Docker image (AWS)
- [ ] `k8s/app-deployment.yaml` - замінено Docker image (Azure)
- [ ] `k8s/aws/ingress.yaml` - замінено hostname (якщо використовуєте AWS ALB)
- [ ] `k8s/azure/ingress.yaml` - замінено hostname (якщо використовуєте Azure AG)
- [ ] `k8s/cert-manager-clusterissuer.yaml` - замінено email (якщо використовуєте Nginx Ingress)
- [ ] `k8s/ingress-nginx.yaml` - замінено домен (якщо використовуєте Nginx Ingress)

---

## 🔍 Як перевірити зміни

```bash
# Перевірте secret.yaml (без паролів)
grep -v "password\|secret" k8s/secret.yaml

# Перевірте configmap.yaml
cat k8s/configmap.yaml

# Перевірте app-deployment.yaml
grep "image:" k8s/app-deployment.yaml

# Перевірте ingress файли
grep "host:" k8s/aws/ingress.yaml
grep "host:" k8s/azure/ingress.yaml
```

---

## 📚 Детальна інструкція

Дивіться [STEP_BY_STEP_DEPLOY.md](./STEP_BY_STEP_DEPLOY.md) для повної інструкції.

