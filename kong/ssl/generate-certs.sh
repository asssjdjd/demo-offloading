#!/bin/bash
# =============================================================================
# Generate self-signed SSL certificates for Kong SSL/TLS Termination
# This is for DEVELOPMENT only. Use proper CA-signed certs in production.
# =============================================================================

CERT_DIR="$(dirname "$0")"

echo "==> Generating self-signed SSL certificate for Kong..."

MSYS_NO_PATHCONV=1 openssl req -x509 -nodes -days 365 \
  -newkey rsa:2048 \
  -keyout "${CERT_DIR}/server.key" \
  -out "${CERT_DIR}/server.crt" \
  -subj "/C=VN/ST=Hanoi/L=Hanoi/O=PTIT/OU=DevTeam/CN=localhost" \
  -addext "subjectAltName=DNS:localhost,DNS:kong,IP:127.0.0.1"

echo "==> SSL certificates generated:"
echo "    Certificate: ${CERT_DIR}/server.crt"
echo "    Private Key: ${CERT_DIR}/server.key"
echo ""
echo "==> Certificate details:"
openssl x509 -in "${CERT_DIR}/server.crt" -noout -subject -dates
