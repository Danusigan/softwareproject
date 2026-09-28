# Non-secret connection details go into a generated Ansible inventory. Secrets stay in
# Terraform state only; Ansible reads them at run time via `terraform output -json`.
resource "local_file" "ansible_inventory" {
  filename        = "${path.module}/../ansible/inventory/hosts.ini"
  file_permission = "0644"
  content = templatefile("${path.module}/templates/hosts.ini.tftpl", {
    jenkins_public_ip    = aws_eip.jenkins.public_ip
    jenkins_private_ip   = aws_instance.jenkins.private_ip
    app_public_ip        = aws_eip.app.public_ip
    app_private_ip       = aws_instance.app.private_ip
    ssh_private_key_path = var.ssh_private_key_path
  })
}

output "jenkins_url" {
  value = "http://${aws_eip.jenkins.public_ip}:8080"
}

output "app_url" {
  value = "http://${aws_eip.app.public_ip}"
}

output "app_public_ip" {
  value = aws_eip.app.public_ip
}

output "app_private_ip" {
  value = aws_instance.app.private_ip
}

output "db_host" {
  value = aws_db_instance.mysql.address
}

output "db_name" {
  value = aws_db_instance.mysql.db_name
}

output "db_master_username" {
  value = aws_db_instance.mysql.username
}

output "db_master_password" {
  value     = random_password.db_master.result
  sensitive = true
}

output "db_app_password" {
  value     = random_password.db_app.result
  sensitive = true
}

output "jwt_secret" {
  value     = random_password.jwt_secret.result
  sensitive = true
}
