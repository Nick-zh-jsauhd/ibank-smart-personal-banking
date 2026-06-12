# Risk Graph Importer

`com.bank.tool.RiskGraphImporter` imports external fraud/AML datasets into graph-first training tables for the RiskBrain GNN workflow.

It does not write customer-facing business transactions. PaySim can still be imported through `PaySimImporter` for product demos, while this importer builds a clean account/counterparty graph for model training.

## Tables

- `t_risk_graph_dataset_batch`: one import batch per dataset file and run.
- `t_risk_graph_node`: account, customer, or merchant nodes with aggregate in/out degree and amount features.
- `t_risk_graph_edge`: transaction edges with labels, amount, timestamp, type, source row number, and feature JSON.

## Supported Formats

- `IBM_AML`: IBM Transactions for Anti Money Laundering style CSV.
- `BANKSIM`: BankSim customer-to-merchant payment fraud CSV.
- `PAYSIM`: PaySim CSV projected as graph edges.
- `AUTO`: detect by CSV header.

## Build

```powershell
mvn -q -DskipTests package dependency:copy-dependencies
```

## Dry Run

```powershell
java -cp "target/classes;target/dependency/*" com.bank.tool.RiskGraphImporter `
  --file scripts/sample-ibm-aml.csv `
  --format IBM_AML `
  --limit 5 `
  --batch-code IBM_AML_DRY_RUN `
  --dry-run
```

## Import IBM AML Sample

```powershell
java -cp "target/classes;target/dependency/*" com.bank.tool.RiskGraphImporter `
  --file scripts/sample-ibm-aml.csv `
  --format IBM_AML `
  --limit 5 `
  --batch-code IBM_AML_SMOKE `
  --commit-size 2
```

## Import BankSim Sample

```powershell
java -cp "target/classes;target/dependency/*" com.bank.tool.RiskGraphImporter `
  --file scripts/sample-banksim.csv `
  --format BANKSIM `
  --limit 5 `
  --batch-code BANKSIM_SMOKE `
  --start-date 2026-01-01 `
  --commit-size 2
```

## First Real Dataset Runs

Keep first imports bounded, then expand after verifying graph statistics.

For imbalanced IBM AML data, do not train from the first N rows. First build a positive-centered sampled file.

```powershell
java -Xmx2g -cp "target/classes;target/dependency/*" com.bank.tool.RiskGraphSampler `
  --file E:\Desktop\iBank\dataset\ibm-aml\HI-Small_Trans.csv `
  --output E:\Desktop\iBank\dataset\ibm-aml\HI-Small_Trans.sampled.csv `
  --max-context 100000 `
  --max-hard-negatives 100000 `
  --max-background 50000 `
  --hard-amount-percentile 0.50 `
  --seed 20260529
```

The sampler keeps all `Is Laundering=1` edges, samples normal one-hop context around laundering accounts, mines high-risk-looking normal edges as hard negatives, and adds background negatives. On HI-Small, this raises the training file positive ratio from about `0.1%` to about `2%` with the default limits.

```powershell
java -Xmx2g -cp "target/classes;target/dependency/*" com.bank.tool.RiskGraphImporter `
  --file E:\Desktop\iBank\dataset\ibm-aml\HI-Small_Trans.sampled.csv `
  --format IBM_AML `
  --dataset-name IBM_AML `
  --limit 0 `
  --batch-code IBM_AML_HI_SMALL_SAMPLED_V1 `
  --commit-size 1000
```

```powershell
java -Xmx2g -cp "target/classes;target/dependency/*" com.bank.tool.RiskGraphImporter `
  --file E:\Desktop\iBank\dataset\bs140513_032310.csv `
  --format BANKSIM `
  --dataset-name BANKSIM `
  --limit 100000 `
  --batch-code BANKSIM_100K_V1 `
  --commit-size 1000
```

## Modeling Notes

- For GNN, treat `t_risk_graph_edge` as the supervised transaction-edge table.
- Use `label_fraud=1` as positive labels. For IBM AML this means laundering; for BankSim/PaySim this means fraud.
- Keep `dataset_name` and `label_source` as features or split keys, because AML laundering and payment fraud are related but not identical labels.
- Train with balanced or hard-negative sampled batches, but validate on a more realistic imbalanced split.

## Export For GPU Training

After importing a graph batch, export it to flat CSV files that can be copied to a GPU server.

```powershell
java -cp "target/classes;target/dependency/*" com.bank.tool.RiskGraphExporter `
  --batch-code IBM_AML_HI_SMALL_100K_V1 `
  --output-dir target/risk-graph-export/IBM_AML_HI_SMALL_100K_V1
```

The exporter writes:

- `nodes.csv`
- `edges.csv`
- `labels.csv`
- `edge_features_v2.csv`
- `feedback_labels.csv`
- `graph_meta.json`

For RiskBrain V2, pass the model version whose manual review feedback should be attached to the export:

```powershell
java -cp "target/classes;target/dependency/*" com.bank.tool.RiskGraphExporter `
  --batch-code IBM_AML_HI_SMALL_SAMPLED_V1 `
  --output-dir target/risk-graph-export/IBM_AML_HI_SMALL_SAMPLED_V1 `
  --feedback-model-version riskbrain-graphsage-ibm-aml-v1
```

`edge_features_v2.csv` avoids using fraud-degree label leakage as model input and adds graph-neighborhood,
amount-ratio, time-window and review-feedback fields. `feedback_labels.csv` records the human review results
from the GNN review queue so that confirmed risks and false positives can be weighted in the next training run.

## Human Feedback Loop

After importing model scores and materializing review cases, risk analysts should review high-priority cases in:

```text
/admin/risk/graph-cases
```

The page now has two feedback outputs:

- `Export training feedback`: downloads reviewed cases from the admin UI as a compact CSV for inspection and
  handoff. `CONFIRMED_RISK` becomes `human_label=1`, `FALSE_POSITIVE` becomes `human_label=0`, while
  `NEED_MORE_DATA` and `IGNORE` are kept as governance signals instead of hard labels.
- `RiskGraphExporter`: exports the full graph protocol package from MySQL, including `edge_features_v2.csv`
  and `feedback_labels.csv`. This is the preferred input for the GPU training server.

Recommended loop:

1. Score a candidate model and import `edge_scores.csv`.
2. Mark the chosen score version as the operational model in `/admin/risk/graph-models`.
3. Sync review candidates in `/admin/risk/graph-cases`.
4. Review cases until the page shows enough trainable feedback.
5. Export the full graph package with `--feedback-model-version`.
6. Train the next model with feedback enabled only for the training split; keep validation/test labels untouched.

Do not use feedback labels from validation or test windows as training labels. The leakage guard should keep
`feedback_mode=off` for strict baseline runs, and only enable feedback-aware training when the split protocol is
explicitly documented.

## Import GNN Edge Scores

After training and scoring on the GPU server, download `edge_scores.csv` and import it back to iBank.

```powershell
java -cp "target/classes;target/dependency/*" com.bank.tool.RiskGraphScoreImporter `
  --file target/riskbrain-server-results/graphsage_full_v2/edge_scores.csv `
  --model-version riskbrain-graphsage-ibm-aml-v1 `
  --feature-version riskgraph-v1 `
  --commit-size 1000
```

Scores are stored in `t_risk_graph_model_score` and linked to `t_risk_graph_edge.graph_edge_id`.
