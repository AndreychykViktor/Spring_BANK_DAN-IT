# Як перевірити, що CI/CD та моніторинг працюють

## ✅ Швидка перевірка (5 хвилин)

### Крок 1: Запустити тестовий pipeline

```bash
# Створити тестовий commit та push
git commit --allow-empty -m "test: verify CI/CD pipeline"
git push origin main
```

### Крок 2: Перевірити в GitHub Actions

1. Перейдіть на GitHub → **Actions** tab
2. Знайдіть останній workflow run: **"CI/CD Pipeline"**
3. Перевірте статус кожного job:

   ✅ **Build and Test** - має бути зелений
   ✅ **Build Docker Image** - має бути зелений
   ✅ **Deploy to Azure AKS** - має бути зелений
   ✅ **Deploy to AWS EKS** - має бути зелений (якщо налаштовано)
   ✅ **Security Scan** - має бути зелений

4. Клікніть на кожен job, щоб побачити деталі:
   - Перевірте логи на помилки
   - Перевірте, що Docker image завантажився
   - Перевірте, що деплой пройшов успішно

### Крок 3: Перевірити Docker Hub

1. Перейдіть на https://hub.docker.com/r/andreychykviktor/bank-app/tags
2. Перевірте, що новий образ з'явився:
   - Тег: `main-<commit-sha>` (наприклад: `main-abc123def`)
   - Тег: `latest` (для main branch)

## 🔍 Детальна перевірка (10 хвилин)

### Перевірка деплою в Azure AKS

```bash
# Перемкнутися на Azure контекст
kubectl config use-context andreychyk-bank-cluster

# Перевірити статус deployment
kubectl get deployment bank-app -n bank-system

# Перевірити поді
kubectl get pods -n bank-system -l app=bank-app

# Перевірити логи
kubectl logs -n bank-system -l app=bank-app --tail=50

# Перевірити сервіси
kubectl get svc -n bank-system

# Перевірити ingress
kubectl get ingress -n bank-system
```

**Що перевірити:**
- ✅ Deployment має статус `READY`
- ✅ Поди мають статус `Running`
- ✅ В логах немає критичних помилок
- ✅ Ingress має правильний адрес

### Перевірка деплою в AWS EKS

```bash
# Перемкнутися на AWS контекст
kubectl config use-context andreychik.viktor@andreychyk-bank-cluster.eu-north-1.eksctl.io

# Перевірити статус deployment
kubectl get deployment bank-app -n bank-system

# Перевірити поді
kubectl get pods -n bank-system -l app=bank-app

# Перевірити логи
kubectl logs -n bank-system -l app=bank-app --tail=50
```

### Перевірка додатку в браузері

1. Відкрити: https://andreychyk-bank.duckdns.org
2. Перевірити:
   - ✅ Сайт відкривається
   - ✅ HTTPS працює (🔒 в адресному рядку)
   - ✅ Можна залогінитися
   - ✅ Додаток працює нормально

## 📊 Перевірка моніторингу

### Перевірка Prometheus

```bash
# Port-forward до Prometheus
kubectl port-forward -n bank-system svc/prometheus 9090:9090

# Відкрити в браузері: http://localhost:9090
```

**Що перевірити:**
1. Перейдіть: **Status → Targets**
2. Перевірте, що `bank-app` endpoint працює (статус UP)
3. Перейдіть: **Graph**
4. Спробуйте запит: `up{job="bank-app"}`

### Перевірка Grafana

```bash
# Port-forward до Grafana
kubectl port-forward -n bank-system svc/grafana 3000:3000

# Відкрити в браузері: http://localhost:3000
# Вхід: admin / admin123 (за замовчуванням)
```

**Що зробити:**
1. Увійти в Grafana
2. Перейти: **Configuration → Data Sources**
3. Перевірити/додати Prometheus:
   - URL: `http://prometheus:9090`
   - Натиснути **Save & Test**
4. Перейти: **Dashboards → Import**
5. Імпортувати готові дашборди:
   - ID: **4701** (JVM Micrometer)
   - ID: **11378** (Spring Boot 2.1)
   - ID: **6417** (Kubernetes Pods)

### Перевірка метрик додатку

```bash
# Перевірити, що метрики експортуються
curl https://andreychyk-bank.duckdns.org/actuator/prometheus | head -20

# Перевірити health
curl https://andreychyk-bank.duckdns.org/actuator/health

# Перевірити info
curl https://andreychyk-bank.duckdns.org/actuator/info
```

**Очікуваний результат:**
- ✅ `/actuator/prometheus` повертає метрики
- ✅ `/actuator/health` показує `{"status":"UP"}`
- ✅ Метрики містять `http_server_requests_seconds`, `jvm_memory_used_bytes`, тощо

## 🐛 Troubleshooting

### Проблема: Pipeline не запускається

**Перевірте:**
- Є чи ні push в `main` або `develop` branch
- Перевірте синтаксис YAML: `.github/workflows/ci-cd.yml`

**Рішення:**
```bash
# Перевірити синтаксис
yamllint .github/workflows/ci-cd.yml  # якщо встановлено

# Або просто перевірити в GitHub UI
# Actions → CI/CD Pipeline → перевірити помилки
```

