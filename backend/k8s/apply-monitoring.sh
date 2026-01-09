

set -e

echo "🚀 Розгортання моніторингу..."

if ! kubectl get namespace bank-system &> /dev/null; then
    echo "❌ Namespace bank-system не знайдено. Спочатку створіть namespace:"
    echo "   kubectl apply -f namespace.yaml"
    exit 1
fi

echo "📊 Встановлення Prometheus..."
kubectl apply -f prometheus-deployment.yaml

echo "📈 Встановлення Grafana..."
kubectl apply -f grafana-deployment.yaml

echo "🌐 Встановлення Grafana Ingress..."
kubectl apply -f grafana-ingress.yaml

echo "⏳ Очікування готовності подів..."
kubectl wait --for=condition=ready pod -l app=prometheus -n bank-system --timeout=120s
kubectl wait --for=condition=ready pod -l app=grafana -n bank-system --timeout=120s

echo ""
echo "✅ Моніторинг успішно розгорнуто!"
echo ""
echo "📊 Доступ до Prometheus:"
echo "   kubectl port-forward -n bank-system svc/prometheus 9090:9090"
echo "   http://localhost:9090"
echo ""
echo "📈 Доступ до Grafana:"
echo "   kubectl port-forward -n bank-system svc/grafana 3000:3000"
echo "   http://localhost:3000"
echo "   Username: admin"
echo "   Password: admin123 (змініть в grafana-deployment.yaml!)"
echo ""
echo "🌐 Або через Ingress:"
echo "   http://grafana.andreychyk-bank.duckdns.org"
echo ""
echo "📖 Детальні інструкції: MONITORING_SETUP.md"

