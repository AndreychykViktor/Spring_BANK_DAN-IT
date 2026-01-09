# Наступні кроки після налаштування CI/CD

## ✅ Що вже зроблено

- [x] Додано `DOCKER_USERNAME` в GitHub Secrets
- [x] Додано `DOCKER_PASSWORD` в GitHub Secrets

## 📋 Наступні кроки

### Крок 1: Додати Kubernetes Secrets для деплою

#### Для Azure AKS:

1. **Отримати KUBECONFIG для Azure:**

```bash
# Увійдіть в Azure
az login

# Отримати credentials
az aks get-credentials --resource-group andreychyk-bank --name andreychyk-bank-cluster

# Створити base64 encoded config
cat ~/.kube/config | base64 -w 0
# На macOS:
cat ~/.kube/config | base64
```

2. **Додати Secret в GitHub:**

Перейдіть: **Settings → Secrets and variables → Actions → New repository secret**

```
Name: KUBECONFIG_AZURE
Value: <вставте base64 строку з попереднього кроку>
```

#### Для AWS EKS (якщо використовуєте):

**Варіант 1: KUBECONFIG**

```bash
# Отримати credentials
aws eks update-kubeconfig --name your-eks-cluster --region eu-central-1

# Створити base64 encoded config
cat ~/.kube/config | base64 -w 0
# На macOS:
cat ~/.kube/config | base64
```

Додати в GitHub:
```
Name: KUBECONFIG_AWS
Value: <вставте base64 строку>
```

**Варіант 2: AWS Credentials (альтернатива)**

```
Name: AWS_ACCESS_KEY_ID
Value: ваш_aws_access_key

Name: AWS_SECRET_ACCESS_KEY
Value: ваш_aws_secret_key

Name: AWS_REGION
Value: eu-central-1

Name: AWS_EKS_CLUSTER_NAME
Value: назва_вашого_eks_кластера
```

### Крок 2: Перевірити налаштування

1. **Перевірити, що всі secrets додані:**

```bash
# Перевірити через GitHub CLI (якщо встановлено)
gh secret list

# Або перевірити вручну в GitHub UI:
# Settings → Secrets and variables → Actions
```

**Мінімальний набір secrets:**
- ✅ `DOCKER_USERNAME`
- ✅ `DOCKER_PASSWORD`
- ⏳ `KUBECONFIG_AZURE` (або `KUBECONFIG` + `AZURE_KUBERNETES_CONTEXT`)
- ⏳ `KUBECONFIG_AWS` (опціонально, якщо використовуєте AWS)

### Крок 3: Тестувати CI/CD Pipeline

1. **Зробити тестовий commit:**

```bash
# Створити тестовий файл
echo "# CI/CD Test" >> backend/TEST.md

# Commit та push
git add backend/TEST.md
git commit -m "test: CI/CD pipeline"
git push origin main
```

2. **Перевірити виконання:**

- Перейдіть в **Actions** tab в GitHub
- Знайдіть workflow run для вашого commit
- Перевірте, що всі jobs виконались успішно:
  - ✅ `Build and Test`
  - ✅ `Build Docker Image`
  - ✅ `Deploy to Azure AKS` (якщо push в main)
  - ✅ `Security Scan`

3. **Перевірити Docker Hub:**

- Перейдіть на https://hub.docker.com/r/andreychykviktor/bank-app/tags
- Переконайтеся, що новий образ з'явився з тегом `main-<commit-sha>`

4. **Перевірити деплой в Kubernetes:**

```bash
# Перевірити, що deployment оновився
kubectl get deployment bank-app -n bank-system

# Перевірити поді
kubectl get pods -n bank-system -l app=bank-app

# Перевірити логи
kubectl logs -n bank-system -l app=bank-app --tail=50
```

### Крок 4: Налаштувати моніторинг (Prometheus + Grafana)

#### 4.1. Застосувати манифести моніторингу

