#!/bin/bash
# Elasticsearch Initialization Script

set -e

# Chờ Elasticsearch khởi động
echo "Waiting for Elasticsearch to be ready..."
until curl -s -u elastic:changeme http://elasticsearch:9200/_cluster/health > /dev/null; do
  echo "Elasticsearch is unavailable - sleeping"
  sleep 1
done

echo "Elasticsearch is ready!"

# Tạo index pattern cho Kibana
echo "Setting up Kibana..."

# Tạo index template cho Kong logs
curl -X PUT "elasticsearch:9200/_index_template/kong-logs-template" \
  -H "Content-Type: application/json" \
  -u "elastic:changeme" \
  -d '{
  "index_patterns": ["kong-logs-*"],
  "template": {
    "settings": {
      "number_of_shards": 1,
      "number_of_replicas": 0
    },
    "mappings": {
      "properties": {
        "timestamp": { "type": "date" },
        "service": { "type": "keyword" },
        "request": {
          "type": "object",
          "properties": {
            "method": { "type": "keyword" },
            "uri": { "type": "text" },
            "headers": { "type": "object", "enabled": false }
          }
        },
        "response": {
          "type": "object",
          "properties": {
            "status": { "type": "integer" },
            "headers": { "type": "object", "enabled": false }
          }
        },
        "latencies": {
          "type": "object",
          "properties": {
            "proxy": { "type": "integer" },
            "upstream": { "type": "integer" },
            "request": { "type": "integer" }
          }
        }
      }
    }
  }
}' || true

echo "Kibana setup completed!"
