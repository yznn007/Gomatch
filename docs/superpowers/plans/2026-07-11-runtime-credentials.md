# Runtime Credentials Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Load MySQL, Redis, and OSS runtime credentials from an ignored `.env` file or process environment, without committed credential fallbacks.

**Architecture:** A resolver in the `database` module serves both database and server consumers. It loads classpath defaults, a root `.env`, then process environment variables in increasing precedence; validates MySQL and Redis before connection creation; and returns one resolved `Properties` object.

**Tech Stack:** Java 21, Maven, JUnit 5, Java `Properties`, PowerShell.

---

## File structure

- `database/src/main/java/com/gomatch/database/config/RuntimeConfig.java`: shared parser, resolver, and validation.
- `database/src/test/java/com/gomatch/database/config/RuntimeConfigTest.java`: no-network parser, precedence, and validation tests.
- `server/src/main/java/com/gomatch/server/config/EnvironmentConfig.java`: removed after migration.
- `server/src/main/java/com/gomatch/server/dao/DBUtil.java`, `server/src/main/java/com/gomatch/server/redis/RedisManager.java`, `server/src/main/java/com/gomatch/server/storage/StorageManager.java`: shared resolved configuration consumers.
- `database/src/main/java/com/gomatch/database/config/DatabaseConnectionManager.java`: database module consumer.
- Both `database.properties` files, `.env.example`, `.gitignore`, server scripts, `README.md`, `docs/DEPLOY_GUIDE.md`, and `docs/DEV_LOG.md`: safe runtime workflow.

### Task 1: Implement and test the shared resolver

**Files:**

- Create: `database/src/main/java/com/gomatch/database/config/RuntimeConfig.java`
- Create: `database/src/test/java/com/gomatch/database/config/RuntimeConfigTest.java`
- Delete: `server/src/main/java/com/gomatch/server/config/EnvironmentConfig.java`
- Delete: `server/src/test/java/com/gomatch/server/config/EnvironmentConfigTest.java`

- [ ] **Step 1: Write the failing precedence and validation tests**

```java
@Test
void processEnvironmentOverridesDotEnvWhichOverridesClasspathProperties() {
    Properties base = new Properties();
    base.setProperty("mysql.username", "classpath-user");
    Properties resolved = RuntimeConfig.resolve(base,
        Map.of("GOMATCH_DB_USERNAME", "dotenv-user"),
        Map.of("GOMATCH_DB_USERNAME", "process-user"));
    assertEquals("process-user", resolved.getProperty("mysql.username"));
}

@Test
void rejectsMissingRequiredCredential() {
    IllegalStateException error = assertThrows(IllegalStateException.class,
        () -> RuntimeConfig.requireRuntimeCredentials(new Properties()));
    assertEquals("Missing required runtime configuration: GOMATCH_DB_URL", error.getMessage());
}
```

- [ ] **Step 2: Run the focused test to verify it fails**

Run: `mvn -pl database -Dtest=RuntimeConfigTest test`

Expected: compilation failure because `RuntimeConfig` does not exist.

- [ ] **Step 3: Implement `RuntimeConfig`**

```java
private static final Map<String, String> VARIABLES = Map.ofEntries(
    Map.entry("mysql.url", "GOMATCH_DB_URL"),
    Map.entry("mysql.username", "GOMATCH_DB_USERNAME"),
    Map.entry("mysql.password", "GOMATCH_DB_PASSWORD"),
    Map.entry("redis.host", "GOMATCH_REDIS_HOST"),
    Map.entry("redis.port", "GOMATCH_REDIS_PORT"),
    Map.entry("redis.password", "GOMATCH_REDIS_PASSWORD"),
    Map.entry("redis.database", "GOMATCH_REDIS_DATABASE"),
    Map.entry("oss.endpoint", "OSS_ENDPOINT"),
    Map.entry("oss.bucket", "OSS_BUCKET"),
    Map.entry("oss.access-key-id", "OSS_ACCESS_KEY_ID"),
    Map.entry("oss.access-key-secret", "OSS_ACCESS_KEY_SECRET"));

public static Properties resolve(Properties base, Map<String, String> dotEnv, Map<String, String> environment) {
    Properties resolved = new Properties();
    resolved.putAll(base);
    apply(resolved, dotEnv);
    apply(resolved, environment);
    return resolved;
}
```

