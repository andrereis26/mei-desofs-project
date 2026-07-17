# Backup & Restore Runbook (NFR-012)

## Purpose
This document describes how system backups and restores are performed for PostgreSQL, Redis, and Docker volumes. It ensures the system can be recovered in case of failure.

---

## Backup System Overview

Backups are executed using the following script:

```bash
/scripts/backup.sh
```

### Behavior
- Runs automatically via cron (daily at 02:00)
- Can be executed manually at any time
- Creates timestamped backups for all critical services

---

## Manual Backup Execution

To manually run a backup:

```bash
chmod +x /scripts/backup.sh
/scripts/backup.sh
```

---

## Backup Storage Locations

### PostgreSQL
```
/backups/postgres/
```

### Redis
```
/backups/redis/
```

### Docker Volumes
```
/backups/volumes/
```

### Logs
```
/backups/logs/backup.log
```

---

## Retention Policy

Backups older than 7 days are automatically deleted.

### Cleanup commands

```bash
find /backups/postgres -type f -mtime +7 -delete
find /backups/redis -type f -mtime +7 -delete
find /backups/volumes -type f -mtime +7 -delete
```

---

## Restore Procedures (STAGING ONLY)

⚠ Always perform restore operations in staging before production.

---

## PostgreSQL Restore

### Command

```bash
gunzip -c postgres_backup.sql.gz | docker exec -i db psql -U postgres
```

### Validation

```sql
SELECT COUNT(*) FROM users;
```

Check:
- Tables exist
- Data is present
- Application connects successfully

---

## Redis Restore

### Rate Limit Redis

```bash
docker cp redis-rate-limit_*.rdb redis-rate-limit:/data/dump.rdb
docker restart redis-rate-limit
```

### Token Revocation Redis

```bash
docker cp redis-token-revocation_*.rdb redis-token-revocation:/data/dump.rdb
docker restart redis-token-revocation
```

### Validation
- Application starts successfully
- Authentication works correctly
- Rate limiting behaves correctly

---

## Docker Volume Restore

### Example (app-storage)

```bash
docker run --rm \
  -v app-storage:/data \
  -v /backups/volumes:/backup \
  alpine \
  sh -c "tar xzf /backup/app-storage_backup.tar.gz -C /data"
```

---

## Restore Validation Checklist

After restoring, verify:

### Database
- Tables exist
- Data is intact

### Application
- Service starts successfully
- Login works
- APIs respond correctly

### Storage
- Files are present and accessible

---

## Failure Troubleshooting

If backups or restores fail, check logs:

```bash
cat /backups/logs/backup.log
```

Also verify:
- Docker containers are running
- Sufficient disk space is available
- Backup directories exist and are writable

---

## Notes

- Backups are stored locally on the host machine
- Retention period is 7 days
- No external storage (e.g. S3) is configured
- Restore testing must be manually performed in staging