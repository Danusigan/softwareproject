resource "random_password" "db_master" {
  length  = 32
  special = false
}

# The application connects as a separate, schema-scoped user (created by Ansible), not the
# RDS master user.
resource "random_password" "db_app" {
  length  = 32
  special = false
}

resource "random_password" "jwt_secret" {
  length  = 64
  special = false
}

resource "aws_db_subnet_group" "main" {
  name       = "${var.project}-db"
  subnet_ids = aws_subnet.private[*].id
}

resource "aws_db_parameter_group" "mysql8" {
  name   = "${var.project}-mysql80"
  family = "mysql8.0"

  # Reject any unencrypted connection.
  parameter {
    name  = "require_secure_transport"
    value = "1"
  }

  parameter {
    name  = "time_zone"
    value = "UTC"
  }
}

resource "aws_db_instance" "mysql" {
  identifier     = "${var.project}-mysql"
  engine         = "mysql"
  engine_version = "8.0"
  instance_class = var.db_instance_class

  allocated_storage     = var.db_allocated_storage
  max_allocated_storage = var.db_max_allocated_storage
  storage_type          = "gp3"
  storage_encrypted     = true

  db_name  = var.db_name
  username = "lopo_admin"
  password = random_password.db_master.result

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.db.id]
  parameter_group_name   = aws_db_parameter_group.mysql8.name
  publicly_accessible    = false
  multi_az               = var.db_multi_az

  backup_retention_period    = var.db_backup_retention_days
  backup_window              = "19:00-20:00"         # 00:30-01:30 Sri Lanka time
  maintenance_window         = "sun:20:30-sun:21:30" # after the backup window
  copy_tags_to_snapshot      = true
  auto_minor_version_upgrade = true

  deletion_protection       = var.db_deletion_protection
  skip_final_snapshot       = false
  final_snapshot_identifier = "${var.project}-mysql-final"

  tags = { Name = "${var.project}-mysql" }
}
