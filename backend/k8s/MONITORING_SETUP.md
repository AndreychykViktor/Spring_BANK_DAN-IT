# Налаштування моніторингу з Prometheus та Grafana

## Огляд

Цей гайд описує налаштування моніторингу банківського додатку за допомогою Prometheus та Grafana.

## Компоненти

- **Prometheus** - збір та зберігання метрик
- **Grafana** - візуалізація метрик у вигляді дашбордів
- **Spring Boot Actuator** - експорт метрик з додатку

## Встановлення

### 1. Додати Prometheus та Grafana

```bash
# Застосувати всі манифести моніторингу
kubectl apply -f prometheus-deployment.yaml
kubectl apply -f grafana-deployment.yaml
kubectl apply -f grafana-ingress.yaml
```

### 2. Перевірити статус

```bash
# Перевірити поді Prometheus
kubectl get pods -n bank-system -l app=prometheus

# Перевірити поді Grafana
kubectl get pods -n bank-system -l app=grafana

# Перевірити сервіси
kubectl get svc -n bank-system | grep -E "prometheus|grafana"
```

### 3. Доступ до Prometheus

```bash
# Port-forward для локального доступу
kubectl port-forward -n bank-system svc/prometheus 9090:9090

# Відкрити в браузері
# http://localhost:9090
```

### 4. Доступ до Grafana

```bash
# Port-forward для локального доступу
kubectl port-forward -n bank-system svc/grafana 3000:3000

# Або через Ingress (якщо налаштовано DNS)
# http://grafana.andreychyk-bank.duckdns.org

# Вхід:
# Username: admin
# Password: admin123 (за замовчуванням, змініть!)
```

## Налаштування дашбордів Grafana

### Імпорт готових дашбордів

1. Увійдіть в Grafana
2. Перейдіть в **Dashboards** → **Import**
3. Імпортуйте один з наступних ID:
   - **JVM Micrometer** (ID: 4701) - метрики JVM
   - **Spring Boot 2.1 Statistics** (ID: 11378) - Spring Boot метрики
   - **Kubernetes Pods** (ID: 6417) - метрики Kubernetes подів

### Створення власних дашбордів

Можна створити власні дашборди для моніторингу:
- Кількість HTTP запитів
- Час відповіді API
- Кількість транзакцій
- Використання пам'яті та CPU
- Кількість активних користувачів

## Метрики, які експортуються

### Spring Boot Actuator метрики:

- `http_server_requests_seconds` - час обробки HTTP запитів
- `jvm_memory_used_bytes` - використання пам'яті JVM
- `jvm_gc_pause_seconds` - паузи збірника сміття
- `process_cpu_usage` - використання CPU
- `hikari_connections_active` - активні підключення до БД
- `custom_bank_transactions_total` - кількість транзакцій (можна додати)

### Перевірка метрик

```bash
# Перевірити експорт метрик з додатку
curl http://andreychyk-bank.duckdns.org/actuator/prometheus | grep bank_app

# Перевірити health check
curl http://andreychyk-bank.duckdns.org/actuator/health
```

## Налаштування алертів

### Prometheus AlertManager

1. Створити `alertmanager-deployment.yaml`
2. Налаштувати правила алертів в `prometheus-alerts.yaml`
3. Налаштувати сповіщення (email, Slack, Telegram)

### Приклади алертів:

- Використання пам'яті > 80%
- Час відповіді API > 1 секунда
- Кількість помилок > 10 за хвилину
- Додаток недоступний (down)

## Безпека

⚠️ **ВАЖЛИВО**: Змініть пароль Grafana в `grafana-deployment.yaml` перед розгортанням!

```yaml
- name: GF_SECURITY_ADMIN_PASSWORD
  valueFrom:
    secretKeyRef:
      name: grafana-secret
      key: admin-password
```

Оновіть secret:
```bash
kubectl create secret generic grafana-secret \
  --from-literal=admin-password='ВАШ_БЕЗПЕЧНИЙ_ПАРОЛЬ' \
  -n bank-system \
  --dry-run=client -o yaml | kubectl apply -f -
```

## Корисні команди

```bash
# Переглянути логи Prometheus
kubectl logs -n bank-system -l app=prometheus --tail=50

# Переглянути логи Grafana
kubectl logs -n bank-system -l app=grafana --tail=50

# Перезапустити Prometheus
kubectl rollout restart deployment/prometheus -n bank-system

# Перезапустити Grafana
kubectl rollout restart deployment/grafana -n bank-system

# Видалити моніторинг
kubectl delete -f prometheus-deployment.yaml
kubectl delete -f grafana-deployment.yaml
kubectl delete -f grafana-ingress.yaml
```

## Додаткові ресурси

- [Prometheus документація](https://prometheus.io/docs/)
- [Grafana документація](https://grafana.com/docs/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

