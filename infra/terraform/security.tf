resource "aws_security_group" "app" {
  name        = "${var.project}-app"
  description = "Application server (nginx + Spring Boot)"
  vpc_id      = aws_vpc.main.id

  # GitHub Actions deploys don't get a permanent rule here: each deploy adds a temporary
  # /32 rule for its own runner IP and removes it afterwards (see github.tf and
  # .github/workflows/ci-cd.yml). Any leftover rule is removed by the next terraform apply.
  ingress {
    description = "SSH from admins"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = var.admin_cidrs
  }

  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "db" {
  name        = "${var.project}-db"
  description = "RDS MySQL - reachable only from the app server"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "MySQL from app server"
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]
  }
}
