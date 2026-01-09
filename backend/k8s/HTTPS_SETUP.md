# 🔒 Налаштування HTTPS з Let's Encrypt для DuckDNS

Цей гайд допоможе налаштувати HTTPS для вашого DuckDNS домену використовуючи Nginx Ingress та cert-manager.

## 📋 Передумови

1. Kubernetes кластер працює
2. Маєте DuckDNS домен (наприклад: `yourbank.duckdns.org`)
3. Ваш DuckDNS домен вказує на IP адресу вашого Load Balancer
4. Встановлено `kubectl` та `helm`

## 🚀 Крок 1: Встановлення Nginx Ingress Controller

### Варіант A: Використовуючи Helm (рекомендовано)

```bash
# Додати Helm репозиторій
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update

# Встановити Nginx Ingress Controller
helm install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx \
  --create-namespace \
  --set controller.service.type=LoadBalancer
```

### Варіант B: Використовуючи kubectl

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.1/deploy/static/provider/cloud/deploy.yaml
```

### Перевірка встановлення

```bash
# Дочекайтеся, поки ingress controller буде готовий
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=300s

# Отримати External IP адресу
kubectl get svc -n ingress-nginx ingress-nginx-controller
```

**Важливо:** Оновіть ваш DuckDNS домен, щоб він вказував на цю External IP адресу!

---

## 🔐 Крок 2: Встановлення cert-manager

```bash
# Додати Helm репозиторій
helm repo add jetstack https://charts.jetstack.io
helm repo update

# Встановити cert-manager
kubectl create namespace cert-manager
helm install cert-manager jetstack/cert-manager \
  --namespace cert-manager \
  --set installCRDs=true

# Дочекайтеся готовності
kubectl wait --namespace cert-manager \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/instance=cert-manager \
  --timeout=300s
```

---

## ⚙️ Крок 3: Налаштування ClusterIssuer

1. Відредагуйте `cert-manager-clusterissuer.yaml`:
   - Замініть `your-email@example.com` на вашу реальну email адресу

2. Застосувати ClusterIssuer:

```bash
kubectl apply -f cert-manager-clusterissuer.yaml
```

3. Перевірити створення:

```bash
kubectl get clusterissuer
```

Ви повинні побачити `letsencrypt-prod` в статусі `True`.

---

## 🌐 Крок 4: Налаштування Ingress з HTTPS

1. Відредагуйте `ingress-nginx.yaml`:
   - Замініть `your-domain.duckdns.org` на ваш реальний DuckDNS домен

2. Застосувати Ingress:

```bash
# Переконайтеся, що namespace створено
kubectl apply -f namespace.yaml

# Застосувати ingress
kubectl apply -f ingress-nginx.yaml
```

3. Перевірити створення сертифікату:

```bash
# Перевірити Certificate
kubectl get certificate -n bank-system

# Перевірити деталі
kubectl describe certificate bank-tls-secret -n bank-system

# Перевірити Order (процес отримання сертифікату)
kubectl get order -n bank-system
```

**Примітка:** Сертифікат може створюватися 2-5 хвилин. Перевірте логи cert-manager, якщо є проблеми:

```bash
kubectl logs -n cert-manager -l app.kubernetes.io/instance=cert-manager
```

---

## ✅ Крок 5: Перевірка HTTPS

Після того, як сертифікат створено (статус `Ready`):

```bash
# Перевірити, що HTTPS працює
curl -I https://your-domain.duckdns.org

# Перевірити сертифікат
openssl s_client -connect your-domain.duckdns.org:443 -servername your-domain.duckdns.org
```

У браузері відкрийте `https://your-domain.duckdns.org` - ви повинні побачити замочок 🔒 і відсутність попереджень.

---

## 🔧 Налаштування DuckDNS

Переконайтеся, що ваш DuckDNS домен вказує на правильний IP:

1. Отримати External IP вашего Load Balancer:
   ```bash
   kubectl get svc -n ingress-nginx ingress-nginx-controller -o jsonpath='{.status.loadBalancer.ingress[0].ip}'
   ```

2. Оновити DuckDNS:
   - Зайти на https://www.duckdns.org
   - Увійти в акаунт
   - Оновити IP адресу для вашого домену

---

## 🐛 Вирішення проблем

### Проблема: Certificate не створюється

**Перевірка 1:** Переконайтеся, що домен вказує на правильний IP
```bash
nslookup your-domain.duckdns.org
```

**Перевірка 2:** Перевірити логи cert-manager
```bash
kubectl logs -n cert-manager -l app.kubernetes.io/instance=cert-manager
```

**Перевірка 3:** Перевірити Challenges
```bash
kubectl get challenge -n bank-system
kubectl describe challenge <challenge-name> -n bank-system
```

### Проблема: 502 Bad Gateway

Переконайтеся, що ваш додаток працює:
```bash
kubectl get pods -n bank-system
kubectl logs -n bank-system -l app=bank-app
```

### Проблема: Certificate в стані Pending

Перевірте Order та Challenge:
```bash
kubectl get order -n bank-system
kubectl describe order <order-name> -n bank-system
kubectl get challenge -n bank-system
```

---

## 📚 Додаткова інформація

- [cert-manager документація](https://cert-manager.io/docs/)
- [Nginx Ingress документація](https://kubernetes.github.io/ingress-nginx/)
- [Let's Encrypt документація](https://letsencrypt.org/docs/)

---

## 🔄 Оновлення сертифікатів

Cert-manager автоматично оновлює сертифікати за 30 днів до закінчення терміну дії. Жодних додаткових дій не потрібно!

---

**Готово!** Ваш HTTPS налаштовано! 🎉