Add `loadDotEnv(Path)` using UTF-8, blank/comment skipping, and split-at-first-`=` parsing. Add `loadRuntimeProperties(String)` that loads the classpath resource, then `$GOMATCH_ENV_FILE` or `<user.dir>/.env`, and resolves using `System.getenv()`. `requireRuntimeCredentials` must require the MySQL and Redis fields named in the approved design. Delete the server-only class and move its tests here.

- [ ] **Step 4: Run the focused test to verify it passes**

Run: `mvn -pl database -Dtest=RuntimeConfigTest test`

Expected: `BUILD SUCCESS` and all tests pass.

- [ ] **Step 5: Commit the shared resolver**

Run: `git add database/src/main/java/com/gomatch/database/config/RuntimeConfig.java database/src/test/java/com/gomatch/database/config/RuntimeConfigTest.java server/src/main/java/com/gomatch/server/config/EnvironmentConfig.java server/src/test/java/com/gomatch/server/config/EnvironmentConfigTest.java; git commit -m "feat(config): 统一加载运行时凭据"`

### Task 2: Migrate database, Redis, and OSS consumers

**Files:**

- Modify: `database/src/main/java/com/gomatch/database/config/DatabaseConnectionManager.java:31-57`
- Modify: `server/src/main/java/com/gomatch/server/dao/DBUtil.java:20-43`
- Modify: `server/src/main/java/com/gomatch/server/redis/RedisManager.java:30-50`
- Modify: `server/src/main/java/com/gomatch/server/storage/StorageManager.java:24-52`

- [ ] **Step 1: Add the Redis mapping regression test**

```java
@Test
void mapsRedisFieldsFromDotEnv() {
    Properties resolved = RuntimeConfig.resolve(new Properties(), Map.of(
        "GOMATCH_REDIS_HOST", "127.0.0.1",
        "GOMATCH_REDIS_PORT", "6379",
        "GOMATCH_REDIS_PASSWORD", "test-secret"), Map.of());
    assertEquals("test-secret", resolved.getProperty("redis.password"));
}
```

- [ ] **Step 2: Run the focused test to verify it fails before the mapping is added**

Run: `mvn -pl database -Dtest=RuntimeConfigTest#mapsRedisFieldsFromDotEnv test`

Expected: FAIL until the Redis mappings are present.

- [ ] **Step 3: Replace all independent loaders and fallbacks**

```java
Properties properties = RuntimeConfig.loadRuntimeProperties("database.properties");
RuntimeConfig.requireRuntimeCredentials(properties);
config.setJdbcUrl(properties.getProperty("mysql.url"));
config.setUsername(properties.getProperty("mysql.username"));
config.setPassword(properties.getProperty("mysql.password"));
```

Use this in `DatabaseConnectionManager` and `DBUtil`. In `RedisManager`, use the same resolver before constructing `RedisURI` and delete `localhost`, `6379`, and empty-password defaults. In `StorageManager`, consume resolved OSS properties directly and delete `resolveProperty`, which bypasses `.env`.

- [ ] **Step 4: Run affected module tests**

Run: `mvn -pl database,server -am test`

Expected: `BUILD SUCCESS` without a production connection attempt.

- [ ] **Step 5: Commit consumer migration**

Run: `git add database/src/main/java/com/gomatch/database/config/DatabaseConnectionManager.java server/src/main/java/com/gomatch/server/dao/DBUtil.java server/src/main/java/com/gomatch/server/redis/RedisManager.java server/src/main/java/com/gomatch/server/storage/StorageManager.java; git commit -m "refactor(config): 移除凭据默认回退"`

### Task 3: Add safe artifacts and remove resource credentials

**Files:**

- Modify: `server/src/main/resources/database.properties`
- Modify: `database/src/main/resources/database.properties`
- Modify: `.gitignore:43-45`
- Create: `.env.example`

- [ ] **Step 1: Verify the credential-bearing resource baseline**

Run: `rg -n "^(mysql\\.(username|password)|redis\\.password)=.+" server/src/main/resources/database.properties database/src/main/resources/database.properties`

Expected: current credential lines are reported.

- [ ] **Step 2: Remove resource credentials and add `.env.example`**

