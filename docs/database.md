# 数据库说明

数据库初始化脚本位于：

```text
src/main/resources/schema.sql
```

## 初始化

```powershell
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS ibank DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -uroot -p ibank < src\main\resources\schema.sql
```

## 配置

公开仓库只提交模板：

```text
src/main/resources/db.properties.example
```

本地运行时复制为：

```text
src/main/resources/db.properties
```

真实文件被 `.gitignore` 忽略。

## 公开数据策略

- `schema.sql` 可以提交，用于表结构和基础演示数据。
- 本地训练数据、PaySim、IBM AML、导入后的大批量流水不提交。
- 数据导出文件、模型输出和中间特征文件不提交。
- 如需提供演示数据，建议单独准备小规模、脱敏、可重复生成的 seed 脚本。

