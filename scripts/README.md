# iBank Regression Scripts

`regression.ps1` runs the local smoke and regression checks for the iBank web app.

`paysim-import.md` documents the PaySim offline importer for the RiskBrain training-data workflow.

`risk-graph-import.md` documents the graph-first importer for IBM AML, BankSim, and PaySim GNN datasets.

Default behavior:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\regression.ps1
```

The script performs:

- Maven build: `mvn clean package`
- Tomcat deploy to `D:\tomcat9`
- HTTP checks against `http://localhost:8080/bank`
- MySQL consistency checks using `src/main/resources/db.properties`

Useful options:

```powershell
# Run against an already deployed local app.
powershell -ExecutionPolicy Bypass -File .\scripts\regression.ps1 -SkipBuild -SkipDeploy

# Only build and database checks.
powershell -ExecutionPolicy Bypass -File .\scripts\regression.ps1 -SkipDeploy -SkipHttp

# Use another Tomcat path or base URL.
powershell -ExecutionPolicy Bypass -File .\scripts\regression.ps1 -TomcatHome "D:\tomcat9" -BaseUrl "http://localhost:8080/bank"

# Apply schema.sql before running checks.
powershell -ExecutionPolicy Bypass -File .\scripts\regression.ps1 -RunSchemaMigration
```

Current checks include:

- `admin / admin123` can log in and access audit pages.
- `admin_reviewer / admin123` is forbidden from audit and admin-user pages.
- Direct backend JSP access is blocked.
- At least one active `SUPER_ADMIN` exists.
- Security permissions are seeded.
- Successful transactions have ledger entries.
- Account balances match the latest ledger entry.
- Forbidden access is written to `t_admin_audit_log`.
