output "instance_id" {
  description = "EC2 instance id for the DESOFS production host."
  value       = aws_instance.app_host.id
}

output "public_ip" {
  description = "Elastic IP assigned to the DESOFS production host."
  value       = aws_eip.app_host.public_ip
}

output "public_dns" {
  description = "Public DNS hostname assigned by AWS for the DESOFS production host."
  value       = aws_instance.app_host.public_dns
}

output "ssh_user" {
  description = "Default SSH user for the Ubuntu AMI."
  value       = local.ssh_user
}

output "deploy_root" {
  description = "Deployment root expected by the GitHub Actions deployment job."
  value       = var.deploy_root
}

output "app_base_url" {
  description = "Public base URL exposed by the instance security group."
  value       = format("http://%s%s", aws_eip.app_host.public_ip, var.app_published_port == 80 ? "" : format(":%d", var.app_published_port))
}

output "healthcheck_url" {
  description = "EC2-local readiness endpoint used by the remote deployment and rollback scripts."
  value       = format("http://127.0.0.1:%d/actuator/health/readiness", var.management_port)
}

output "grafana_url" {
  description = "Public Grafana URL when Grafana ingress is enabled."
  value       = format("http://%s:%d", aws_eip.app_host.public_ip, var.grafana_published_port)
}

output "ssh_known_hosts_command" {
  description = "Run this command after apply to generate the AWS_SSH_KNOWN_HOSTS secret value."
  value       = format("ssh-keyscan -H %s", aws_eip.app_host.public_ip)
}

output "github_actions_deploy_values" {
  description = "Non-sensitive GitHub Actions deployment values derived from the provisioned host."
  value = {
    AWS_SSH_HOST        = aws_eip.app_host.public_ip
    AWS_SSH_PORT        = 22
    AWS_SSH_USER        = local.ssh_user
    AWS_APP_BASE_URL    = format("http://%s%s", aws_eip.app_host.public_ip, var.app_published_port == 80 ? "" : format(":%d", var.app_published_port))
    AWS_HEALTHCHECK_URL = format("http://127.0.0.1:%d/actuator/health/readiness", var.management_port)
    DEPLOY_ROOT         = var.deploy_root
  }
}
