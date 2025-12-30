terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = ">= 4.51.0"
    }
    random = {
      source = "hashicorp/random"
    }
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}

variable "project_id" {
  description = "Google Cloud project ID"
  type        = string
}

variable "region" {
  description = "GCP Region"
  default     = "europe-central2"
}

variable "db_password" {
  description = "Password for db"
  type        = string
  sensitive   = true
}

variable "cron_schedule" {
  description = "Cron schedule for price updates (Unix-cron format)"
  type        = string
  default     = "*/30 * * * *"
}

resource "google_project_service" "run_api" {
  service = "run.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "sqladmin_api" {
  service = "sqladmin.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "artifact_registry_api" {
  service = "artifactregistry.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "pubsub_api" {
  service = "pubsub.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "cloudfunctions_api" {
  service = "cloudfunctions.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "scheduler_api" {
  service            = "cloudscheduler.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "build_api" {
  service = "cloudbuild.googleapis.com"
  disable_on_destroy = false
}

resource "google_artifact_registry_repository" "repo" {
  location      = var.region
  repository_id = "backend-repo"
  description   = "Repozytorium na obrazy Docker Price Processor"
  format        = "DOCKER"

  depends_on = [google_project_service.artifact_registry_api]
}

# Database
resource "random_id" "db_name_suffix" {
  byte_length = 4
}

resource "google_sql_database_instance" "instance" {
  name             = "price-db-${random_id.db_name_suffix.hex}"
  region           = var.region
  database_version = "MYSQL_8_0"
  deletion_protection = false

  settings {
    tier = "db-f1-micro"
    ip_configuration {
      ipv4_enabled = true
    }
  }
  depends_on = [google_project_service.sqladmin_api]
}

resource "google_sql_database" "database" {
  name     = "price_processor_db"
  instance = google_sql_database_instance.instance.name
}

resource "google_sql_user" "users" {
  name     = "root"
  instance = google_sql_database_instance.instance.name
  password = var.db_password
}

# --- Pub/Sub ---
resource "google_pubsub_topic" "email_topic" {
  name = "email-notifications"
}

# --- Cloud Run: BACKEND ---
resource "google_service_account" "backend_sa" {
  account_id   = "backend-sa"
  display_name = "Backend Service Account"
}

resource "google_project_iam_member" "sql_client" {
  project = var.project_id
  role    = "roles/cloudsql.client"
  member  = "serviceAccount:${google_service_account.backend_sa.email}"
}

resource "google_project_iam_member" "monitoring_editor" {
  project = var.project_id
  role    = "roles/monitoring.editor"
  member  = "serviceAccount:${google_service_account.backend_sa.email}"
}

resource "google_project_iam_member" "pubsub_publisher" {
  project = var.project_id
  role    = "roles/pubsub.publisher"
  member  = "serviceAccount:${google_service_account.backend_sa.email}"
}

resource "google_cloud_run_v2_service" "backend" {
  name     = "price-processor-backend"
  location = var.region
  deletion_protection = false
  ingress = "INGRESS_TRAFFIC_ALL"

  template {
    service_account = google_service_account.backend_sa.email

    volumes {
      name = "cloudsql"
      cloud_sql_instance {
        instances = [google_sql_database_instance.instance.connection_name]
      }
    }

    containers {
      image = "us-docker.pkg.dev/cloudrun/container/hello"

      volume_mounts {
        name       = "cloudsql"
        mount_path = "/cloudsql"
      }

      env {
        name  = "DB_URL"
        value = "jdbc:mysql:///${google_sql_database.database.name}?cloudSqlInstance=${google_sql_database_instance.instance.connection_name}&socketFactory=com.google.cloud.sql.mysql.SocketFactory&useSSL=false"
      }
      env {
        name  = "DB_USERNAME"
        value = "root"
      }
      env {
        name  = "DB_PASSWORD"
        value = var.db_password
      }
    }
    scaling {
      min_instance_count = 0
      max_instance_count = 1
    }
  }
  depends_on = [google_project_service.run_api, google_sql_database_instance.instance]
}

resource "google_cloud_run_service_iam_member" "backend_public_access" {
  service  = google_cloud_run_v2_service.backend.name
  location = google_cloud_run_v2_service.backend.location
  role     = "roles/run.invoker"
  member   = "allUsers"
}

# --- Cloud Run: SCRAPER ---
resource "google_cloud_run_v2_service" "scraper" {
  name     = "scraper-service"
  location = var.region
  deletion_protection = false

  template {
    containers {
      image = "us-docker.pkg.dev/cloudrun/container/hello"
    }
  }
  depends_on = [google_project_service.run_api]
}

