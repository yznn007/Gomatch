# Gomatch Non-TLS Security Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix confirmed authorization, resource-exhaustion, room-access, JSON, local-credential, and configuration weaknesses while deliberately retaining plaintext user-password storage and plaintext TCP.

**Architecture:** Add small pure security utilities at the socket boundary, then integrate them into the existing `ServerMain`/`ClientHandler` flow. Make session revocation authoritative across memory, Redis, and JDBC; keep UI changes limited to password-protected spectating and username-only persistence.

**Tech Stack:** JDK 21, Java sockets, JavaFX 21, org.json, JDBC/HikariCP, Redis/Lettuce, JUnit 5, Maven.

---

## File map

- Create `server/src/main/java/com/gomatch/server/security/BoundedLineReader.java`: bounded newline framing.
- Create `server/src/main/java/com/gomatch/server/security/FixedWindowRateLimiter.java`: expiring per-key counters.
- Create `server/src/main/java/com/gomatch/server/security/SecurityLimits.java`: protocol size and pagination policy.
- Create `server/src/main/java/com/gomatch/server/security/ConnectionGuard.java`: total and per-IP connection accounting.
- Modify `server/.../ServerMain.java`, `ClientHandler.java`: enforce boundary policy.
- Modify `server/.../SessionManager.java`, `SocketSessionDAO.java`, `UserDAO.java`: authoritative session revocation and fresh auth state.
- Create `server/src/main/java/com/gomatch/server/handler/HistoryPayloads.java`: safe history JSON serialization.
- Modify `client/.../CasualLobbyController.java`: password-aware spectating.
- Create `client/src/main/java/com/gomatch/client/network/InboundMessageParser.java`: isolate malformed input.
- Create `client/src/main/java/com/gomatch/client/security/CredentialStore.java`: username-only persistence and legacy cleanup.
- Modify `client/.../ServerConnector.java`, `LoginController.java`, message bundles/FXML text.
- Modify `server/.../DBUtil.java`, `RedisManager.java`, `database/.../DatabaseConnectionManager.java`: environment-first configuration.
- Add focused JUnit tests beside each new unit.
- Update `docs/requirements.md` and append `docs/DEV_LOG.md`.

### Task 1: Pure boundary security utilities

**Files:**
- Create: `server/src/main/java/com/gomatch/server/security/BoundedLineReader.java`
- Create: `server/src/main/java/com/gomatch/server/security/FixedWindowRateLimiter.java`
- Create: `server/src/main/java/com/gomatch/server/security/SecurityLimits.java`
- Create: `server/src/main/java/com/gomatch/server/security/ConnectionGuard.java`
- Test: `server/src/test/java/com/gomatch/server/security/SecurityUtilitiesTest.java`

- [ ] **Step 1: Write failing utility tests**

```java
class SecurityUtilitiesTest {
    @Test void rejectsLinePastLimit() throws Exception {
        var reader = new BoundedLineReader(new StringReader("123456\n"), 5);
        assertThrows(BoundedLineReader.LineTooLongException.class, reader::readLine);
    }

    @Test void limiterResetsAfterWindow() {
        AtomicLong now = new AtomicLong(1_000);
        var limiter = new FixedWindowRateLimiter<String>(2, 1_000, now::get);
        assertTrue(limiter.tryAcquire("ip"));
        assertTrue(limiter.tryAcquire("ip"));
        assertFalse(limiter.tryAcquire("ip"));
        now.set(2_001);
        assertTrue(limiter.tryAcquire("ip"));
    }

    @Test void connectionGuardAppliesPerIpAndTotalLimits() throws Exception {
        var guard = new ConnectionGuard(2, 1);
        InetAddress one = InetAddress.getByName("127.0.0.1");
        InetAddress two = InetAddress.getByName("127.0.0.2");
        assertTrue(guard.tryAcquire(one));
        assertFalse(guard.tryAcquire(one));
        assertTrue(guard.tryAcquire(two));
        assertFalse(guard.tryAcquire(InetAddress.getByName("127.0.0.3")));
        guard.release(one);
        assertTrue(guard.tryAcquire(one));
    }

    @Test void clampsPaginationAndTextLengths() {
        assertEquals(0, SecurityLimits.page(-1));
        assertEquals(100, SecurityLimits.pageSize(Integer.MAX_VALUE));
        assertTrue(SecurityLimits.validLength("hello", 1, 5));
        assertFalse(SecurityLimits.validLength("hello!", 1, 5));
    }
}
```

- [ ] **Step 2: Run the tests and verify red**

