# 多标识登录与国际手机号注册 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 注册要求唯一的 E.164 国际手机号，登录支持用户 ID、用户名、邮箱和手机号。

**Architecture:** 服务端用一个无状态手机号规范化工具统一处理注册与登录号码。客户端只做即时校验；服务端和数据库分别负责业务校验与并发唯一性。

**Tech Stack:** Java 21、JavaFX、Socket JSON、JDBC、MySQL、JUnit 5、Maven。

---

### Task 1: 共享国际手机号规范化规则

**Files:**
- Create: `server/src/main/java/com/gomatch/server/security/PhoneNumberNormalizer.java`
- Create: `server/src/test/java/com/gomatch/server/security/PhoneNumberNormalizerTest.java`

- [ ] **Step 1: 写失败测试**

```java
assertEquals("+8613800138000", PhoneNumberNormalizer.normalize("+86 138-0013-8000"));
assertEquals("+14155552671", PhoneNumberNormalizer.normalize("14155552671"));
assertNull(PhoneNumberNormalizer.normalize("+01234567890"));
assertNull(PhoneNumberNormalizer.normalize("1234567"));
```

- [ ] **Step 2: 确认测试失败**

Run: `mvn -pl server -Dtest=PhoneNumberNormalizerTest test`

Expected: 编译失败，`PhoneNumberNormalizer` 不存在。

- [ ] **Step 3: 实现最小工具**

```java
private static final Pattern E164 = Pattern.compile("\\+?[1-9]\\d{7,14}");

public static String normalize(String value) {
    if (value == null) return null;
    String compact = value.trim().replaceAll("[\\s()\\-]", "");
    if (!E164.matcher(compact).matches()) return null;
    return compact.startsWith("+") ? compact : "+" + compact;
}
```

- [ ] **Step 4: 确认测试通过并提交**

Run: `mvn -pl server -Dtest=PhoneNumberNormalizerTest test`

Expected: `Tests run: 2, Failures: 0, Errors: 0`。

```powershell
git add server/src/main/java/com/gomatch/server/security/PhoneNumberNormalizer.java server/src/test/java/com/gomatch/server/security/PhoneNumberNormalizerTest.java
git commit -m "feat(auth): 增加国际手机号规范化"
```

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

### Task 3: 修改 JavaFX 注册和登录界面

**Files:**
- Create: `client/src/main/java/com/gomatch/client/ui/PhoneInputValidator.java`
- Create: `client/src/test/java/com/gomatch/client/ui/PhoneInputValidatorTest.java`
- Modify: `client/src/main/java/com/gomatch/client/ui/LoginController.java:16-68`
- Modify: `client/src/main/java/com/gomatch/client/ui/RegisterController.java:13-62`
- Modify: `client/src/main/resources/fxml/LoginUI.fxml:33`
- Modify: `client/src/main/resources/fxml/RegisterUI.fxml:20-36`

- [ ] **Step 1: 写失败测试**

```java
assertTrue(PhoneInputValidator.isValid("+8613800138000"));
assertTrue(PhoneInputValidator.isValid("14155552671"));
assertFalse(PhoneInputValidator.isValid("1234567"));
```

- [ ] **Step 2: 确认测试失败**

Run: `mvn -pl client -Dtest=PhoneInputValidatorTest test`

Expected: 编译失败，校验器尚不存在。

- [ ] **Step 3: 实现控件与消息字段**

客户端校验器使用与服务端一致的 E.164 基础规则（去除空格、连字符、圆括号后匹配 `^\\+?[1-9]\\d{7,14}$`）。注册 FXML 在邮箱和密码间新增 `phoneField`，提示为“国际手机号（含国家区号）”，并调大认证卡片高度；控制器将号码设为必填、校验后随 `REGISTER` 的 `phone` 发送。

登录 FXML 将 `usernameField` 改为 `accountField`，提示为“手机号 / 邮箱 / ID / 用户名”；控制器的凭据保存、空提示和请求改为：

