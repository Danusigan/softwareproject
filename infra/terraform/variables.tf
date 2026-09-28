variable "project" {
  type    = string
  default = "lopo"
}

variable "aws_region" {
  type    = string
  default = "ap-south-1"
}

variable "admin_cidrs" {
  description = "CIDRs allowed to SSH into the servers, e.g. [\"203.0.113.10/32\"] (your public IP)."
  type        = list(string)
}

variable "ssh_public_key_path" {
  description = "Public half of the key used to SSH in as 'ubuntu' (Ansible uses the private half)."
  type        = string
  default     = "~/.ssh/lopo_deploy.pub"
}

variable "ssh_private_key_path" {
  description = "Written into the generated Ansible inventory."
  type        = string
  default     = "~/.ssh/lopo_deploy"
}

variable "jenkins_instance_type" {
  type    = string
  default = "t3.medium"
}

variable "app_instance_type" {
  type    = string
  default = "t3.small"
}

variable "db_instance_class" {
  type    = string
  default = "db.t4g.micro"
}

variable "db_allocated_storage" {
  description = "Initial storage in GB; RDS grows it automatically up to db_max_allocated_storage."
  type        = number
  default     = 20
}

variable "db_max_allocated_storage" {
  type    = number
  default = 100
}

variable "db_name" {
  type    = string
  default = "obqa"
}

variable "db_multi_az" {
  description = "Standby replica in a second AZ. Roughly doubles DB cost; enable if the client needs high availability."
  type        = bool
  default     = false
}

variable "db_backup_retention_days" {
  type    = number
  default = 7
}

variable "db_deletion_protection" {
  description = "Must be set to false (and applied) before the database can be destroyed."
  type        = bool
  default     = true
}
