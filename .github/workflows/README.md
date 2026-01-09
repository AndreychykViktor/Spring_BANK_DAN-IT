# CI/CD Pipeline

## Огляд

GitHub Actions pipeline для автоматичної збірки, тестування та розгортання банківського додатку.

## Workflow

### 1. Test Job
- Запускається при кожному push/PR
- Компілює код
- Запускає тести
- Перевіряє якість коду

### 2. Build Job
- Запускається тільки при push в `main` або `develop`
- Збирає Docker образ
- Пушить в Docker Hub з тегами:
  - `main-<sha>` або `develop-<sha>`
  - `latest` (тільки для main)

### 3. Deploy Job
- Запускається тільки при push в `main`
- Оновлює deployment в Kubernetes
- Перевіряє успішність розгортання

### 4. Security Job
- Сканує Docker образ на вразливості
- Використовує Trivy scanner

## Налаштування Secrets

Додайте наступні secrets в GitHub Settings → Secrets and variables → Actions:

### Docker Hub
- `DOCKER_USERNAME` - ваш username в Docker Hub
- `DOCKER_PASSWORD` - ваш access token або пароль

### Kubernetes
- `KUBECONFIG` - base64 encoded kubeconfig файл
  ```bash
  cat ~/.kube/config | base64 -w 0
  ```
- `KUBERNETES_CONTEXT` - назва контексту (опціонально)

## Використання

### Автоматичне розгортання

1. Push в `main` → автоматично запускається pipeline
2. Після успішних тестів → збирається Docker образ
3. Після збірки → автоматично деплоїться в Kubernetes

### Ручне розгортання

Можна також налаштувати manual workflow dispatch для ручного запуску.

## Перевірка статусу

- Відкрийте **Actions** tab в GitHub
- Перегляньте статус кожного job
- Перегляньте логи для діагностики проблем

## Troubleshooting

### Помилка авторизації Docker Hub
- Перевірте `DOCKER_USERNAME` та `DOCKER_PASSWORD`
- Використовуйте Access Token замість пароля

### Помилка підключення до Kubernetes
- Перевірте `KUBECONFIG` secret
- Перевірте права доступу до кластера
- Перевірте правильність контексту

### Тести не проходять
- Перевірте локально: `mvn test`
- Перегляньте логи в GitHub Actions