```java
ClientMain.getConnector().send(Message.of(Protocol.LOGIN,
        "account", account, "password", password));
```

- [ ] **Step 4: 确认客户端测试通过并提交**

Run: `mvn -pl client -Dtest=PhoneInputValidatorTest test`

Expected: `Tests run: 1, Failures: 0, Errors: 0`。

```powershell
git add client/src/main/java/com/gomatch/client/ui/PhoneInputValidator.java client/src/test/java/com/gomatch/client/ui/PhoneInputValidatorTest.java client/src/main/java/com/gomatch/client/ui/LoginController.java client/src/main/java/com/gomatch/client/ui/RegisterController.java client/src/main/resources/fxml/LoginUI.fxml client/src/main/resources/fxml/RegisterUI.fxml
git commit -m "feat(auth): 增加国际手机号注册界面"
```

### Task 4: 完整验证与开发日志

**Files:**
- Modify: `docs/DEV_LOG.md`

- [ ] **Step 1: 检查旧登录请求字段**

Run: `rg -n '"username"|usernameField' client/src/main/java/com/gomatch/client/ui/LoginController.java server/src/main/java/com/gomatch/server/handler/ClientHandler.java server/src/main/java/com/gomatch/server/security/ProtocolRequestGuard.java`

Expected: 登录流程不再用 `username` 作为协议字段；注册仍使用用户名。

- [ ] **Step 2: 运行完整验证**

Run: `mvn test -pl common,database,server,client -am`

Expected: Maven 构建成功，全部测试通过。

- [ ] **Step 3: 记录与检查差异**

向 `docs/DEV_LOG.md` 追加本次功能、E.164 规范化和验证结果。

Run: `git diff --check; git diff --stat`

Expected: 无空白错误，改动仅涉及认证、迁移、测试和日志。

- [ ] **Step 4: 提交**

```powershell
git add docs/DEV_LOG.md
git commit -m "docs(auth): 记录多标识认证改造"
```

### Task 5: 国家区号下拉框与本地号码登录

**Files:**
- Modify: `client/src/main/resources/fxml/LoginUI.fxml`
- Modify: `client/src/main/resources/fxml/RegisterUI.fxml`
- Modify: `client/src/main/java/com/gomatch/client/ui/LoginController.java`
- Modify: `client/src/main/java/com/gomatch/client/ui/RegisterController.java`
- Modify: `client/src/main/java/com/gomatch/client/ui/PhoneInputValidator.java`

- [ ] **Step 1: 写入号码组合测试**

```java
assertEquals("+8613800138000", PhoneInputValidator.toE164("+86", "13800138000"));
assertEquals("+14155552671", PhoneInputValidator.toE164("+1", "4155552671"));
assertNull(PhoneInputValidator.toE164("+86", "1234567"));
```

- [ ] **Step 2: 实现下拉框和账号发送规则**

在登录与注册 FXML 的手机号输入前加入 `ComboBox<String>`，选项至少包含“中国 +86”“美国 +1”“日本 +81”“英国 +44”，默认“中国 +86”。用户只填写本地号码；控制器通过 `PhoneInputValidator.toE164(countryCode, localNumber)` 组合 E.164 值。

注册将组合后的号码作为 `phone` 发送。登录保留账号输入框：当其为纯本地手机号并可按所选区号组合时，向 `LOGIN` 同时发送 `account` 和规范化 `phone`；邮箱、用户 ID 与用户名保持原样作为 `account`。

- [ ] **Step 3: 服务端按可选规范手机号匹配**

`ClientHandler.handleLogin` 读取可选 `phone`；`UserDAO.login` 接受该规范号码并在 `phone = ?` 条件中优先使用它，缺失时继续从 `account` 尝试 E.164 规范化。这样手机号不会要求用户手写区号，ID、用户名、邮箱也不受影响。

- [ ] **Step 4: 验证并提交**

Run: `mvn -pl client test; mvn -pl server compile`

Expected: 客户端测试与服务端主编译均成功。