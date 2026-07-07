# Windows 终端中文乱码修复指南

## 问题现象

服务端日志中的中文字符在 Windows 终端（PowerShell / CMD）中显示为乱码，例如：

```
浜烘満瀵规垬鎴块棿鍒涘缓鎴愬姛: b478d49f 鐢辩敤鎴?19 鍒涘缓, 闅惧害=MEDIUM
```

## 原因分析

| 组件 | 编码 | 说明 |
|------|------|------|
| Java 源代码 | UTF-8 | `.java` 文件保存为 UTF-8 |
| logback.xml | UTF-8 | `<charset>UTF-8</charset>` 已配置 |
| JVM 默认编码 | GBK | Windows 中文系统下 `file.encoding` 默认值为 GBK |
| Windows 终端 | GBK | 代码页 936（简体中文 GBK） |

logback 将 UTF-8 字符串按 UTF-8 编码为字节流写入 stdout，但 Windows 终端以 GBK 解码这些字节，导致乱码。

## 修复方案

在 `scripts/run-server.ps1` 中增加两项配置：

### 1. 设置终端代码页为 UTF-8

```powershell
chcp 65001 | Out-Null
```

- `chcp 65001` 将当前终端窗口的代码页切换为 UTF-8
- `| Out-Null` 抑制 chcp 命令的输出信息

### 2. 设置 JVM 默认编码为 UTF-8

```powershell
"-Dfile.encoding=UTF-8"
```

- 添加到 Maven exec 的 `-Dexec.args` 或作为独立 JVM 参数
- 确保 JVM 内部字符串与外部编码一致

## 修改后的启动脚本示例

```powershell
$env:JAVA_HOME = "D:\develop\Java\jdk21"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
# Set console code page to UTF-8 to avoid garbled Chinese output
chcp 65001 | Out-Null
mvn -f server/pom.xml exec:java "-Dexec.mainClass=com.gomatch.server.ServerMain" "-Dfile.encoding=UTF-8"
```

## 注意事项

1. **chcp 65001 仅影响当前终端窗口**，新开窗口需要重新执行
2. **UTF-8 代码页（65001）** 在某些旧版 Windows 上可能存在兼容问题，Win10 1809+ 已完全支持
3. 若仍然出现乱码，请确认 logback.xml 中 ConsoleAppender 的 `<charset>UTF-8</charset>` 配置存在且生效
4. 文件日志（`logs/gomatch-server.log`）不受终端编码影响，始终以 UTF-8 正确写入
