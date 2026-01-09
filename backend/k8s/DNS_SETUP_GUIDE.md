# 🌐 Налаштування глобального DNS балансировщика

Цей гайд допоможе налаштувати глобальний DNS балансировщик для маршрутизації трафіку між AWS EKS та Azure AKS.

## 📋 Передумови

1. Два endpoints (AWS та Azure) вже працюють
2. Health check endpoint доступний: `/actuator/health` або `/api/test/health`
3. Домен налаштований

## 🎯 Варіанти налаштування

### Варіант 1: Cloudflare (рекомендовано для старту) ⭐

**Переваги:**
- ✅ Найпростіше налаштування
- ✅ Безкоштовний Load Balancing (на платних тарифах)
- ✅ DDoS захист
- ✅ SSL/TLS автоматично

**Крок 1: Додайте DNS записи**

1. Увійдіть в [Cloudflare Dashboard](https://dash.cloudflare.com)
2. Виберіть ваш домен
3. Перейдіть в **DNS** → **Records**
4. Додайте A записи:

**AWS Endpoint:**
- Type: `A`
- Name: `@` (або `mybankapp`)
- IPv4: `AWS_ENDPOINT_IP` (з `kubectl get svc -n ingress-nginx`)
- Proxy: Enabled (оранжева хмара) ☁️
- TTL: Auto

**Azure Endpoint:**
- Type: `A`
- Name: `@` (або `mybankapp`)
- IPv4: `AZURE_ENDPOINT_IP`
- Proxy: Enabled (оранжева хмара) ☁️
- TTL: Auto

**Крок 2: Налаштуйте Load Balancing**

1. Перейдіть в **Traffic** → **Load Balancing**
2. Натисніть **Create**
3. Заповніть форму:
   - **Name**: `mybankapp-lb`
   - **Hostname**: `mybankapp.com` (або `www.mybankapp.com`)
   - **TTL**: `30`

4. **Create Pool:**
   - **Name**: `aws-pool`
   - **Origin**: `aws-endpoint.elb.amazonaws.com` (або IP)
   - **Health Check**: Enabled
     - **Path**: `/actuator/health`
     - **Interval**: `30s`
     - **Timeout**: `5s`
     - **Retries**: `3`

   - **Name**: `azure-pool`
   - **Origin**: `azure-endpoint.azure.com` (або IP)
   - **Health Check**: Enabled
     - **Path**: `/actuator/health`
     - **Interval**: `30s`
     - **Timeout**: `5s`
     - **Retries**: `3`

5. **Налаштуйте Load Balancing Policy:**
   - **Method**: `Round Robin` (для active-active) або `Failover` (для primary/secondary)
   - **Weight**: `50/50` (для active-active)

6. **Save**

**Крок 3: Перевірка**

```bash
# Перевірте health checks
curl https://mybankapp.com/actuator/health

# Перевірте, що трафік розподіляється
for i in {1..10}; do curl -s https://mybankapp.com/api/test/health | grep -o "ok"; done
```

---

### Варіант 2: AWS Route 53

**Переваги:**
- ✅ Інтеграція з AWS
- ✅ Health checks
- ✅ Failover та Weighted routing

**Крок 1: Створення Health Checks**

```bash
# Health check для AWS
aws route53 create-health-check \
  --caller-reference $(date +%s) \
  --health-check-config '{
    "Type": "HTTPS",
    "ResourcePath": "/actuator/health",
    "FullyQualifiedDomainName": "aws-endpoint.elb.amazonaws.com",
    "Port": 443,
    "RequestInterval": 30,
    "FailureThreshold": 3,
    "MeasureLatency": true
  }'

# Запишіть HealthCheckId з виводу

# Health check для Azure
aws route53 create-health-check \
  --caller-reference $(date +%s) \
  --health-check-config '{
    "Type": "HTTPS",
    "ResourcePath": "/actuator/health",
    "FullyQualifiedDomainName": "azure-endpoint.azure.com",
    "Port": 443,
    "RequestInterval": 30,
    "FailureThreshold": 3,
    "MeasureLatency": true
  }'
```

**Крок 2: Створення DNS записів**

**Failover (Primary/Secondary):**

```bash
# Отримайте Hosted Zone ID
aws route53 list-hosted-zones-by-name --dns-name mybankapp.com

# Primary (AWS)
aws route53 change-resource-record-sets \
  --hosted-zone-id YOUR_ZONE_ID \
  --change-batch '{
    "Changes": [{
      "Action": "UPSERT",
      "ResourceRecordSet": {
        "Name": "mybankapp.com",
        "Type": "A",
        "SetIdentifier": "aws-primary",
        "Failover": "PRIMARY",
        "TTL": 60,
        "ResourceRecords": [{"Value": "AWS_ENDPOINT_IP"}],
        "HealthCheckId": "AWS_HEALTH_CHECK_ID"
      }
    }]
  }'

# Secondary (Azure)
aws route53 change-resource-record-sets \
  --hosted-zone-id YOUR_ZONE_ID \
  --change-batch '{
    "Changes": [{
      "Action": "UPSERT",
      "ResourceRecordSet": {
        "Name": "mybankapp.com",
        "Type": "A",
        "SetIdentifier": "azure-secondary",
        "Failover": "SECONDARY",
        "TTL": 60,
        "ResourceRecords": [{"Value": "AZURE_ENDPOINT_IP"}],
        "HealthCheckId": "AZURE_HEALTH_CHECK_ID"
      }
    }]
  }'
```

**Active-Active (Weighted):**

```bash
# AWS (50%)
aws route53 change-resource-record-sets \
  --hosted-zone-id YOUR_ZONE_ID \
  --change-batch '{
    "Changes": [{
      "Action": "UPSERT",
      "ResourceRecordSet": {
        "Name": "mybankapp.com",
        "Type": "A",
        "SetIdentifier": "aws-weighted",
        "Weight": 50,
        "TTL": 60,
        "ResourceRecords": [{"Value": "AWS_ENDPOINT_IP"}],
        "HealthCheckId": "AWS_HEALTH_CHECK_ID"
      }
    }]
  }'

# Azure (50%)
aws route53 change-resource-record-sets \
  --hosted-zone-id YOUR_ZONE_ID \
  --change-batch '{
    "Changes": [{
      "Action": "UPSERT",
      "ResourceRecordSet": {
        "Name": "mybankapp.com",
        "Type": "A",
        "SetIdentifier": "azure-weighted",
        "Weight": 50,
        "TTL": 60,
        "ResourceRecords": [{"Value": "AZURE_ENDPOINT_IP"}],
        "HealthCheckId": "AZURE_HEALTH_CHECK_ID"
      }
    }]
  }'
```

---

### Варіант 3: Azure Traffic Manager

**Переваги:**
- ✅ Інтеграція з Azure
- ✅ Health checks
- ✅ Priority, Weighted, Geographic routing

**Крок 1: Створення Traffic Manager Profile**

```bash
az network traffic-manager profile create \
  --resource-group bank-rg \
  --name mybankapp-tm \
  --routing-method Priority \
  --unique-dns-name mybankapp \
  --ttl 30 \
  --protocol HTTPS \
  --port 443 \
  --path "/actuator/health"
```

**Крок 2: Додавання Endpoints**

```bash
# AWS endpoint (Priority 1)
az network traffic-manager endpoint create \
  --resource-group bank-rg \
  --profile-name mybankapp-tm \
  --name aws-endpoint \
  --type externalEndpoints \
  --target "aws-endpoint.elb.amazonaws.com" \
  --priority 1 \
  --weight 1 \
  --endpoint-status Enabled

# Azure endpoint (Priority 2)
az network traffic-manager endpoint create \
  --resource-group bank-rg \
  --profile-name mybankapp-tm \
  --name azure-endpoint \
  --type externalEndpoints \
  --target "azure-endpoint.azure.com" \
  --priority 2 \
  --weight 1 \
  --endpoint-status Enabled
```

**Крок 3: Налаштування DNS**

Додайте CNAME запис у вашому DNS провайдері:
```
mybankapp.com -> mybankapp.trafficmanager.net
```

---

## 🔍 Перевірка та тестування

### 1. Перевірка Health Checks

```bash
# AWS
curl https://aws-endpoint.elb.amazonaws.com/actuator/health

# Azure
curl https://azure-endpoint.azure.com/actuator/health

# Глобальний домен
curl https://mybankapp.com/actuator/health
```

### 2. Тестування Failover

```bash
# "Уроніть" AWS endpoint
kubectl scale deployment bank-app --replicas=0 -n bank-system --context=aws

# Перевірте, що трафік переключився на Azure
curl https://mybankapp.com/actuator/health

# Відновіть AWS
kubectl scale deployment bank-app --replicas=2 -n bank-system --context=aws
```

### 3. Тестування Active-Active

```bash
# Перевірте розподіл трафіку
for i in {1..20}; do 
  curl -s https://mybankapp.com/api/test/health | jq -r '.status'
done | sort | uniq -c
```

## 💰 Вартість

- **Cloudflare:** Безкоштовно (на платних тарифах Load Balancing)
- **Route 53:** ~$0.50/місяць за health check + $0.40/мільйон запитів
- **Traffic Manager:** ~$2/місяць + $0.01/мільйон запитів

## 📚 Корисні посилання

- [Cloudflare Load Balancing](https://developers.cloudflare.com/load-balancing/)
- [AWS Route 53 Health Checks](https://docs.aws.amazon.com/Route53/latest/DeveloperGuide/dns-failover.html)
- [Azure Traffic Manager](https://docs.microsoft.com/azure/traffic-manager/)

---

**Готово!** Ваш глобальний DNS балансировщик налаштовано! 🎉

