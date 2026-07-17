# AWS Terraform Deployment

This directory provisions the AWS infrastructure used by the production deployment flow.

The production environment under `envs/prod` creates:

- a dedicated VPC in `eu-west-3`
- one public subnet and internet gateway
- a least-privilege security group
- one Ubuntu 24.04 EC2 instance with:
  - encrypted EBS root volume
  - IMDSv2 enforcement
  - EC2 detailed monitoring
  - an IAM instance profile for AWS Systems Manager Session Manager
  - Docker Engine, Docker Compose plugin, Doppler CLI, and Docker Swarm bootstrapped by cloud-init
- one Elastic IP so the deployment target remains stable for GitHub secrets and DNS

## Prerequisites

1. An AWS account with access to `eu-west-3`.
2. Local AWS credentials configured for Terraform, for example with `aws configure` or `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY`.
3. Terraform installed locally.
4. A Docker Hub repository for the published image.
5. A Doppler project/config containing the production application secrets.
6. A dedicated SSH keypair for deployment access.

## Recommended Setup

1. Generate a dedicated SSH keypair:

   ```bash
   ssh-keygen -t ed25519 -f ~/.ssh/desofs-prod -C "desofs-prod"
   ```

2. Create your environment file from the example:

   ```bash
   cd infrastructure/terraform/envs/prod
   cp terraform.tfvars.example terraform.tfvars
   ```

3. Edit `terraform.tfvars` and replace at least:

   - `allowed_ssh_cidrs`
   - `deployment_ssh_public_key`
   - `allowed_grafana_cidrs` if you want Grafana to be publicly reachable
   - `common_tags`
   - `instance_type` if you intentionally want something smaller or larger

4. Initialize and validate the configuration:

   ```bash
   terraform init
   terraform fmt -check
   terraform validate
   ```

5. Review the execution plan:

   ```bash
   terraform plan
   ```

6. Apply the infrastructure:

   ```bash
   terraform apply
   ```

7. Capture the deployment outputs:

   ```bash
   terraform output
   ```

8. Generate the `known_hosts` value for GitHub Actions:

   ```bash
   ssh-keyscan -H "$(terraform output -raw public_ip)"
   ```

## What You Must Configure After `terraform apply`

1. Add the GitHub Actions secrets listed in the repository README and Sprint 1 CI/CD docs.
2. Store the private half of your SSH keypair in `AWS_SSH_PRIVATE_KEY`.
3. Store the `ssh-keyscan` output in `AWS_SSH_KNOWN_HOSTS`.
4. Store `terraform output -raw public_ip` in `AWS_SSH_HOST`.
5. Store `terraform output -raw app_base_url` in `AWS_APP_BASE_URL`.
   If `app_published_port` is not `80`, this value must include the port, for example `http://<public-app-host>:8080`.
6. Store `terraform output -raw healthcheck_url` in `AWS_HEALTHCHECK_URL`.

   This output targets the management port on `127.0.0.1` because actuator endpoints are not exposed on the public application port.
7. Store `terraform output -raw ssh_user` in `AWS_SSH_USER`.
8. Store `22` in `AWS_SSH_PORT`.
9. Ensure Doppler contains the production secrets required by `deployment/docker-compose.swarm.yml`, including `APP_PUBLISHED_PORT=80` unless you intentionally choose another port.
   If you intentionally choose another port, keep Terraform `app_published_port`, Doppler `APP_PUBLISHED_PORT`, and GitHub `AWS_APP_BASE_URL` aligned.
10. If you enabled public Grafana access, use `terraform output -raw grafana_url` to confirm the reachable URL.

## Validation Commands

Use these commands locally after editing the Terraform:

```bash
cd infrastructure/terraform/envs/prod
terraform fmt -recursive
terraform validate
```

## Additional resources
Look at the [`deployment/README.md`](../../deployment/README.md) for instructions on how to deploy the application to the provisioned infrastructure and manage it after deployment.