```properties
GOMATCH_DB_URL=jdbc:mysql://127.0.0.1:3306/gomatch?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
GOMATCH_DB_USERNAME=your_mysql_username
GOMATCH_DB_PASSWORD=your_mysql_password
GOMATCH_REDIS_HOST=127.0.0.1
GOMATCH_REDIS_PORT=6379
GOMATCH_REDIS_PASSWORD=your_redis_password
GOMATCH_REDIS_DATABASE=0
OSS_ACCESS_KEY_ID=
OSS_ACCESS_KEY_SECRET=
```

Delete `mysql.username`, `mysql.password`, and `redis.password` from both resources. Ignore `.env` and `.env.*`, then negate `.env.example` so it remains tracked.

- [ ] **Step 3: Verify the safe artifacts**

Run: `rg -n "^(mysql\\.(username|password)|redis\\.password)=.+" server/src/main/resources/database.properties database/src/main/resources/database.properties; git check-ignore -v .env; git check-ignore -v .env.example`

Expected: no resource credential matches; `.env` is ignored; `.env.example` is not ignored.

- [ ] **Step 4: Commit safe configuration artifacts**

Run: `git add .gitignore .env.example server/src/main/resources/database.properties database/src/main/resources/database.properties; git commit -m "chore(config): 移除提交的运行时凭据"`

### Task 4: Align scripts and documentation

**Files:**

- Modify: `scripts/ssc_server.ps1`
- Modify: `scripts/zpw_server.ps1`
- Modify: `scripts/hln-server.ps1`
- Modify: `scripts/jzx-server.ps1`
- Modify: `README.md`
- Modify: `docs/DEPLOY_GUIDE.md`
- Modify: `docs/DEV_LOG.md`

- [ ] **Step 1: Record the hard-coded documentation baseline**

Run: `rg -n "(GOMATCH_DB_PASSWORD=|GOMATCH_REDIS_PASSWORD=|MYSQL_ROOT_PASSWORD:|MYSQL_PASSWORD:)" README.md docs/DEPLOY_GUIDE.md`

Expected: deployment examples are reported.

- [ ] **Step 2: Add a `.env` preflight and revise docs**

```powershell
Set-Location (Join-Path $PSScriptRoot "..")
if (-not (Test-Path ".env")) {
    Write-Host "[ERROR] Missing .env. Copy .env.example to .env and set runtime credentials." -ForegroundColor Red
    exit 1
}
```

Place the preflight before Maven execution in all four server scripts. In `README.md` and `docs/DEPLOY_GUIDE.md`, replace credential assignments and Compose passwords with `.env` instructions, `${VARIABLE}` references, or neutral placeholders. Append a dated log entry to `docs/DEV_LOG.md`.

- [ ] **Step 3: Verify docs and scripts**

Run: `rg -n "(GOMATCH_DB_PASSWORD=|GOMATCH_REDIS_PASSWORD=|MYSQL_ROOT_PASSWORD:|MYSQL_PASSWORD:)" README.md docs/DEPLOY_GUIDE.md; rg -n "GOMATCH_DB_(URL|USERNAME|PASSWORD)|GOMATCH_REDIS_(HOST|PORT|PASSWORD)" .env.example README.md docs/DEPLOY_GUIDE.md scripts`

Expected: no live credential assignment appears in Markdown; the second command reports the variables and preflight guidance.

- [ ] **Step 4: Commit operational guidance**

Run: `git add scripts/ssc_server.ps1 scripts/zpw_server.ps1 scripts/hln-server.ps1 scripts/jzx-server.ps1 README.md docs/DEPLOY_GUIDE.md docs/DEV_LOG.md; git commit -m "docs(config): 补充本地凭据配置流程"`

### Task 5: Verify the completed change

- [ ] **Step 1: Run the full relevant suite**

Run: `mvn -pl database,server -am test`

Expected: `BUILD SUCCESS`.

- [ ] **Step 2: Run safety checks**

Run: `git diff --check; rg -n -i --glob '!*target*' --glob '!sql/**' --glob '!docs/DEV_LOG.md' "(mysql\\.password=gomatch|mysql\\.username=gomatch|GOMATCH_DB_PASSWORD=your_secure_password|GOMATCH_REDIS_PASSWORD=your_redis_password)" .; git status --short`

Expected: no diff errors or credential scan matches; only intended changes remain, or the tree is clean after commits.

- [ ] **Step 3: Record actual commands and results before reporting completion**

Separate any existing Maven failure from the outcome of this change.
