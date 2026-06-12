# GitHub 公开发布检查清单

公开前必须确认仓库只包含可公开的源码、模板和说明文档。

## 必须忽略

- `target/`
- `src/main/resources/db.properties`
- `src/main/resources/ai.properties`
- `scripts/open-llm-tunnel.ps1`
- `scripts/start-server-llm.sh`
- `external/`
- 数据集、模型权重、训练输出、浏览器临时目录

## 发布前检查

```powershell
git status --short
git check-ignore --no-index -v src/main/resources/db.properties
git check-ignore --no-index -v src/main/resources/ai.properties
git check-ignore --no-index -v scripts/open-llm-tunnel.ps1
git check-ignore --no-index -v target/bank.war
```

敏感信息扫描建议：

```powershell
rg -n "password=|apiKey=|connect\.|ssh -p|BEGIN .*PRIVATE KEY" . -g "!target/**" -g "!external/**"
```

如果命中真实密码、API Key、服务器地址或私钥，先改成 example 模板或环境变量，再提交。

## 推荐首次提交流程

```powershell
git init
git add .
git status --short
git commit -m "Initial public release"
git branch -M main
git remote add origin https://github.com/<your-name>/ibank-smart-personal-banking.git
git push -u origin main
```

推送前一定查看 `git status --short`，确认没有本地配置、数据集和模型权重进入暂存区。

