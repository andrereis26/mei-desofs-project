variable "aws_region" {
  description = "AWS region used for the production environment."
  type        = string
  default     = "eu-west-3"
}

variable "project_name" {
  description = "Short project identifier used in AWS naming and tagging."
  type        = string
  default     = "desofs"
}

variable "environment" {
  description = "Environment name used in AWS naming and tagging."
  type        = string
  default     = "prod"
}

variable "vpc_cidr" {
  description = "CIDR block for the dedicated VPC."
  type        = string
  default     = "10.42.0.0/16"
}

variable "public_subnet_cidr" {
  description = "CIDR block for the public subnet hosting the production instance."
  type        = string
  default     = "10.42.10.0/24"
}

variable "availability_zone" {
  description = "Optional explicit availability zone inside eu-west-3. Leave null to use the first available zone."
  type        = string
  default     = null
}

variable "instance_type" {
  description = "EC2 instance type for the production host. Keep it aligned with the memory footprint of Docker Swarm plus database and Redis sidecars."
  type        = string
  default     = "t3.small"
}

variable "root_volume_size_gb" {
  description = "Root EBS volume size in GiB."
  type        = number
  default     = 30

  validation {
    condition     = var.root_volume_size_gb >= 20
    error_message = "root_volume_size_gb must be at least 20 GiB to leave room for Docker images, logs, and persistent data."
  }
}

variable "allowed_ssh_cidrs" {
  description = "CIDR blocks allowed to SSH into the instance. Restrict this to trusted operator or GitHub runner egress ranges that you control."
  type        = list(string)

  validation {
    condition     = length(var.allowed_ssh_cidrs) > 0
    error_message = "allowed_ssh_cidrs must contain at least one trusted CIDR so the deployment host remains reachable."
  }
}

variable "allowed_app_cidrs" {
  description = "CIDR blocks allowed to reach the published application port."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "app_published_port" {
  description = "Public host port published by the Docker Swarm ingress mesh."
  type        = number
  default     = 80

  validation {
    condition     = var.app_published_port >= 1 && var.app_published_port <= 65535
    error_message = "app_published_port must be a valid TCP port."
  }
}

variable "grafana_published_port" {
  description = "Public host port published for Grafana when enabled."
  type        = number
  default     = 3000

  validation {
    condition     = var.grafana_published_port >= 1 && var.grafana_published_port <= 65535
    error_message = "grafana_published_port must be a valid TCP port."
  }
}

variable "allowed_grafana_cidrs" {
  description = "CIDR blocks allowed to reach the Grafana UI. Set to an empty list to keep Grafana private."
  type        = list(string)
  default     = []
}

variable "management_port" {
  description = "Port used by the Spring Boot management server for readiness and metrics endpoints."
  type        = number
  default     = 9090

  validation {
    condition     = var.management_port >= 1 && var.management_port <= 65535
    error_message = "management_port must be a valid TCP port."
  }
}

variable "deployment_ssh_public_key" {
  description = "Public SSH key used for the EC2 key pair. The matching private key is stored in GitHub Actions as AWS_SSH_PRIVATE_KEY."
  type        = string

  validation {
    condition     = can(regex("^ssh-(rsa|ed25519|ecdsa)", var.deployment_ssh_public_key))
    error_message = "deployment_ssh_public_key must be a valid OpenSSH public key."
  }
}

variable "deploy_root" {
  description = "Directory on the EC2 host where the GitHub Actions workflow syncs deployment assets."
  type        = string
  default     = "/opt/desofs-api"
}

variable "enable_detailed_monitoring" {
  description = "Enable EC2 detailed monitoring for faster CloudWatch datapoints."
  type        = bool
  default     = true
}

variable "common_tags" {
  description = "Additional AWS tags applied to provisioned resources."
  type        = map(string)
  default     = {}
}
