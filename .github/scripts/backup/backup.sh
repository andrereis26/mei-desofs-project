#!/bin/bash

set -euo pipefail

# ----------------------------
# CONFIG
# ----------------------------
DATE=$(date +%F_%H-%M)
BACKUP_ROOT="/backups"

POSTGRES_CONTAINER="db"

REDIS_CONTAINERS=(
  "redis-rate-limit"
  "redis-token-revocation"
)

VOLUMES=(
  "postgres-data"
  "redis-rate-limit-data"
  "redis-token-revocation-data"
  "app-storage"
)

# ----------------------------
# INIT
# ----------------------------
mkdir -p $BACKUP_ROOT/{postgres,redis,volumes,logs}

exec >> $BACKUP_ROOT/logs/backup.log 2>&1

log() {
  echo "[$(date +'%F %T')] $1"
}

log "=== BACKUP START: $DATE ==="

# ----------------------------
# POSTGRES BACKUP
# ----------------------------
log "Starting PostgreSQL backup..."

docker exec $POSTGRES_CONTAINER pg_dumpall -U postgres | gzip > \
  $BACKUP_ROOT/postgres/postgres_$DATE.sql.gz

log "PostgreSQL backup completed."

# ----------------------------
# REDIS BACKUPS
# ----------------------------
for redis in "${REDIS_CONTAINERS[@]}"; do
  log "Backing up Redis: $redis"

  docker exec $redis redis-cli BGSAVE

  # allow snapshot to flush to disk
  sleep 10

  docker cp $redis:/data/dump.rdb \
    $BACKUP_ROOT/redis/${redis}_$DATE.rdb

  log "Redis backup completed: $redis"
done

# ----------------------------
# VOLUME BACKUPS
# ----------------------------
log "Starting volume backups..."

for vol in "${VOLUMES[@]}"; do
  log "Backing up volume: $vol"

  docker run --rm \
    -v ${vol}:/data:ro \
    -v $BACKUP_ROOT/volumes:/backup \
    alpine \
    tar czf /backup/${vol}_$DATE.tar.gz -C /data .

  log "Volume backup completed: $vol"
done

# ----------------------------
# RETENTION POLICY (7 days)
# ----------------------------
log "Applying retention policy (7 days)..."

find $BACKUP_ROOT/postgres -type f -mtime +7 -delete
find $BACKUP_ROOT/redis -type f -mtime +7 -delete
find $BACKUP_ROOT/volumes -type f -mtime +7 -delete

log "Retention cleanup completed."

# ----------------------------
# FINISH
# ----------------------------
log "=== BACKUP SUCCESS: $DATE ==="