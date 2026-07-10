# Task 1 Report: 国际手机号规范化

## 范围与实现

- 新增 `PhoneNumberNormalizer.normalize(String)`，供后续登录与注册流程共享使用。
- 输入会先去除首尾空白及空格、圆括号、连字符；随后按 E.164 的 8--15 位、首位非零规则验证。
- 合法号码统一返回带 `+` 的值；`null` 与非法输入返回 `null`。

## TDD 证据

### RED

1. 新建 `PhoneNumberNormalizerTest`，包含两个行为测试和四个断言：
   - 带展示分隔符的中国号码以及未带 `+` 的美国号码会被规范化；
   - 国家码以零开头及长度不足的号码会被拒绝。
2. 运行 `mvn -pl server -Dtest=PhoneNumberNormalizerTest test`，结果为失败。输出中包含四处 `PhoneNumberNormalizer` 符号不存在，符合新增工具尚未实现的 RED 预期。

### GREEN

1. 已实现任务说明指定的最小正则与规范化逻辑。
2. 再次运行同一 Maven 命令及一次 `clean` 变体，均未能进入 Surefire：`testCompile` 同时无法解析已有的 `EnvironmentConfig`、`OssService`、`SecurityLimits` 等主类，并包含本任务类。清理后 Maven 主编译实际生成这些 `.class` 文件，测试编译仍失败；这是当前共享 `server/target` 输出或 Maven 测试编译基线问题，非本任务测试断言失败。
3. 因此没有可诚实报告的 Maven GREEN / 服务端全套 GREEN 结果。未以未通过的 JShell 探测替代测试结果。

## 已修改文件

- `server/src/main/java/com/gomatch/server/security/PhoneNumberNormalizer.java`
- `server/src/test/java/com/gomatch/server/security/PhoneNumberNormalizerTest.java`

## 测试输出摘要

- 目标测试命令：失败于 `testCompile`，没有执行测试；错误共 39 处，覆盖既有测试和新测试，均为主类符号无法解析。
- 服务端套件：未运行；同一 `testCompile` 阻断使其不具备有效性。

## 自检

- 公共 API、正则、空值处理、分隔符清理和 `+` 补齐均逐项对照任务说明。
- 测试通过公开静态 API 断言可观察行为，未测试实现细节。
- 未修改任何无关源文件；报告不随功能提交暂存。
