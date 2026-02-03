#!/bin/bash
# Railway Deployment Script for Cricnets AI
# Run this script from your local machine where Railway CLI is installed

set -e

echo "=== Cricnets AI - Railway Deployment ==="
echo ""

# Check if Railway CLI is installed
if ! command -v railway &> /dev/null; then
    echo "Railway CLI not found. Installing..."
    npm install -g @railway/cli
fi

# Check if logged in
if ! railway whoami &> /dev/null; then
    echo "Not logged in. Starting login..."
    railway login
fi

echo ""
echo "Logged in as: $(railway whoami)"
echo ""

# Initialize project if not linked
if ! railway status &> /dev/null; then
    echo "Creating new Railway project..."
    railway init --name cricnets-ai
fi

echo ""
echo "=== Adding PostgreSQL Database ==="
railway add --plugin postgresql || echo "PostgreSQL may already exist"

echo ""
echo "=== Setting Environment Variables ==="
echo "Please enter your Google Gemini API Key:"
read -r GOOGLE_API_KEY
railway variables set GOOGLE_GENAI_API_KEY="$GOOGLE_API_KEY"
railway variables set ENV=prod

echo ""
echo "=== Deploying Application ==="
railway up --detach

echo ""
echo "=== Deployment Started! ==="
echo ""
echo "View your deployment at: https://railway.app/dashboard"
echo "Use 'railway logs' to view deployment logs"
echo "Use 'railway domain' to generate a public URL"
