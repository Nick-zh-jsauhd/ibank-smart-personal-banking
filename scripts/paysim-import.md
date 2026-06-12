# PaySim Importer

`com.bank.tool.PaySimImporter` imports PaySim CSV data into iBank for the first RiskBrain offline-scoring workflow.

It writes both business data and model-training data:

- `t_user`, `t_customer`, `t_account`: stable synthetic entities mapped from PaySim account IDs.
- `t_transaction`, `t_ledger_entry`, `t_bill_payment`: visible iBank transactions and ledger entries.
- `t_risk_event`: generated for `isFraud=1` or `isFlaggedFraud=1`.
- `t_risk_dataset_batch`, `t_risk_external_entity_map`, `t_risk_training_sample`: training and traceability tables.
- `t_risk_model_score`: reserved for later offline model-score import.

## Build

```powershell
mvn -q -DskipTests package dependency:copy-dependencies
```

## Dry Run

```powershell
java -cp "target/classes;target/dependency/*" com.bank.tool.PaySimImporter `
  --file scripts/sample-paysim.csv `
  --limit 5 `
  --batch-code PAYSIM_DRY_RUN `
  --dry-run
```

## Import A Small Local Sample

```powershell
java -cp "target/classes;target/dependency/*" com.bank.tool.PaySimImporter `
  --file scripts/sample-paysim.csv `
  --limit 5 `
  --batch-code PAYSIM_SMOKE `
  --start-date 2026-01-01 `
  --commit-size 2
```

## Import The Real PaySim CSV

Download PaySim from Kaggle, then point `--file` to the CSV.

```powershell
java -Xmx2g -cp "target/classes;target/dependency/*" com.bank.tool.PaySimImporter `
  --file D:\datasets\paysim\PS_20174392719_1491204439457_log.csv `
  --limit 50000 `
  --batch-code PAYSIM_50K_V1 `
  --start-date 2026-01-01 `
  --commit-size 500
```

Use `--limit 0` only when you intentionally want to import the full dataset. The full PaySim file has millions of rows, so keep the first local run at `50000` rows or less.

## Mapping

| PaySim field | iBank target |
| --- | --- |
| `nameOrig` | source synthetic user/customer/account |
| `nameDest` | target synthetic user/customer/account or merchant |
| `type=CASH_IN` | `t_transaction.txn_type=DEPOSIT` |
| `type=CASH_OUT` | `t_transaction.txn_type=WITHDRAW` |
| `type=TRANSFER` | `t_transaction.txn_type=TRANSFER_INNER` |
| `type=PAYMENT` / `DEBIT` | `t_transaction.txn_type=PAYMENT` |
| `isFraud` | `t_risk_training_sample.label_fraud`, high-risk event |
| `isFlaggedFraud` | `t_risk_training_sample.label_flagged_rule`, warning event |

The importer stores original PaySim balances in `t_risk_training_sample` for model research, but the iBank business ledger maintains its own account balances.
