#!/bin/bash

set -e

echo "Reading output from Terraform"
PROJECT_ID=$(terraform output -raw project_id)
REGION=$(terraform output -raw region)
DB_INSTANCE_CONNECTION_NAME=$(terraform output -raw db_connection_name)
DB_NAME=$(terraform output -raw db_name)
DB_PASS=$(terraform output -raw db_password_output)

echo "Project: $PROJECT_ID"
echo "DB: $DB_INSTANCE_CONNECTION_NAME"

REPO_NAME="backend-repo"
BACKEND_SERVICE="price-processor-backend"
FRONTEND_SERVICE="frontend-app"
SCRAPER_SERVICE="scraper-service"
BACKEND_DIR="../price-processor"
FRONTEND_DIR="../price-notifier-ui"
SCRAPER_DIR="../price-crawler"
DB_USER="root"

echo "--- 1. Scraper ---"
gcloud builds submit --tag $REGION-docker.pkg.dev/$PROJECT_ID/$REPO_NAME/$SCRAPER_SERVICE $SCRAPER_DIR
gcloud run deploy $SCRAPER_SERVICE --image $REGION-docker.pkg.dev/$PROJECT_ID/$REPO_NAME/$SCRAPER_SERVICE --region $REGION --allow-unauthenticated

SCRAPER_URL=$(gcloud run services describe $SCRAPER_SERVICE --region $REGION --format 'value(status.url)' 2>/dev/null || echo "")

if [ -z "$SCRAPER_URL" ]; then
    echo "Scraper is not working"
else
    echo "Scraper working: $SCRAPER_URL"
fi

echo "--- 1.5. Cloud Function for mail"
FUNCTION_NAME="email-sender-func"
TOPIC_NAME="email-notifications"
EMAIL_DIR="../email-sender"

gcloud pubsub topics create $TOPIC_NAME 2>/dev/null || echo "Topic already exists"

gcloud functions deploy $FUNCTION_NAME \
    --gen2 \
    --runtime=python310 \
    --region=$REGION \
    --source=$EMAIL_DIR \
    --entry-point=send_email_pubsub \
    --trigger-topic=$TOPIC_NAME \
    --set-env-vars SMTP_USER=$SMTP_USER,SMTP_PASS=$SMTP_PASS,SMTP_SENDER=$SMTP_SENDER \
    --allow-unauthenticated


echo "--- 2. Java Backendu ---"

gcloud builds submit --tag $REGION-docker.pkg.dev/$PROJECT_ID/$REPO_NAME/$BACKEND_SERVICE $BACKEND_DIR

gcloud run deploy $BACKEND_SERVICE \
  --image $REGION-docker.pkg.dev/$PROJECT_ID/$REPO_NAME/$BACKEND_SERVICE \
  --region $REGION \
  --allow-unauthenticated \
  --timeout=120s \
  --set-env-vars SPRING_CLOUD_GCP_SQL_ENABLED=true \
  --set-env-vars SPRING_CLOUD_GCP_SQL_DATABASE_NAME="$DB_NAME" \
  --set-env-vars SPRING_CLOUD_GCP_SQL_INSTANCE_CONNECTION_NAME="$DB_INSTANCE_CONNECTION_NAME" \
  --set-env-vars SPRING_DATASOURCE_USERNAME="$DB_USER" \
  --set-env-vars SPRING_DATASOURCE_PASSWORD="$DB_PASS" \
  --set-env-vars SERVER_PORT="8080" \
  --set-env-vars SCRAPER_URL_SEARCH="$SCRAPER_URL/find_price" \
  --set-env-vars SCRAPER_URL_DIRECT="$SCRAPER_URL/scrape_direct_url" \
  --set-env-vars GCP_PUBSUB_TOPIC_NAME="projects/$PROJECT_ID/topics/$TOPIC_NAME" \

BACKEND_URL=$(gcloud run services describe $BACKEND_SERVICE --region $REGION --format 'value(status.url)')
echo "Backend is working: $BACKEND_URL"


echo "--- 3. Frontend ---"

ENV_FILE="$FRONTEND_DIR/src/environments/environment.prod.ts"

sed -i.bak "s|apiUrl: '.*'|apiUrl: '$BACKEND_URL/api'|g" $ENV_FILE

gcloud builds submit --tag $REGION-docker.pkg.dev/$PROJECT_ID/$REPO_NAME/$FRONTEND_SERVICE $FRONTEND_DIR

mv "$ENV_FILE.bak" "$ENV_FILE"

gcloud run deploy $FRONTEND_SERVICE \
  --image $REGION-docker.pkg.dev/$PROJECT_ID/$REPO_NAME/$FRONTEND_SERVICE \
  --region $REGION \
  --allow-unauthenticated

FRONTEND_URL=$(gcloud run services describe $FRONTEND_SERVICE --region $REGION --format 'value(status.url)')

echo "---------------------------------------------------"
echo "Frontend: $FRONTEND_URL"
echo "Backend:  $BACKEND_URL"
echo "Scraper:  $SCRAPER_URL"
echo "---------------------------------------------------"