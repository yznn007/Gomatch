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
