# 移除未接入的用户设置与音乐数据库设计

## 目标

移除未接入运行链路的 `t_user_settings` 与 `t_user_music` 数据库设计及其 `database` 模块代码，使用户配置唯一由 `t_user_profile.config_json` 保存。

## 保留的架构

客户端 `UserSettings` 仍是运行时设置模型，并继续保存到 `~/.gomatch/config.json`。登录后的 Socket 配置同步仍由 `ConfigManager`、`ConfigDAO` 和 `t_user_profile.config_json` 完成。

阿里云 OSS 的头像上传、对象访问与预签名 URL 功能保持不变；它们不依赖 `t_user_music`。`t_audit_log` 也不在本次范围内。

## 删除范围

- `sql/init.sql` 中 `t_user_settings` 与 `t_user_music` 的建表定义。
- `database` 模块中两张表对应的实体、Repository 接口和 Repository 实现。
- `docs/DB_DESIGN.md` 中与两张表有关的表说明、ER 图、流程图、索引说明和 SQL 副本。
- `docs/DEV_LOG.md` 中追加本次收敛记录；保留此前的历史日志。

## 数据与兼容性

当前开发数据库中不存在 `t_user_settings` 与 `t_user_music`，因此不执行 `DROP TABLE`，也不需要数据迁移。更新后的 `sql/init.sql` 面向新环境，不再创建这两张表。

## 验证标准

1. 源代码、初始化 SQL 与 `docs/DB_DESIGN.md` 不再包含两张表名及对应 Repository/实体名称；设计文档与开发日志中的历史记录除外。
2. `database`、`server`、`client` Maven 模块能够编译。
3. 现有 `t_user_profile` 配置同步相关代码与头像 OSS 代码仍保留。