Run: `mvn -pl server -am -Dtest=SecurityUtilitiesTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: compilation fails because the four security classes do not exist.

- [ ] **Step 3: Implement the utilities**

`BoundedLineReader.readLine()` must read one character at a time, return `null` only when EOF occurs before any character, strip `\r` before `\n`, and throw `LineTooLongException` immediately after `maxChars`.

```java
public final class SecurityLimits {
    public static final int MAX_MESSAGE_CHARS = 3_000_000;
    public static final int MAX_CHAT_CHARS = 500;
    public static final int MAX_ROOM_NAME_CHARS = 100;
    public static final int MAX_ROOM_PASSWORD_CHARS = 100;
    public static final int MAX_PASSWORD_CHARS = 100;
    public static final int MAX_PAGE_SIZE = 100;
    public static int page(int value) { return Math.max(0, value); }
    public static int pageSize(int value) { return Math.max(1, Math.min(MAX_PAGE_SIZE, value)); }
    public static boolean validLength(String value, int min, int max) {
        return value != null && value.length() >= min && value.length() <= max;
    }
    private SecurityLimits() {}
}
```

`FixedWindowRateLimiter<K>` stores `{windowStart,count}` in a `ConcurrentHashMap`, accepts a `LongSupplier` clock for tests, and removes expired entries every 256 acquisitions. `ConnectionGuard` uses one `AtomicInteger` plus `ConcurrentHashMap<InetAddress,AtomicInteger>` and never lets counters fall below zero.

- [ ] **Step 4: Run the focused tests**

Run: `mvn -pl server -am -Dtest=SecurityUtilitiesTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: `SecurityUtilitiesTest` passes with zero failures.

- [ ] **Step 5: Commit**

```text
fix(security): add bounded request guards

Cap protocol frames and provide reusable rate and connection limits to
prevent unauthenticated resource exhaustion.
```

### Task 2: Enforce socket, request, and input limits

**Files:**
- Modify: `server/src/main/java/com/gomatch/server/ServerMain.java`
- Modify: `server/src/main/java/com/gomatch/server/handler/ClientHandler.java`
- Test: `server/src/test/java/com/gomatch/server/security/RequestPolicyTest.java`

- [ ] **Step 1: Add failing request-policy tests**

Test these exact policies through package-visible helpers: unauthenticated clients get at most 60 commands/minute per IP, login gets 10 attempts/minute per IP+normalized username, registration gets 5 attempts/hour per IP, pagination clamps to `page >= 0` and `1 <= size <= 100`, chat caps at 500, room name/password cap at 100, account password caps at the existing SQL width of 100, and encoded avatar input is rejected before Base64 decoding when over 2,796,400 characters.

- [ ] **Step 2: Verify policy tests fail**

Run: `mvn -pl server -am -Dtest=RequestPolicyTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: failures show that `ClientHandler` has no request-policy helpers.

- [ ] **Step 3: Integrate connection and frame limits**

Replace `BufferedReader.readLine()` with `BoundedLineReader(MAX_MESSAGE_CHARS)`. On `LineTooLongException`, send one generic `ERROR`, log remote address and observed limit only, then exit the read loop. Replace `ServerMain.connectionCount` checks with `ConnectionGuard(100, 10)`; pass the remote address to release during `ClientHandler.cleanup()`.

- [ ] **Step 4: Integrate rate and field limits**

Add static limiters to `ClientHandler`, call them before DAO work, normalize usernames with `trim().toLowerCase(Locale.ROOT)`, and return generic failure messages. Route every page/size through `SecurityLimits`. Reject oversize chat, friend-request text, room fields, password fields, save name, keyword, difficulty and avatar Base64 before broadcasting, persistence, logging, enum parsing or decoding.

- [ ] **Step 5: Run boundary tests**

Run: `mvn -pl server -am -Dtest=SecurityUtilitiesTest,RequestPolicyTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: both classes pass.

- [ ] **Step 6: Commit**

```text
fix(server): enforce request limits

Bound connections, frames, authentication attempts, pagination, and free
text before requests reach database, Redis, logs, or OSS.
```

### Task 3: Make session revocation authoritative

**Files:**
- Modify: `server/src/main/java/com/gomatch/server/session/SessionManager.java`
- Modify: `server/src/main/java/com/gomatch/server/dao/SocketSessionDAO.java`
- Modify: `server/src/main/java/com/gomatch/server/dao/UserDAO.java`
- Modify: `server/src/main/java/com/gomatch/server/handler/ClientHandler.java`
- Modify: `client/src/main/java/com/gomatch/client/ui/ProfileController.java`
- Test: `server/src/test/java/com/gomatch/server/session/SessionPolicyTest.java`

