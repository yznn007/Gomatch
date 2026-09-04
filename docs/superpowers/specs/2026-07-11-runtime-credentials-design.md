# Runtime Credentials Configuration Design

## Goal

Remove committed runtime account credentials and secrets from the application configuration while keeping seed data non-personal and injecting its local password through `.env` at initialization time.

## Scope

- Replace the committed MySQL username and password in both runtime `database.properties` files with environment-variable placeholders.
- Load a repository-root `.env` file for local development without adding its values to version control.
- Allow process environment variables to override `.env` values for CI, containers, and production deployments.
- Cover MySQL, Redis, and OSS credentials. Non-secret connection settings remain configurable through the same override mechanism where supported.
- Add a tracked `.env.example` containing only variable names and non-sensitive example values.
- Update startup/deployment documentation and scripts so they rely on `.env` or process environment variables rather than embedded credentials.

## Exclusions

- `sql/init.sql` contains no database-account creation credentials or seed email addresses; initialization scripts inject the local seed password from `.env`.
- User login, registration, password-reset, and room-password protocol behavior is not changed.
- This change does not introduce a new external secret-management service.

## Configuration Resolution

The server and database modules use a shared resolver with the following precedence:

1. Process environment variables.
2. Repository-root `.env` file (or a path supplied through `GOMATCH_ENV_FILE`).
3. Non-sensitive committed defaults in `database.properties`.

The resolver accepts standard `KEY=value` lines, ignores blank lines and `#` comments, and never logs values. The `.env` file is optional only when every required variable is supplied by the process environment.

Required variables are `GOMATCH_DB_URL`, `GOMATCH_DB_USERNAME`, and `GOMATCH_DB_PASSWORD`. Redis requires `GOMATCH_REDIS_HOST`, `GOMATCH_REDIS_PORT`, and `GOMATCH_REDIS_PASSWORD`; the project treats Redis as a required runtime dependency. OSS access keys remain optional only when no OSS-backed storage operation is requested.

## Variables

| Area | Variables |
| --- | --- |
| MySQL | `GOMATCH_DB_URL`, `GOMATCH_DB_USERNAME`, `GOMATCH_DB_PASSWORD` |
| Redis | `GOMATCH_REDIS_HOST`, `GOMATCH_REDIS_PORT`, `GOMATCH_REDIS_PASSWORD`, `GOMATCH_REDIS_DATABASE` |
| OSS | `OSS_ENDPOINT`, `OSS_BUCKET`, `OSS_ACCESS_KEY_ID`, `OSS_ACCESS_KEY_SECRET` |

## Failure Handling

Startup validates required MySQL and Redis values before initializing connection pools or clients. A missing or blank value produces an actionable error naming the variable, without echoing credentials. No committed database credential fallback remains.

## Verification

- Unit-test precedence, parsing, and required-value validation using injected maps/files.
- Compile the server and database modules.
- Confirm committed configuration files, scripts, and Markdown contain no live credential values.
- Confirm `.env` is ignored and `.env.example` is tracked.
