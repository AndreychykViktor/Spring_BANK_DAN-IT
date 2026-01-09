# 💾 Налаштування Storage Class

`storageClassName` визначає тип сховища (storage), який використовуватиметься для PersistentVolumeClaim (PVC).

## 🔍 Як знайти правильне значення для вашого кластера

Виконайте цю команду, щоб побачити доступні Storage Classes у вашому кластері:

```bash
kubectl get storageclass
```

Або коротша версія:
```bash
kubectl get sc
```

**Приклад виводу:**
```
NAME                 PROVISIONER             RECLAIMPOLICY   VOLUMEBINDINGMODE      ALLOWVOLUMEEXPANSION   AGE
gp2 (default)        kubernetes.io/aws-ebs   Delete          WaitForFirstConsumer   false                  1d
gp3                  ebs.csi.aws.com         Delete          WaitForFirstConsumer   true                   1d
```

## 📋 Типові значення для різних провайдерів

### AWS EKS (Elastic Kubernetes Service)

**Рекомендовані значення:**
- `gp3` (рекомендовано) - новий тип, дешевший та швидший
- `gp2` (за замовчуванням) - стандартний тип
- `io1` - для високої продуктивності (дорожче)

**Якщо використовуєте AWS EBS CSI Driver:**
- `gp3-csi` або `ebs-sc` (залежить від вашої конфігурації)

**Перевірка:**
```bash
kubectl get storageclass
# Шукайте значення, що містить gp2, gp3, або ebs
```

---

### Azure AKS (Azure Kubernetes Service)

**Типові значення:**
- `managed-csi` (рекомендовано для нових кластерів)
- `managed-premium` (для старих кластерів)
- `azurefile` - для Azure File Share
- `default` - залежить від конфігурації

**Перевірка:**
```bash
kubectl get storageclass
# Шукайте значення, що містить managed, azurefile, або default
```

---

### Google Cloud GKE (Google Kubernetes Engine)

**Типові значення:**
- `standard` - стандартний SSD
- `premium-rwo` - SSD з ReadWriteOnce
- `pd-ssd` - Persistent Disk SSD
- `pd-standard` - стандартний Persistent Disk

**Перевірка:**
```bash
kubectl get storageclass
```

---

### Мінікубе / Kind / Локальні кластери

**Типові значення:**
- `standard` - зазвичай працює "з коробки"
- `hostpath` - для Minikube
- Або можна залишити порожнім (буде використано default)

---

### Інші провайдери

1. Виконайте `kubectl get storageclass`
2. Знайдіть storage class, позначений як `(default)` або використайте найпідходящий
3. Скопіюйте NAME з виводу

## ✅ Як змінити storageClassName

Після того, як визначили правильне значення:

### 1. Для RabbitMQ (StatefulSet)

Відредагуйте `rabbitmq-deployment.yaml`:

```yaml
volumeClaimTemplates:
- metadata:
    name: rabbitmq-storage
  spec:
    accessModes:
    - ReadWriteOnce
    resources:
      requests:
        storage: 5Gi
    storageClassName: gp3  # ← ЗАМІНІТЬ на ваше значення
```

### 2. Для PostgreSQL (PVC)

Відредагуйте `postgresql-pvc.yaml`:

```yaml
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 10Gi
  storageClassName: gp3  # ← ЗАМІНІТЬ на ваше значення
```

## ⚠️ Важливі примітки

1. **Якщо storageClassName не вказано або порожній (`""`):**
   - Kubernetes використає default Storage Class
   - Це зазвичай працює, але не завжди оптимально

2. **Якщо вказати неправильне значення:**
   - PVC не зможе створитися
   - Поди будуть в стані `Pending`
   - Потрібно виправити значення та перестворити PVC

3. **Перевірка після зміни:**
   ```bash
   # Перевірити статус PVC
   kubectl get pvc -n bank-system
   
   # Перевірити події (якщо є проблеми)
   kubectl describe pvc rabbitmq-storage-rabbitmq-0 -n bank-system
   ```

## 🔧 Приклади для різних сценаріїв

### Сценарій 1: AWS EKS з gp3
```yaml
storageClassName: gp3
```

### Сценарій 2: Azure AKS з managed-csi
```yaml
storageClassName: managed-csi
```

### Сценарій 3: Мінікубе (локальний кластер)
```yaml
storageClassName: standard
# або можна залишити порожнім:
# storageClassName: ""
```

### Сценарій 4: Використання default Storage Class
```yaml
storageClassName: ""  # Порожнє значення використає default
# або просто не вказувати поле storageClassName
```

## 📚 Додаткова інформація

- [Kubernetes Storage Classes](https://kubernetes.io/docs/concepts/storage/storage-classes/)
- [AWS EBS Storage Classes](https://docs.aws.amazon.com/eks/latest/userguide/storage-classes.html)
- [Azure Storage Classes](https://docs.microsoft.com/azure/aks/azure-disks-dynamic-pv)

---

## 🎯 Швидка інструкція

1. Виконайте: `kubectl get storageclass`
2. Скопіюйте NAME з виводу (або використайте той, що позначений як `(default)`)
3. Замініть `standard` на скопійоване значення у всіх файлах:
   - `rabbitmq-deployment.yaml`
   - `postgresql-pvc.yaml`
4. Застосуйте зміни: `kubectl apply -f <файл>`

**Готово!** ✅