- [ ] **Step 1: Write failing pure session-policy tests**

Extract package-visible decisions that can be tested without MySQL/Redis. Cover: expired session is rejected before activity refresh; active handler blocks a second handler for the same token; `BANNED` auth state rejects reconnect; database role replaces cached role; creating a session removes every older token for the user.

- [ ] **Step 2: Verify the tests fail**

Run: `mvn -pl server -am -Dtest=SessionPolicyTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: failures demonstrate pre-refresh expiry, stale role/status trust, and retained old tokens.

- [ ] **Step 3: Implement full-user revocation**

Add `SocketSessionDAO.disconnectByUserId(long,String)`. Add `SessionManager.revokeAllSessionsByUserId(long,String)` that iterates `sessionsByToken`, removes matching tokens with compare-safe `sessionsByUserId.remove(userId, session)`, deletes every Redis session, marks JDBC sessions disconnected, and returns distinct live handlers so the caller can notify/close them. `createSession()` calls this method before storing a new token.

- [ ] **Step 4: Revalidate reconnect authorization**

Make `getSessionByToken()` return an expired session without refreshing it. In `handleReconnect`, check expiry and concurrent handler first, then call a new `UserDAO.getAuthState(userId)` query returning `userId, username, role, status`; reject non-`ACTIVE`, apply the fresh role, then update activity/handler.

- [ ] **Step 5: Revoke on security changes**

After ban or role change, notify and close returned target handlers. After successful password change, revoke all old tokens, create one replacement session bound to the current handler, and return the new token in `PWD_CHANGED`; `ProfileController` stores that token in `AppConfig`.

- [ ] **Step 6: Run session tests**

Run: `mvn -pl server -am -Dtest=SessionPolicyTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: all session-policy tests pass.

- [ ] **Step 7: Commit**

```text
fix(auth): revoke stale sessions

Revalidate account status and role on reconnect, rotate tokens after password
changes, and invalidate every token after bans or role changes.
```

### Task 4: Protect spectating and serialize history safely

**Files:**
- Modify: `server/src/main/java/com/gomatch/server/handler/ClientHandler.java`
- Create: `server/src/main/java/com/gomatch/server/handler/HistoryPayloads.java`
- Modify: `client/src/main/java/com/gomatch/client/ui/CasualLobbyController.java`
- Test: `server/src/test/java/com/gomatch/server/handler/HistoryPayloadsTest.java`
- Test: `server/src/test/java/com/gomatch/server/room/RoomAccessTest.java`

- [ ] **Step 1: Add failing authorization and serialization tests**

`RoomAccessTest` verifies `room.hasPassword()` requires the correct password for spectating policy and leaves unprotected rooms accessible. `HistoryPayloadsTest` uses names/content such as `a\"}],\"cmd\":\"BANNED` and asserts `Message.fromJson(payload)` succeeds while preserving the exact original strings.

- [ ] **Step 2: Verify red**

Run: `mvn -pl server -am -Dtest=RoomAccessTest,HistoryPayloadsTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: history test fails on manual concatenation and spectating policy is absent.

- [ ] **Step 3: Enforce password-aware spectating**

Read optional `password` in `handleSpectate`; before leaving the current room, reject when `room.hasPassword() && !room.checkPassword(password)`. In `CasualLobbyController`, reuse the existing password dialog for protected-room spectate buttons and send `{roomId,password}`.

- [ ] **Step 4: Replace manual JSON building**

Implement `HistoryPayloads.saveList`, `chatHistory`, and `watcherHistory` with `JSONArray`/`JSONObject`, and send them through `Message.builder(...).set(...).build()`. Remove all three `StringBuilder` JSON blocks from `ClientHandler`.

- [ ] **Step 5: Run focused tests**

Run: `mvn -pl server -am -Dtest=RoomAccessTest,HistoryPayloadsTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: both test classes pass.

- [ ] **Step 6: Commit**

```text
fix(protocol): enforce room and JSON safety

Require protected-room credentials for spectators and serialize persisted
user content without allowing JSON structure injection.
```

### Task 5: Harden client parsing and remove saved passwords

**Files:**
- Modify: `client/pom.xml`
- Create: `client/src/main/java/com/gomatch/client/network/InboundMessageParser.java`
- Create: `client/src/main/java/com/gomatch/client/security/CredentialStore.java`
- Modify: `client/src/main/java/com/gomatch/client/network/ServerConnector.java`
- Modify: `client/src/main/java/com/gomatch/client/ui/LoginController.java`
- Modify: `client/src/main/resources/messages_zh.properties`
- Modify: `client/src/main/resources/messages_en.properties`
- Test: `client/src/test/java/com/gomatch/client/network/InboundMessageParserTest.java`
- Test: `client/src/test/java/com/gomatch/client/security/CredentialStoreTest.java`

