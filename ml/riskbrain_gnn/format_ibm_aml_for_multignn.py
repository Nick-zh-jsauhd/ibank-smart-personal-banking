#!/usr/bin/env python3
"""Format IBM AML raw CSV for IBM/Multi-GNN without datatable.

The official repository ships ``format_kaggle_files.py`` implemented with the
``datatable`` package. Some GPU images have an older libstdc++ than current
datatable wheels require. This script keeps the same output schema and mapping
order while using pandas plus explicit dictionaries.
"""

from __future__ import annotations

import argparse
from datetime import datetime
from pathlib import Path

import pandas as pd


OUTPUT_COLUMNS = [
    "EdgeID",
    "from_id",
    "to_id",
    "Timestamp",
    "Amount Sent",
    "Sent Currency",
    "Amount Received",
    "Received Currency",
    "Payment Format",
    "Is Laundering",
]


def get_dict_val(name: str, collection: dict[str, int]) -> int:
    if name not in collection:
        collection[name] = len(collection)
    return collection[name]


def format_file(input_csv: Path, output_csv: Path | None = None) -> Path:
    if output_csv is None:
        output_csv = input_csv.with_name("formatted_transactions.csv")

    raw = pd.read_csv(input_csv, dtype=str)
    account_columns = [col for col in raw.columns if col == "Account" or col.startswith("Account.")]
    if len(account_columns) != 2:
        raise ValueError(f"Expected two Account columns, got {account_columns}")
    from_account_col, to_account_col = account_columns

    currency: dict[str, int] = {}
    payment_format: dict[str, int] = {}
    account: dict[str, int] = {}

    timestamps = pd.to_datetime(raw["Timestamp"], format="%Y/%m/%d %H:%M")
    first_dt = timestamps.iloc[0].to_pydatetime()
    start_time = datetime(first_dt.year, first_dt.month, first_dt.day)
    normalized_ts = (timestamps - start_time).dt.total_seconds().astype("int64") + 10

    received_currency_ids: list[int] = []
    paid_currency_ids: list[int] = []
    format_ids: list[int] = []
    from_ids: list[int] = []
    to_ids: list[int] = []

    for recv_cur, pay_cur, fmt, from_bank, from_acc, to_bank, to_acc in zip(
        raw["Receiving Currency"].to_numpy(),
        raw["Payment Currency"].to_numpy(),
        raw["Payment Format"].to_numpy(),
        raw["From Bank"].to_numpy(),
        raw[from_account_col].to_numpy(),
        raw["To Bank"].to_numpy(),
        raw[to_account_col].to_numpy(),
    ):
        received_currency_ids.append(get_dict_val(recv_cur, currency))
        paid_currency_ids.append(get_dict_val(pay_cur, currency))
        format_ids.append(get_dict_val(fmt, payment_format))
        from_ids.append(get_dict_val(from_bank + from_acc, account))
        to_ids.append(get_dict_val(to_bank + to_acc, account))

    formatted = pd.DataFrame(
        {
            "EdgeID": range(len(raw)),
            "from_id": from_ids,
            "to_id": to_ids,
            "Timestamp": normalized_ts,
            "Amount Sent": raw["Amount Paid"].astype(float),
            "Sent Currency": paid_currency_ids,
            "Amount Received": raw["Amount Received"].astype(float),
            "Received Currency": received_currency_ids,
            "Payment Format": format_ids,
            "Is Laundering": raw["Is Laundering"].astype(int),
        },
        columns=OUTPUT_COLUMNS,
    )
    formatted = formatted.sort_values("Timestamp", kind="stable")
    formatted.to_csv(output_csv, index=False)
    return output_csv


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("input_csv", type=Path)
    parser.add_argument("--output", type=Path, default=None)
    args = parser.parse_args()
    output = format_file(args.input_csv, args.output)
    print(output)


if __name__ == "__main__":
    main()
