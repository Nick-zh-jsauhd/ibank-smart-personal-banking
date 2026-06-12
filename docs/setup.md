# 本地运行指南

本文档说明如何从干净环境启动 iBank。

## 环境要求

- JDK 8 或更高版本
- Maven 3.8+
- MySQL 8+
- Tomcat 9
- Windows PowerShell 或兼容终端

## 初始化数据库

```powershell
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS ibank DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -uroot -p ibank < src\main\resources\schema.sql
```

如果已有旧库，建议先备份再执行 schema。

## 配置数据库

```powershell
Copy-Item src\main\resources\db.properties.example src\main\resources\db.properties
```

修改：

```properties
db.url=jdbc:mysql://localhost:3306/ibank?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
db.username=root
db.password=your_password
```

也可以使用环境变量覆盖：

- `IBANK_DRIVER`
- `IBANK_URL`
- `IBANK_USERNAME`
- `IBANK_PASSWORD`

## 构建与部署

```powershell
mvn -q -DskipTests package
Copy-Item target\bank.war D:\tomcat9\webapps\bank.war -Force
D:\tomcat9\bin\startup.bat
```

如果 Tomcat 已经运行：

```powershell
D:\tomcat9\bin\shutdown.bat
D:\tomcat9\bin\startup.bat
```

## AI 助手配置

默认建议使用本地私有模型服务：

```powershell
Copy-Item src\main\resources\ai.properties.example src\main\resources\ai.properties
```

本地服务示例：

```properties
ai.provider=local
ai.local.baseUrl=http://127.0.0.1:18080/v1
ai.local.model=qwen3:8b
```

如需 SSH 隧道：

```powershell
Copy-Item scripts\open-llm-tunnel.example.ps1 scripts\open-llm-tunnel.ps1
.\scripts\open-llm-tunnel.ps1 -RemoteHost your-server.example.com -SshPort 22
```

## 验证

```powershell
mvn -q -DskipTests package
scripts\regression.ps1 -SkipBuild -SkipDb
```

浏览器检查：

- `http://localhost:8080/bank/`
- `http://localhost:8080/bank/login`
- `http://localhost:8080/bank/admin/login`