```bash
cd backend/k8s

# Застосувати Prometheus
kubectl apply -f prometheus-deployment.yaml

# Застосувати Grafana
kubectl apply -f grafana-deployment.yaml
kubectl apply -f grafana-ingress.yaml

# Або використати скрипт
chmod +x apply-monitoring.sh
./apply-monitoring.sh
```

#### 4.2. Перевірити статус

```bash
# Перевірити поді
kubectl get pods -n bank-system | grep -E "prometheus|grafana"

# Перевірити сервіси
kubectl get svc -n bank-system | grep -E "prometheus|grafana"
```

#### 4.3. Доступ до Grafana

**Через port-forward (локально):**

```bash
kubectl port-forward -n bank-system svc/grafana 3000:3000
```

Відкрити в браузері: http://localhost:3000

**Через Ingress (якщо налаштовано):**

http://grafana.andreychyk-bank.duckdns.org

**Вхід:**
- Username: `admin`
- Password: `admin123` (⚠️ змініть в production!)

#### 4.4. Налаштувати дашборди Grafana

1. **Додати Prometheus як Data Source:**

- Перейдіть: **Configuration → Data Sources → Add data source**
- Виберіть **Prometheus**
- URL: `http://prometheus:9090` (внутрішній сервіс)
- Натисніть **Save & Test**

2. **Імпортувати готові дашборди:**

- Перейдіть: **Dashboards → Import**
- Імпортуйте один з наступних ID:
  - **4701** - JVM Micrometer (метрики JVM)
  - **11378** - Spring Boot 2.1 Statistics
  - **6417** - Kubernetes Pods

3. **Перевірити метрики додатку:**

```bash
# Перевірити, що метрики експортуються
curl http://andreychyk-bank.duckdns.org/actuator/prometheus | head -20
```

### Крок 5: Налаштувати алерти (опціонально)

1. **Створити AlertManager** (якщо потрібно)
2. **Налаштувати правила алертів**
3. **Додати сповіщення** (email, Slack, Telegram)

Детальні інструкції: [MONITORING_SETUP.md](./MONITORING_SETUP.md)

## 🔍 Troubleshooting

### Проблема: "Docker login failed"

**Рішення:**
- Перевірте `DOCKER_USERNAME` та `DOCKER_PASSWORD`
- Використовуйте Access Token замість пароля
- Переконайтеся, що токен не прострочений

### Проблема: "KUBECONFIG secret не знайдено"

**Рішення:**
- Перевірте, що додано `KUBECONFIG_AZURE` або `KUBECONFIG`
- Переконайтеся, що config правильно закодовано в base64
- Перевірте правильність контексту

### Проблема: "Deployment not found"

**Рішення:**
- Спочатку застосуйте манифести: `kubectl apply -f k8s/`
- Перевірте namespace: `kubectl get namespaces`
- Перевірте, що deployment існує: `kubectl get deployment -n bank-system`

### Проблема: "Image pull failed"

**Рішення:**
- Перевірте, що образ існує в Docker Hub
- Перевірте теги: `docker pull andreychykviktor/bank-app:main-<sha>`
- Переконайтеся, що build job успішно завершився

## 📚 Додаткові ресурси

- [CICD_SETUP.md](./CICD_SETUP.md) - Детальна інструкція по CI/CD
- [MONITORING_SETUP.md](./MONITORING_SETUP.md) - Детальна інструкція по моніторингу
- [MULTI_CLOUD_SETUP.md](./MULTI_CLOUD_SETUP.md) - Налаштування multi-cloud

## ✅ Чеклист завершення

- [ ] Додано `KUBECONFIG_AZURE` в GitHub Secrets
- [ ] Додано `KUBECONFIG_AWS` (якщо використовуєте AWS)
- [ ] Протестовано CI/CD pipeline (push в main)
- [ ] Перевірено, що образ завантажився в Docker Hub
- [ ] Перевірено, що деплой пройшов успішно
- [ ] Встановлено Prometheus та Grafana
- [ ] Налаштовано дашборди в Grafana
- [ ] Перевірено, що метрики збираються

## 🎉 Готово!

Після виконання всіх кроків ваш CI/CD pipeline та моніторинг будуть повністю налаштовані!

