# Production Deployment

This directory contains the production deployment assets used by the main GitHub Actions workflow:

- `docker-compose.swarm.yml`: Docker Swarm stack definition for PostgreSQL, Redis, and the DESOFS API.
- `remote-deploy.sh`: Server-side deployment script that renders the stack with Doppler, deploys it to Swarm, validates readiness, and rolls back on failure.
- `remote-rollback.sh`: Server-side rollback script that triggers a Swarm rollback for the API service and waits for readiness.

## Target Runtime

- Single AWS EC2 host in `eu-west-3`, provisioned by `infrastructure/terraform/envs/prod`.
- Cloud-init installs Docker Engine, Docker Compose plugin, Doppler CLI, and initializes Docker Swarm during instance bootstrap.
- Doppler must contain the production values required by `deployment/docker-compose.swarm.yml`.

## Provisioning and Host Preparation

1. Provision the AWS infrastructure first:

   ```bash
   cd infrastructure/terraform/envs/prod
   cp terraform.tfvars.example terraform.tfvars
   terraform init
   terraform plan
   terraform apply
   ```

2. After `terraform apply`, verify the host bootstrap:

   ```bash
   ssh -i ~/.ssh/desofs-prod ubuntu@"$(terraform output -raw public_ip)" docker --version
   ssh -i ~/.ssh/desofs-prod ubuntu@"$(terraform output -raw public_ip)" docker compose version
   ssh -i ~/.ssh/desofs-prod ubuntu@"$(terraform output -raw public_ip)" docker info --format '{{.Swarm.LocalNodeState}}'
   ssh -i ~/.ssh/desofs-prod ubuntu@"$(terraform output -raw public_ip)" doppler --version
   ssh -i ~/.ssh/desofs-prod ubuntu@"$(terraform output -raw public_ip)" curl --version
   ```

3. Also verify that the Docker runtime is compatible with this deployment model:

   ```bash
   ssh -i ~/.ssh/desofs-prod ubuntu@"$(terraform output -raw public_ip)" \
     "docker info | grep -E 'Storage Driver|Backing Filesystem|Supports d_type|Native Overlay Diff|Docker Root Dir|Security Options|rootless|Rootless|Kernel Version'"
   ```

   Expected posture:
   - Disable `live-restore` with `sudo nano /etc/docker/daemon.json` and change the `live-restore` value to `false` if it is `true`.
      - Then restart Docker with `sudo systemctl restart docker` and verify `live-restore` is `false` in `docker info`.
   - Docker Swarm is active.
   - Docker is not running in rootless mode.
   - The host storage backend supports Docker layer extraction without overlay mount permission errors.

4. Confirm the deployment user can run Docker commands:

   ```bash
   ssh -i ~/.ssh/desofs-prod ubuntu@"$(terraform output -raw public_ip)" docker ps
   ```

5. Create a Doppler service token with the minimum required access to the production project/config and store it in the GitHub secret `DOPPLER_TOKEN`.

## Manual Deployment Flow

The CI workflow syncs this directory to the Terraform-provisioned EC2 host and then runs:

```bash
STACK_NAME=desofs \
DEPLOY_ROOT=/opt/desofs-api \
APP_IMAGE=docker.io/<dockerhub-username>/desofs-api:<version-tag> \
DOCKERHUB_USERNAME=<dockerhub-username> \
DOCKERHUB_TOKEN=<dockerhub-token> \
HEALTHCHECK_URL=http://127.0.0.1:9090/actuator/health/readiness \
DOPPLER_PROJECT=<doppler-project> \
DOPPLER_CONFIG=<doppler-config> \
DOPPLER_TOKEN=<doppler-service-token> \
bash /opt/desofs-api/deployment/remote-deploy.sh
```

Use the Terraform outputs to populate the matching GitHub secrets:

- `terraform output -raw public_ip` -> `AWS_SSH_HOST`
- `terraform output -raw ssh_user` -> `AWS_SSH_USER`
- `22` -> `AWS_SSH_PORT`
- `terraform output -raw app_base_url` -> `AWS_APP_BASE_URL`
- `terraform output -raw healthcheck_url` -> `AWS_HEALTHCHECK_URL` for EC2-local deploy and rollback readiness checks
- `ssh-keyscan -H "$(terraform output -raw public_ip)"` -> `AWS_SSH_KNOWN_HOSTS`

`AWS_APP_BASE_URL` must point to the public application port reachable from GitHub-hosted runners. If the application is published on `8080`, use a value like `http://<aws-elastic-ip>:8080`; the fallback `http://AWS_SSH_HOST` only works when the application is publicly reachable on port `80`.

## Why the Deployment Script Renders Then Uses `docker stack deploy`

The production compose file uses Swarm-only `deploy` settings such as:

- `replicas: 2`
- `update_config.parallelism: 1`
- `update_config.order: start-first`
- rollback-aware restart and update policies

Those controls are ignored by plain `docker compose up -d`. For that reason, the deployment script uses Doppler to render the compose file first and then applies the rendered stack with `docker stack deploy`, which is the Docker-supported path for Swarm rolling updates and rollback behavior.

## Required Doppler Variables

At minimum, the production Doppler config must provide values for:

- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `JWT_SECRET`
- `RATE_LIMIT_REDIS_PASSWORD`
- `TOKEN_REVOCATION_REDIS_PASSWORD`
- `GRAFANA_ADMIN_USER`
- `GRAFANA_ADMIN_PASSWORD`

Optional but supported Doppler-managed overrides include:

- `JWT_EXPIRATION`
- `FILE_MAX_SIZE_BYTES`
- `AUTH_RATE_LIMIT_MAX_PER_IP`
- `AUTH_RATE_LIMIT_WINDOW_SECONDS`
- `REGISTER_RATE_LIMIT_MAX_PER_IP`
- `REGISTER_RATE_LIMIT_WINDOW_SECONDS`
- `AUTH_RATE_LIMIT_MAX_PER_IDENTITY`
- `REGISTER_RATE_LIMIT_MAX_PER_IDENTITY`
- `TOKEN_REVOCATION_REDIS_DATABASE`
- `TOKEN_REVOCATION_REDIS_TIMEOUT`
- `TOKEN_REVOCATION_REDIS_SSL`
- `TOKEN_REVOCATION_REDIS_KEY_PREFIX`
- `RATE_LIMIT_REDIS_TIMEOUT`
- `RATE_LIMIT_REDIS_CONNECT_TIMEOUT`
- `RATE_LIMIT_REDIS_SSL`
- `APP_PUBLISHED_PORT`
- `APP_REPLICAS`
- `GRAFANA_PUBLISHED_PORT`
- `MANAGEMENT_PORT`
- `JAVA_TOOL_OPTIONS` (for example, to set memory limits or GC options)

Keep `MANAGEMENT_PORT` aligned with the `management_port` value in Terraform so deployment health checks remain consistent.

For the default AWS security-group setup, set `APP_PUBLISHED_PORT=80`.
If you intentionally expose the application on another port, keep Terraform `app_published_port`, Doppler `APP_PUBLISHED_PORT`, and the GitHub secret `AWS_APP_BASE_URL` aligned. For example, with port `8080`, Terraform should open `8080`, Doppler should set `APP_PUBLISHED_PORT=8080`, and `AWS_APP_BASE_URL` should be `http://<aws-elastic-ip>:8080`.

## Operational Notes

- The API service is constrained to the Swarm manager node because the application currently uses local persistent storage for uploaded documents.
- The API service publishes application and management ports in host mode to avoid routing-mesh ambiguity on the single-node manager deployment. It defaults to one replica because fixed host ports cannot be shared by multiple replicas on the same node.
- Actuator health and Prometheus metrics are served on the management port (default `9090`) and are not exposed on the public application port.
- GitHub-hosted post-deploy validation uses `AWS_APP_BASE_URL` to reach the public application port. It must not use the EC2-local `AWS_HEALTHCHECK_URL`.
- A quick external validation is `curl -i <AWS_APP_BASE_URL>/api/users`; a reachable deployment should return `401` or `403` without a connection timeout.
- If the deployment validation fails, `remote-deploy.sh` triggers `docker service rollback` for the API service.
- `remote-deploy.sh` performs a Docker runtime preflight and prints storage-driver diagnostics when image pull or unpack fails.

## Automated Rollback on Post-Deploy Validation Failure

The production workflow runs post-deploy validation after a successful rollout. When those checks fail, the pipeline connects to the Swarm host and runs `remote-rollback.sh` to roll back the API service to the previous version and wait for readiness before the workflow finishes.

## Observability Access

- Grafana is published on port `3000` by default; override via `GRAFANA_PUBLISHED_PORT`.
- The Grafana instance is pre-provisioned with Prometheus and Loki data sources and the DESOFS overview dashboard.
- If you change `grafana_published_port` in Terraform, keep `GRAFANA_PUBLISHED_PORT` in Doppler aligned with the same value.
- Example URL: `http://<aws-elastic-ip>:3000`

### Private Access via SSH Tunnel

If you prefer to keep Grafana private, remove its public ingress in Terraform (see below) and access it via SSH tunnel instead:

```bash
ssh -i ~/.ssh/desofs-prod -L 3000:localhost:3000 ubuntu@<aws-elastic-ip>
```

Then open `http://localhost:3000` in your browser. If you changed the published port, replace both `3000` values with your `GRAFANA_PUBLISHED_PORT`.

### Remove Public Access

To remove public access to Grafana, set `allowed_grafana_cidrs = []` in `infrastructure/terraform/envs/prod/terraform.tfvars` and run `terraform apply`.
You can still access Grafana through the SSH tunnel described above.

## CI Email Notifications

The production GitHub Actions pipeline sends an email notification after each deployment attempt indicating whether the deployment passed post-deploy validation or failed (and whether a rollback ran).

Notifications are created as GitHub Issues by the pipeline (no external secrets required).

The `notify-deployment` job opens an Issue on deployment result using `actions/github-script`. It mentions all CODEOWNERS in the issue body as deployment reviewers and attempts to assign individual CODEOWNERS users to the issue. Repository watchers and participants receive notifications according to their GitHub notification settings.

These are used by the `notify-deployment` job in `.github/workflows/main-workflow.yml` which runs with `if: always()` so email is sent even when the pipeline fails.
