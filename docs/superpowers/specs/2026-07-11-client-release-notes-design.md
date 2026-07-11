# 客户端发布说明设计

## 目标

为客户端 MSI 发布建立可维护的中文版本说明来源，使 GitHub Release 能按 Git 标签读取对应 Markdown 文件。

## 文件约定

- 发布说明目录：`docs/releases/`。
- 版本文件：`docs/releases/vX.Y.Z.md`，文件名必须与推送的 Git 标签完全一致。
- 目录说明：`docs/releases/README.md`，包含编写规则和可复制模板。

## 发布行为

客户端发布工作流从 `GITHUB_REF_NAME` 获取标签名，并读取同名 Markdown 文件作为 Release 正文。若文件不存在，工作流自动生成 GitHub 变更摘要，确保发布不会因遗漏说明文件而失败。

版本说明采用 Markdown，可包含本次更新、安装与升级、已知问题三个部分。MSI 由工作流作为 Release 资产上传，不需要在版本说明中硬编码下载 URL。

## 边界与验证

- 本次只建立发布说明约定和目录，不部署服务端。
- 已有 `docs/` 文档入口必须链接到 `docs/releases/README.md`。
- 后续客户端发布工作流必须只读取标签同名的文件，且测试缺失文件时的自动摘要回退路径。
