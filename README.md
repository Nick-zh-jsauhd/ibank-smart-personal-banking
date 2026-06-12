# iBank 智能个人银行系统

iBank 是一个基于 Java Web、JSP/Servlet、MySQL 的智能个人银行演示系统。项目覆盖客户账户、交易流水、月账单、理财申购、风控预警、消息待办、客户工单、后台运营、权限管理，以及离线图算法风控实验链路。

> 说明：本项目用于课程设计、系统原型和算法集成演示，不适合作为真实金融生产系统直接使用。

## 功能概览

- 客户侧：注册登录、账户资产、存款取款、转账缴费、流水查询、日/月/年账单报表、打印和导出。
- 财富侧：理财产品浏览、风险测评、适配校验、申购赎回、持仓收益模拟。
- 风控侧：交易限额、异常交易预警、风控事件、图谱看板、离线模型评分接入。
- 服务侧：通知中心、待办闭环、客户服务工单、交易争议处理。
- 管理端：运营看板、客户管理、产品上下架、风控规则管理、调账审核、权限角色管理、审计日志。
- AI 能力：本地 LLM 助手配置模板、风险图谱与 GNN 实验模块。

## 技术栈

- Java 8+
- Servlet / JSP / JSTL
- MySQL 8+
- Maven
- Tomcat 9
- Python 风控实验模块：pandas、scikit-learn、PyTorch、PyG 等按需安装

## 目录结构

```text
.
├── src/main/java/com/bank/      # Java 后端代码
├── src/main/resources/          # SQL、配置模板
├── src/main/webapp/             # JSP、CSS、静态资源
├── scripts/                     # 本地运行、回归、LLM 示例脚本
├── ml/riskbrain_gnn/            # 风控图算法实验模块
├── docs/                        # 架构、部署、公开发布说明
├── pom.xml
└── README.md
```

## 本地启动

1. 准备 MySQL 数据库：

```powershell
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS ibank DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -uroot -p ibank < src\main\resources\schema.sql
```

2. 创建本地配置：

```powershell
Copy-Item src\main\resources\db.properties.example src\main\resources\db.properties
Copy-Item src\main\resources\ai.properties.example src\main\resources\ai.properties
```

然后修改 `src/main/resources/db.properties` 中的数据库用户名和密码。真实配置文件已被 `.gitignore` 忽略。

3. 构建 WAR：

```powershell
mvn -q -DskipTests package
```

4. 部署到 Tomcat 9：

```powershell
Copy-Item target\bank.war D:\tomcat9\webapps\bank.war -Force
D:\tomcat9\bin\startup.bat
```

访问：

- 首页：`http://localhost:8080/bank/`
- 客户登录：`http://localhost:8080/bank/login`
- 管理端登录：`http://localhost:8080/bank/admin/login`

## 默认演示账号

管理员引导逻辑会在系统启动时确保基础管理员存在：

- 超级管理员：`admin / admin123`

客户账号可以通过注册页面创建；如果使用已有演示库，可在数据库中查看 `customers` 表对应用户。

## AI 与隐私

默认建议使用本地 OpenAI-compatible LLM 服务，例如私有 GPU 服务器上的 llama.cpp、vLLM 或 Ollama 网关。`ai.properties.example` 只提供模板，不包含密钥。

如需远程 LLM 隧道，可复制：

```powershell
Copy-Item scripts\open-llm-tunnel.example.ps1 scripts\open-llm-tunnel.ps1
```

再填入自己的服务器地址。真实隧道脚本被 `.gitignore` 忽略，避免泄露主机、端口和账号。

## 风控图算法

`ml/riskbrain_gnn/` 保存了 PaySim / IBM AML 等数据集实验、图特征构建、基线模型、GNN 训练和离线评分相关脚本。大型数据集、模型权重和训练输出不进入 Git 仓库。

详细说明见 [docs/riskbrain-gnn.md](docs/riskbrain-gnn.md)。

## 回归验证

```powershell
mvn -q -DskipTests package
scripts\regression.ps1 -SkipBuild -SkipDb
```

## 公开仓库注意事项

- 不提交 `src/main/resources/db.properties`、`src/main/resources/ai.properties`。
- 不提交 `target/`、数据集、模型权重、远程服务器脚本、Chrome 临时目录。
- 不提交第三方论文代码目录 `external/`，除非单独核查许可证并保留原始声明。
- 发布前执行一次敏感信息扫描。

更多发布检查见 [docs/public-release.md](docs/public-release.md)。