# --- Cloud Run: FRONTEND ---
resource "google_cloud_run_v2_service" "frontend" {
  name     = "frontend-app"
  location = var.region
  deletion_protection = false
  ingress = "INGRESS_TRAFFIC_ALL"

  template {
    containers {
      image = "us-docker.pkg.dev/cloudrun/container/hello"
    }
  }
  depends_on = [google_project_service.run_api]
}

resource "google_cloud_run_service_iam_member" "frontend_public_access" {
  service  = google_cloud_run_v2_service.frontend.name
  location = google_cloud_run_v2_service.frontend.location
  role     = "roles/run.invoker"
  member   = "allUsers"
}

# --- Scheduler ---
resource "google_service_account" "scheduler_sa" {
  account_id   = "scheduler-sa"
  display_name = "Cloud Scheduler Service Account"
}

resource "google_cloud_run_service_iam_member" "scheduler_invoker" {
  service  = google_cloud_run_v2_service.backend.name
  location = google_cloud_run_v2_service.backend.location
  role     = "roles/run.invoker"
  member   = "serviceAccount:${google_service_account.scheduler_sa.email}"
}

resource "google_cloud_scheduler_job" "price_update_job" {
  name             = "price-update-cron"
  description      = "Triggers price update"
  schedule         = var.cron_schedule
  time_zone        = "Europe/Warsaw"
  attempt_deadline = "320s"
  region           = var.region

  retry_config {
    retry_count = 1
  }

  http_target {
    http_method = "POST"
    uri         = "${google_cloud_run_v2_service.backend.uri}/api/cron/update-prices"

    oidc_token {
      service_account_email = google_service_account.scheduler_sa.email
    }
  }

  depends_on = [google_project_service.scheduler_api, google_cloud_run_v2_service.backend]
}

# --- Mail Sender ---
variable "smtp_user" { type = string }
variable "smtp_pass" { type = string }
variable "smtp_sender" { type = string }

resource "google_project_service" "eventarc_api" {
  service            = "eventarc.googleapis.com"
  disable_on_destroy = false
}

resource "google_storage_bucket" "function_bucket" {
  name                        = "${var.project_id}-gcf-source"
  location                    = var.region
  uniform_bucket_level_access = true
}

data "archive_file" "email_sender_zip" {
  type        = "zip"
  source_dir  = "../email-sender"
  output_path = "/tmp/email-sender.zip"
}

resource "google_storage_bucket_object" "function_source" {
  name   = "email-sender-${data.archive_file.email_sender_zip.output_md5}.zip"
  bucket = google_storage_bucket.function_bucket.name
  source = data.archive_file.email_sender_zip.output_path
}

resource "google_cloudfunctions2_function" "email_sender" {
  name        = "email-sender-func"
  location    = var.region
  description = "Wysyła maile z Pub/Sub"

  build_config {
    runtime     = "python310"
    entry_point = "send_email_pubsub"
    source {
      storage_source {
        bucket = google_storage_bucket.function_bucket.name
        object = google_storage_bucket_object.function_source.name
      }
    }
  }

  service_config {
    max_instance_count = 1
    available_memory   = "256M"

    environment_variables = {
      SMTP_USER   = var.smtp_user
      SMTP_PASS   = var.smtp_pass
      SMTP_SENDER = var.smtp_sender
    }
  }

  event_trigger {
    trigger_region = var.region
    event_type     = "google.cloud.pubsub.topic.v1.messagePublished"
    pubsub_topic   = google_pubsub_topic.email_topic.id
    retry_policy   = "RETRY_POLICY_RETRY"
    service_account_email = google_service_account.backend_sa.email # Używamy istniejącego SA lub utwórz nowe
  }

  depends_on = [google_project_service.eventarc_api]
}

resource "google_cloud_run_service_iam_member" "invoker" {
  location = google_cloudfunctions2_function.email_sender.location
  service  = google_cloudfunctions2_function.email_sender.name
  role     = "roles/run.invoker"
  member   = "allUsers"
}

# --- Outputs ---
output "db_connection_name" {
  value = google_sql_database_instance.instance.connection_name
}

output "db_name" {
  value = google_sql_database.database.name
}

output "db_password_output" {
  value     = var.db_password
  sensitive = true
}

output "project_id" {
  value = var.project_id
}

output "region" {
  value = var.region
}

output "pubsub_topic_name" {
  value = google_pubsub_topic.email_topic.id
}