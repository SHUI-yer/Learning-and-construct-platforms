# Flyway migration rules

- `V1__baseline_schema.sql` is the immutable schema used for a brand-new database.
- An existing production database is registered at version 1 only during the one-time baseline procedure documented in `docs/DEPLOYMENT_RUNBOOK.md`.
- Never edit a migration that has run in any shared environment. Add the next migration, for example `V2__add_course_index.sql`.
- Prefer backward-compatible expand/contract changes: add nullable columns or new tables first, deploy compatible code, backfill, and remove old structures in a later release.
- Flyway Community migrations are forward-only. A destructive schema rollback requires restoring the verified pre-deployment database backup.
