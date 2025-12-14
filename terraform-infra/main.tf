terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = ">= 4.51.0"
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
      authorized_networks {
        name  = "all"
        value = "0.0.0.0/0"
      }
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

# Java backend
resource "google_cloud_run_v2_service" "backend" {
  name     = "price-processor-backend"
  location = var.region
  deletion_protection = false
  ingress = "INGRESS_TRAFFIC_ALL"

  template {
    containers {
      image = "us-docker.pkg.dev/cloudrun/container/hello" # Placeholder

      env {
        name  = "DB_URL"
        value = "jdbc:mysql://${google_sql_database_instance.instance.public_ip_address}:3306/price_processor_db?allowPublicKeyRetrieval=true&useSSL=false"
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

resource "google_cloud_run_service_iam_member" "public_access" {
  service  = google_cloud_run_v2_service.backend.name
  location = google_cloud_run_v2_service.backend.location
  role     = "roles/run.invoker"
  member   = "allUsers"
}


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