### Проблема: "Docker login failed"

**Перевірте:**
- Secrets `DOCKER_USERNAME` та `DOCKER_PASSWORD` правильні
- Використовується Access Token, а не пароль

**Рішення:**
1. Перейдіть: Settings → Secrets → Actions
2. Перевірте `DOCKER_USERNAME` та `DOCKER_PASSWORD`
3. Оновіть, якщо потрібно

### Проблема: "KUBECONFIG not found"

**Перевірте:**
- Додано принаймні один з: `KUBECONFIG_AZURE`, `KUBECONFIG_AWS`, або `KUBECONFIG`

**Рішення:**
1. Перевірте, що secret додано правильно
2. Перевірте, що base64 строка повна (не обрізана)
3. Спробуйте заново отримати та додати config

### Проблема: "Deployment not found"

**Перевірте:**
```bash
kubectl get deployment -n bank-system
```

**Рішення:**
Якщо deployment не існує, спочатку застосуйте манифести:
```bash
cd backend/k8s
kubectl apply -f app-deployment.yaml
kubectl apply -f app-service.yaml
```

### Проблема: "Image pull failed"

**Перевірте:**
1. Образ існує в Docker Hub: https://hub.docker.com/r/andreychykviktor/bank-app/tags
2. Тег правильний: `main-<commit-sha>`

**Рішення:**
```bash
# Перевірити образ локально
docker pull andreychykviktor/bank-app:main-<commit-sha>

# Якщо не працює, перевірити теги в Docker Hub
```

### Проблема: Pods не запускаються

**Перевірте:**
```bash
# Подивитися події
kubectl get events -n bank-system --sort-by='.lastTimestamp' | tail -20

# Описати под
kubectl describe pod <pod-name> -n bank-system

# Перевірити логи
kubectl logs <pod-name> -n bank-system
```

**Типові проблеми:**
- Image pull error - образ не знайдено
- ConfigMap/Secret не знайдено - перевірте secrets
- Помилки в коді - перевірте логи

### Проблема: Метрики не збираються

**Перевірте:**
```bash
# Перевірити, що Prometheus scrape додаток
kubectl port-forward -n bank-system svc/prometheus 9090:9090
# Відкрити: http://localhost:9090/targets
```

**Рішення:**
1. Перевірте, що додаток експортує метрики:
   ```bash
   curl https://andreychyk-bank.duckdns.org/actuator/prometheus
   ```

2. Перевірте Prometheus config в `prometheus-deployment.yaml`
3. Перезапустіть Prometheus:
   ```bash
   kubectl rollout restart deployment/prometheus -n bank-system
   ```

## ✅ Чеклист перевірки

### CI/CD Pipeline
- [ ] Pipeline запускається при push в main
- [ ] Build and Test job успішний
- [ ] Build Docker Image job успішний
- [ ] Docker image завантажився в Docker Hub
- [ ] Deploy to Azure AKS job успішний
- [ ] Deploy to AWS EKS job успішний (якщо налаштовано)
- [ ] Security Scan job успішний

### Azure AKS
- [ ] Deployment оновився з новим образом
- [ ] Поди запущені та працюють
- [ ] Додаток доступний через Ingress
- [ ] HTTPS працює

### AWS EKS (якщо налаштовано)
- [ ] Deployment оновився з новим образом
- [ ] Поди запущені та працюють
- [ ] Додаток доступний

### Моніторинг
- [ ] Prometheus запущений та збирає метрики
- [ ] Grafana доступний та налаштований
- [ ] Метрики додатку експортуються
- [ ] Дашборди в Grafana працюють

## 🎯 Команди для швидкої перевірки

```bash
#!/bin/bash
# Скрипт для швидкої перевірки всіх компонентів

echo "🔍 Перевірка CI/CD та моніторингу..."
echo ""

# Перевірка Azure
echo "📦 Azure AKS:"
kubectl config use-context andreychyk-bank-cluster > /dev/null 2>&1
kubectl get deployment bank-app -n bank-system
kubectl get pods -n bank-system -l app=bank-app
echo ""

# Перевірка AWS (якщо налаштовано)
echo "📦 AWS EKS:"
kubectl config use-context andreychik.viktor@andreychyk-bank-cluster.eu-north-1.eksctl.io > /dev/null 2>&1
kubectl get deployment bank-app -n bank-system 2>/dev/null || echo "AWS deployment не знайдено або не налаштовано"
echo ""

# Перевірка моніторингу
echo "📊 Моніторинг:"
kubectl get pods -n bank-system | grep -E "prometheus|grafana"
echo ""

# Перевірка метрик
echo "📈 Метрики додатку:"
curl -s https://andreychyk-bank.duckdns.org/actuator/health | head -1
echo ""

echo "✅ Перевірка завершена!"
```

## 📚 Додаткові ресурси

- [VERIFY_CI_CD.md](./VERIFY_CI_CD.md) - Детальна перевірка CI/CD
- [MONITORING_SETUP.md](./MONITORING_SETUP.md) - Налаштування моніторингу
- [NEXT_STEPS.md](./NEXT_STEPS.md) - Наступні кроки

