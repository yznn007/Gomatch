### Task 2: 修改服务端认证与数据库约束

**Files:**
- Modify: `server/src/main/java/com/gomatch/server/security/ProtocolRequestGuard.java`
- Modify: `server/src/main/java/com/gomatch/server/handler/ClientHandler.java:205-384`
- Modify: `server/src/main/java/com/gomatch/server/dao/UserDAO.java:66-160,239-260`
- Modify: `server/src/test/java/com/gomatch/server/security/RequestPolicyTest.java`
- Create: `server/src/test/java/com/gomatch/server/dao/UserDaoSqlTest.java`
- Modify: `sql/init.sql:9-32`
- Create: `sql/migrations/2026-07-11-add-unique-phone.sql`

- [ ] **Step 1: 写失败测试**

```java
String login = "{\"cmd\":\"LOGIN\",\"data\":{\"account\":\"+8613800138000\",\"password\":\"secret\"}}";
assertDoesNotThrow(() -> guard.validateAndNormalize(login, address));
assertTrue(UserDAO.LOGIN_BY_ACCOUNT_SQL.contains("user_id = ?"));
assertTrue(UserDAO.LOGIN_BY_ACCOUNT_SQL.contains("phone = ?"));
```

- [ ] **Step 2: 确认测试失败**

Run: `mvn -pl server -Dtest=RequestPolicyTest,UserDaoSqlTest test`

Expected: 失败，因为守卫仍读取 `username`，且 DAO 尚无多标识 SQL。

- [ ] **Step 3: 实现协议、处理器和 DAO**

`ProtocolRequestGuard` 读取 `LOGIN.data.account`，允许 1–100 字符，并以 `IP|account` 限流；`REGISTER` 要求 8–32 字符的 `phone`。

`ClientHandler` 使用 `account` 登录；注册时调用 `PhoneNumberNormalizer.normalize`，为空则返回 `手机号必须是有效的国际号码（E.164，含国家区号）`。失败时依次检查用户名、邮箱、手机号，给出对应重复提示。

`UserDAO` 增加包可见的 SQL：

```java
static final String LOGIN_BY_ACCOUNT_SQL = "SELECT user_id, username, score, win_count, lose_count, draw_count, " +
        "role, status, phone FROM t_user WHERE (user_id = ? OR username = ? OR email = ? OR phone = ?) AND password = ?";
```

仅将 `[1-9]\\d{0,18}` 解析为 ID；邮箱转小写，手机号以 `normalize(account)` 作为查询值；新增 `phoneExists`，注册时拒绝空或重复号码。

`init.sql` 将列定义改为：

```sql
`phone` VARCHAR(16) NOT NULL UNIQUE COMMENT 'E.164国际手机号',
```

迁移脚本先查询空值和重复项；确认修复后执行 `MODIFY COLUMN phone VARCHAR(16) NOT NULL` 与 `ADD CONSTRAINT uk_user_phone UNIQUE (phone)`。

- [ ] **Step 4: 确认测试通过并提交**

Run: `mvn -pl server -Dtest=RequestPolicyTest,UserDaoSqlTest test`

Expected: 所有认证防护和 SQL 结构测试通过。

```powershell
git add server/src/main/java/com/gomatch/server/security/ProtocolRequestGuard.java server/src/main/java/com/gomatch/server/handler/ClientHandler.java server/src/main/java/com/gomatch/server/dao/UserDAO.java server/src/test/java/com/gomatch/server/security/RequestPolicyTest.java server/src/test/java/com/gomatch/server/dao/UserDaoSqlTest.java sql/init.sql sql/migrations/2026-07-11-add-unique-phone.sql
git commit -m "feat(auth): 支持多标识登录与手机号唯一性"
```

