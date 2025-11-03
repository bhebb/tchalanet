#!/usr/bin/env bash
set -euo pipefail

sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg lsb-release
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list >/dev/null
sudo apt-get update && sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Configure sysctl for Docker
echo "→ Configuring sysctl for Docker..."
sudo tee /etc/sysctl.d/99-docker.conf >/dev/null <<'EOF'
net.ipv4.ip_forward=1
net.bridge.bridge-nf-call-iptables=1
EOF
sudo sysctl --system >/dev/null

# Ensure docker service enabled and running
if command -v systemctl >/dev/null 2>&1; then
  sudo systemctl enable --now docker
else
  echo "systemctl not available; ensure docker daemon is running" >&2
fi

# Add current user to docker group (note: re-login needed to take effect)
sudo usermod -aG docker "$USER" || true

# Activate docker group for current shell session
echo "→ Activating docker group for current session..."
# Note: This creates a new shell with the docker group active
# The calling script should handle Docker commands within this context

echo "→ Testing docker: running hello-world"
if ! sudo docker run --rm hello-world >/dev/null 2>&1; then
  echo "Warning: docker hello-world failed; try 'sudo docker run --rm hello-world' on the host to debug" >&2
else
  echo "✔ docker run hello-world OK"
fi

echo ""
echo "✔ Docker installed successfully"
echo "⚠️  Note: The user '$USER' has been added to the 'docker' group."
echo "   For the current session, use 'sudo docker' or run: newgrp docker"
echo "   New SSH sessions will have docker access automatically."
echo ""

