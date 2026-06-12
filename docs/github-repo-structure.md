# GitHub 仓库组织建议

推荐仓库名：

```text
ibank-smart-personal-banking
```

## 推荐结构

```text
ibank-smart-personal-banking/
├── README.md
├── LICENSE
├── .gitignore
├── pom.xml
├── docs/
│   ├── setup.md
│   ├── architecture.md
│   ├── database.md
│   ├── riskbrain-gnn.md
│   └── public-release.md
├── src/
│   └── main/
│       ├── java/com/bank/
│       ├── resources/
│       └── webapp/
├── scripts/
└── ml/
    └── riskbrain_gnn/
```

## 不建议提交

- `external/`：第三方论文代码需要单独核查许可证。
- `target/`：Maven 构建产物和临时浏览器目录。
- 真实配置：数据库密码、AI API Key、服务器地址。
- 大型数据：PaySim、IBM AML、模型权重、训练输出。