- [ ] **Step 1: Add JUnit 5 test support and failing tests**

Add `junit-jupiter:5.10.1` test dependency and Surefire `3.1.2`. Test `InboundMessageParser.parse("not-json")` returns empty and a following valid message parses. With `@TempDir`, seed legacy `credentials.json` containing a password; assert `CredentialStore.load()` returns only username and rewrites/removes the password field, `saveUsername()` never writes `password`, and `clear()` removes the file.

- [ ] **Step 2: Verify red**

Run: `mvn -pl client -am -Dtest=InboundMessageParserTest,CredentialStoreTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: compilation fails because parser/store classes are missing.

- [ ] **Step 3: Implement parser isolation**

`InboundMessageParser.parse(String)` returns `Optional<Message>` and catches `RuntimeException`. `ServerConnector` logs and continues on empty results; only `IOException` terminates the receive loop. Never log the complete rejected line.

- [ ] **Step 4: Implement username-only persistence**

Move filesystem work from `LoginController` into `CredentialStore`. On initialize, load only username while preserving `UserSettings.rememberPassword` as the compatibility flag. On successful login, save username only when selected; otherwise delete the credentials file. Remove `SavedCredential.password`, never prefill `passwordField`, and change visible translations from “记住密码” to “记住用户名”.

- [ ] **Step 5: Run client tests**

Run: `mvn -pl client -am -Dtest=InboundMessageParserTest,CredentialStoreTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: both classes pass.

- [ ] **Step 6: Commit**

```text
fix(client): isolate bad input and secrets

Keep receiving after malformed server messages and stop persisting account
passwords in the user profile directory.
```

### Task 6: Environment-first configuration, documentation, and full verification

**Files:**
- Create: `server/src/main/java/com/gomatch/server/config/EnvironmentConfig.java`
- Modify: `server/src/main/java/com/gomatch/server/dao/DBUtil.java`
- Modify: `server/src/main/java/com/gomatch/server/redis/RedisManager.java`
- Modify: `database/src/main/java/com/gomatch/database/config/DatabaseConnectionManager.java`
- Test: `server/src/test/java/com/gomatch/server/config/EnvironmentConfigTest.java`
- Modify: `docs/requirements.md`
- Modify: `docs/DEV_LOG.md`

- [ ] **Step 1: Write failing configuration tests**

Test a pure resolver taking `Properties` plus `Map<String,String>`: environment values override resource values for `GOMATCH_DB_URL`, `GOMATCH_DB_USERNAME`, `GOMATCH_DB_PASSWORD`, `GOMATCH_REDIS_HOST`, `GOMATCH_REDIS_PORT`, `GOMATCH_REDIS_PASSWORD`, `OSS_ACCESS_KEY_ID`, and `OSS_ACCESS_KEY_SECRET`; missing environment values fall back to properties; default `gomatch` DB password and empty Redis password produce warning flags.

- [ ] **Step 2: Verify red**

Run: `mvn -pl server -am -Dtest=EnvironmentConfigTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: compilation fails because `EnvironmentConfig` is missing.

- [ ] **Step 3: Integrate environment resolution**

Use `EnvironmentConfig` from `DBUtil`, `RedisManager`, and the reference database module. Keep the current localhost values as fallback. Log warnings naming the insecure setting but never log the resolved secret value.

- [ ] **Step 4: Update project documentation**

Document the non-TLS security boundaries and environment variable names in `docs/requirements.md`; append a dated entry to `docs/DEV_LOG.md`. Do not include environment assignment commands or machine-specific paths.

- [ ] **Step 5: Run full verification**

Run in order:

```powershell
mvn install -pl common -am
mvn -pl server -am test
mvn -pl client -am test
mvn -DskipTests package
```

Expected: all security unit tests pass and all modules package. If `OssServiceTest` requires unavailable external credentials, run it only when credentials exist and report that integration-test limitation separately; do not hide unrelated failures.

- [ ] **Step 6: Review the final diff**

Run: `git diff --check` and `git status --short`.

Expected: no whitespace errors; only planned source, test, and documentation files are changed.

- [ ] **Step 7: Commit**

```text
fix(config): prefer environment secrets

Allow production credentials to override development defaults and warn when
the server starts with weak local-only settings.
```

