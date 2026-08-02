#!/bin/bash
# One-time bootstrap script for the Oracle Cloud Ubuntu ARM VM.
# Run this once after SSH-ing in: bash deploy/server-setup.sh
set -e

echo "=== Installing Docker ==="
sudo apt-get update -y
sudo apt-get install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update -y
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
sudo usermod -aG docker $USER

echo "=== Opening port 80 in OS firewall ==="
# IMPORTANT: You must ALSO open port 80 in the Oracle Cloud Console:
#   VCN -> Security Lists -> Add Ingress Rule: Source 0.0.0.0/0, Port 80
sudo ufw allow 80/tcp
sudo ufw allow 22/tcp
sudo ufw --force enable

echo "=== Creating deploy directory ==="
mkdir -p ~/deploy

echo ""
echo "=== NEXT STEPS ==="
echo "1. Copy deploy/docker-compose.prod.yml to ~/deploy/ on this server"
echo "2. Create ~/deploy/.env with your production secrets:"
echo "     PROD_DB_NAME=softwareproject"
echo "     PROD_DB_USERNAME=appuser"
echo "     PROD_DB_PASSWORD=<strong-password>"
echo "     JWT_SECRET=<random-32+-char-string>"
echo "3. Log out and back in (so docker group takes effect)"
echo "4. Test: docker compose -f ~/deploy/docker-compose.prod.yml pull"
echo ""
echo "=== ORACLE CLOUD GOTCHA ==="
echo "Port 80 must be opened in TWO places:"
echo "  a) VCN Security List (Oracle Cloud Console)"
echo "  b) OS firewall (done above via ufw)"
echo "Both must be open or the app won't be reachable from the internet."
