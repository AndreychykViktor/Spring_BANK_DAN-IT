# Налаштування CI/CD Pipeline

## Швидкий старт

### 1. Налаштування GitHub Secrets

Перейдіть в ваш репозиторій на GitHub:
**Settings → Secrets and variables → Actions → New repository secret**

#### Docker Hub (обов'язково)

```
Name: DOCKER_USERNAME
Value: ваш_username_в_docker_hub
```

```
Name: DOCKER_PASSWORD
Value: ваш_password_або_access_token
```

**Як отримати Docker Hub token:**
1. Увійдіть в Docker Hub
2. Account Settings → Security → New Access Token
3. Скопіюйте токен та використовуйте його як пароль

#### Kubernetes для Multi-Cloud (Azure + AWS)

**Варіант 1: Окремі config файли (рекомендовано)**

Для Azure:
```
Name: KUBECONFIG_AZURE
Value: <base64_encoded_azure_kubeconfig>
```

Для AWS:
```
Name: KUBECONFIG_AWS
Value: <base64_encoded_aws_kubeconfig>
```

**Варіант 2: Один config з обома контекстами**

```
Name: KUBECONFIG
Value: <base64_encoded_kubeconfig_з_обома_контекстами>
Name: AZURE_KUBERNETES_CONTEXT
Value: назва_контексту_azure
Name: AWS_KUBERNETES_CONTEXT
Value: назва_контексту_aws
```

**Варіант 3: AWS через credentials**

```
Name: AWS_ACCESS_KEY_ID
Value: ваш_aws_access_key
Name: AWS_SECRET_ACCESS_KEY
Value: ваш_aws_secret_key
Name: AWS_REGION
Value: eu-central-1
Name: AWS_EKS_CLUSTER_NAME
Value: назва_eks_кластера
```

**Детальні інструкції:** [MULTI_CLOUD_SETUP.md](./MULTI_CLOUD_SETUP.md)

**Як отримати KUBECONFIG:**

```bash
# Вариант 1: з локального файлу
cat ~/.kube/config | base64 -w 0

# Вариант 2: з Azure AKS
az aks get-credentials --resource-group andreychyk-bank --name andreychyk-bank-cluster
cat ~/.kube/config | base64 -w 0
```

```
Name: KUBERNETES_CONTEXT (опціонально)
Value: ваш_kubernetes_context_назва
```

### 2. Перевірка налаштувань

Після додавання secrets, перевірте:

```bash
# Перевірте, що workflow файл на місці
ls -la .github/workflows/ci-cd.yml

# Перевірте синтаксис YAML
yamllint .github/workflows/ci-cd.yml  # якщо встановлено
```

### 3. Запуск pipeline

#### Автоматичний запуск

Pipeline автоматично запускається при:
- Push в `main` або `develop` гілку
- Створенні Pull Request в `main` або `develop`

#### Ручний запуск (якщо налаштовано)

1. Перейдіть в **Actions** tab в GitHub
2. Виберіть **CI/CD Pipeline**
3. Натисніть **Run workflow**

### 4. Моніторинг pipeline

1. Перейдіть в **Actions** tab
2. Клікніть на конкретний workflow run
3. Перегляньте логи кожного job

## Структура Pipeline

```
┌─────────────┐
│   Push/PR   │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│    Test     │  ◄── Компіляція + Тести
└──────┬──────┘
       │
       ▼
┌─────────────┐
│    Build    │  ◄── Docker build + push
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   Deploy    │  ◄── Kubernetes deployment (тільки main)
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Security   │  ◄── Trivy scan (тільки main)
└─────────────┘
```

## Troubleshooting

### ❌ Помилка: "Docker login failed"

**Причина:** Неправильні credentials

**Рішення:**
1. Перевірте `DOCKER_USERNAME` та `DOCKER_PASSWORD`
2. Використовуйте Access Token замість пароля
3. Переконайтеся, що токен не прострочений

### ❌ Помилка: "Kubernetes connection failed"

**Причина:** Неправильний kubeconfig або немає доступу

**Рішення:**
1. Перевірте `KUBECONFIG` secret - має бути base64 encoded
2. Перевірте права доступу: `kubectl auth can-i create deployments`
3. Перевірте правильність контексту

### ❌ Помилка: "Tests failed"

**Причина:** Тести не проходять

**Рішення:**
1. Запустіть тести локально: `mvn test`
2. Перевірте логи в GitHub Actions
3. Виправте помилки в коді

### ❌ Помилка: "Image pull failed"

**Причина:** Образ не знайдено в registry

**Рішення:**
1. Перевірте, що образ існує: `docker pull andreychykviktor/bank-app:main-<sha>`
2. Перевірте теги в Docker Hub
3. Переконайтеся, що build job успішно завершився

## Кастомізація

### Зміна умов запуску

Відредагуйте `.github/workflows/ci-cd.yml`:

```yaml
on:
  push:
    branches: [ main, develop, feature/* ]  # Додати інші гілки
  schedule:
    - cron: '0 2 * * *'  # Щоденний запуск о 2:00
```

### Додавання нових jobs

```yaml
jobs:
  lint:
    name: Code Linting
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run linter
        run: mvn checkstyle:check
```

### Зміна стратегії деплою

```yaml
deploy:
  strategy:
    type: blue-green  # або rolling, canary
  steps:
    - name: Deploy
      run: |
        # Ваш кастомний скрипт деплою
```

## Best Practices

1. ✅ Завжди тестуйте локально перед push
2. ✅ Використовуйте feature branches для нових функцій
3. ✅ Перевіряйте логи при помилках
4. ✅ Оновлюйте secrets регулярно
5. ✅ Використовуйте semantic versioning для тегів

## Додаткові ресурси

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Docker Build and Push Action](https://github.com/docker/build-push-action)
- [Kubernetes Deployment](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/)

