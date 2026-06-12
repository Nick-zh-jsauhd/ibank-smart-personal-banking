package com.bank.tool;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class RiskGraphSampler {
    private static final int DEFAULT_MAX_CONTEXT = 100000;
    private static final int DEFAULT_MAX_HARD_NEGATIVES = 100000;
    private static final int DEFAULT_MAX_BACKGROUND = 50000;
    private static final long DEFAULT_SEED = 20260529L;
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final DateTimeFormatter DEFAULT_OUTPUT_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static void main(String[] args) {
        Options options;
        try {
            options = Options.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            Options.printUsage();
            System.exit(2);
            return;
        }

        RiskGraphSampler sampler = new RiskGraphSampler();
        try {
            SampleStats stats = sampler.sample(options);
            System.out.println("Risk graph sampling completed.");
            System.out.println("Input: " + options.inputFile.toAbsolutePath().normalize());
            if (!options.dryRun) {
                System.out.println("Output: " + options.outputFile.toAbsolutePath().normalize());
                System.out.println("Meta: " + metadataPath(options.outputFile).toAbsolutePath().normalize());
            }
            System.out.println("Total rows: " + stats.totalRows);
            System.out.println("Positive rows: " + stats.positiveRows);
            System.out.println("Positive accounts: " + stats.positiveAccounts);
            System.out.println("Hard amount threshold: " + stats.hardAmountThreshold.toPlainString());
            System.out.println("Context candidates: " + stats.contextCandidates);
            System.out.println("Hard negative candidates: " + stats.hardNegativeCandidates);
            System.out.println("Background candidates: " + stats.backgroundCandidates);
            System.out.println("Selected rows: " + stats.selectedRows);
            System.out.println("Selected positives: " + stats.selectedPositiveRows);
            System.out.println("Selected context negatives: " + stats.selectedContextRows);
            System.out.println("Selected hard negatives: " + stats.selectedHardNegativeRows);
            System.out.println("Selected background negatives: " + stats.selectedBackgroundRows);
            System.out.println("Selected positive ratio: " + stats.selectedPositiveRatioText());
            if (options.dryRun) {
                System.out.println("Dry run only. Output CSV was not written.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private SampleStats sample(Options options) throws IOException {
        if (!Files.isRegularFile(options.inputFile)) {
            throw new IllegalArgumentException("Input CSV not found: " + options.inputFile);
        }

        Header header;
        try (BufferedReader reader = Files.newBufferedReader(options.inputFile, StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("Input CSV is empty: " + options.inputFile);
            }
            header = Header.from(headerLine);
            requireIbmAmlHeader(header);
        }

        SampleStats stats = firstPass(options, header);
        stats.hardAmountThreshold = options.hardAmountThreshold != null
                ? options.hardAmountThreshold
                : percentile(stats.positiveAmounts, options.hardAmountPercentile);

        Selection selection = secondPass(options, header, stats);
        stats.selectedPositiveRows = stats.positiveRows;
        stats.selectedContextRows = selection.contextRows.size();
        stats.selectedHardNegativeRows = selection.hardNegativeRows.size();
        stats.selectedBackgroundRows = selection.backgroundRows.size();
        stats.selectedRows = stats.selectedPositiveRows + stats.selectedContextRows
                + stats.selectedHardNegativeRows + stats.selectedBackgroundRows;

        if (!options.dryRun) {
            writeSample(options, selection);
            writeMetadata(options, stats);
        }
        return stats;
    }

    private SampleStats firstPass(Options options, Header header) throws IOException {
        SampleStats stats = new SampleStats();
        try (BufferedReader reader = Files.newBufferedReader(options.inputFile, StandardCharsets.UTF_8)) {
            reader.readLine();
            String line;
            int rowNo = 0;
            while ((line = reader.readLine()) != null) {
                rowNo++;
                IbmAmlRow row = IbmAmlRow.parse(rowNo, line, header);
                stats.totalRows++;
                if (row.laundering) {
                    stats.positiveRows++;
                    stats.positiveRowNumbers.add(rowNo);
                    stats.positiveAccountsSet.add(row.fromNodeKey);
                    stats.positiveAccountsSet.add(row.toNodeKey);
                    stats.positiveBanks.add(row.fromBank);
                    stats.positiveBanks.add(row.toBank);
                    stats.positivePaymentFormats.add(row.paymentFormat);
                    stats.positiveAmounts.add(row.amountPaid.compareTo(ZERO) > 0 ? row.amountPaid : row.amountReceived);
                }
            }
        }
        stats.positiveAccounts = stats.positiveAccountsSet.size();
        if (stats.positiveRows == 0) {
            throw new IllegalArgumentException("Input has no positive Is Laundering rows.");
        }
        return stats;
    }

    private Selection secondPass(Options options, Header header, SampleStats stats) throws IOException {
        BoundedSampler contextSampler = new BoundedSampler(options.maxContext);
        BoundedSampler hardNegativeSampler = new BoundedSampler(options.maxHardNegatives);
        BoundedSampler backgroundSampler = new BoundedSampler(options.maxBackground);

        try (BufferedReader reader = Files.newBufferedReader(options.inputFile, StandardCharsets.UTF_8)) {
            reader.readLine();
            String line;
            int rowNo = 0;
            while ((line = reader.readLine()) != null) {
                rowNo++;
                IbmAmlRow row = IbmAmlRow.parse(rowNo, line, header);
                if (row.laundering) {
                    continue;
                }

                boolean context = stats.positiveAccountsSet.contains(row.fromNodeKey)
                        || stats.positiveAccountsSet.contains(row.toNodeKey);
                if (context) {
                    stats.contextCandidates++;
                    contextSampler.offer(rowNo, score(options.seed, rowNo, 1));
                    continue;
                }

                boolean hardNegative = isHardNegative(row, stats);
                if (hardNegative) {
                    stats.hardNegativeCandidates++;
                    hardNegativeSampler.offer(rowNo, score(options.seed, rowNo, 2));
                    continue;
                }

                stats.backgroundCandidates++;
                backgroundSampler.offer(rowNo, score(options.seed, rowNo, 3));
            }
        }

        Selection selection = new Selection();
        selection.positiveRows.addAll(stats.positiveRowNumbers);
        selection.contextRows.addAll(contextSampler.rowNumbers());
        selection.hardNegativeRows.addAll(hardNegativeSampler.rowNumbers());
        selection.backgroundRows.addAll(backgroundSampler.rowNumbers());
        return selection;
    }

    private boolean isHardNegative(IbmAmlRow row, SampleStats stats) {
        BigDecimal amount = row.amountPaid.compareTo(ZERO) > 0 ? row.amountPaid : row.amountReceived;
        boolean highAmount = amount.compareTo(stats.hardAmountThreshold) >= 0;
        boolean sameFormat = stats.positivePaymentFormats.contains(row.paymentFormat);
        boolean sameBank = stats.positiveBanks.contains(row.fromBank) || stats.positiveBanks.contains(row.toBank);
        return highAmount || (sameFormat && sameBank);
    }

    private void writeSample(Options options, Selection selection) throws IOException {
        Path parent = options.outputFile.toAbsolutePath().normalize().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Set<Integer> selected = new HashSet<Integer>();
        selected.addAll(selection.positiveRows);
        selected.addAll(selection.contextRows);
        selected.addAll(selection.hardNegativeRows);
        selected.addAll(selection.backgroundRows);

        try (BufferedReader reader = Files.newBufferedReader(options.inputFile, StandardCharsets.UTF_8);
             BufferedWriter writer = Files.newBufferedWriter(options.outputFile, StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            writer.write(headerLine);
            writer.newLine();

            String line;
            int rowNo = 0;
            while ((line = reader.readLine()) != null) {
                rowNo++;
                if (selected.contains(rowNo)) {
                    writer.write(line);
                    writer.newLine();
                }
            }
        }
    }

    private void writeMetadata(Options options, SampleStats stats) throws IOException {
        Path metadataPath = metadataPath(options.outputFile);
        try (BufferedWriter writer = Files.newBufferedWriter(metadataPath, StandardCharsets.UTF_8)) {
            writer.write("{\n");
            writer.write("  \"strategy\": \"IBM_AML_POSITIVE_CENTERED_EDGE_SAMPLE\",\n");
            writer.write("  \"input_file\": \"" + json(options.inputFile.toAbsolutePath().normalize().toString()) + "\",\n");
            writer.write("  \"output_file\": \"" + json(options.outputFile.toAbsolutePath().normalize().toString()) + "\",\n");
            writer.write("  \"seed\": " + options.seed + ",\n");
            writer.write("  \"total_rows\": " + stats.totalRows + ",\n");
            writer.write("  \"positive_rows\": " + stats.positiveRows + ",\n");
            writer.write("  \"positive_accounts\": " + stats.positiveAccounts + ",\n");
            writer.write("  \"hard_amount_threshold\": \"" + stats.hardAmountThreshold.toPlainString() + "\",\n");
            writer.write("  \"context_candidates\": " + stats.contextCandidates + ",\n");
            writer.write("  \"hard_negative_candidates\": " + stats.hardNegativeCandidates + ",\n");
            writer.write("  \"background_candidates\": " + stats.backgroundCandidates + ",\n");
            writer.write("  \"selected_rows\": " + stats.selectedRows + ",\n");
            writer.write("  \"selected_positive_rows\": " + stats.selectedPositiveRows + ",\n");
            writer.write("  \"selected_context_rows\": " + stats.selectedContextRows + ",\n");
            writer.write("  \"selected_hard_negative_rows\": " + stats.selectedHardNegativeRows + ",\n");
            writer.write("  \"selected_background_rows\": " + stats.selectedBackgroundRows + ",\n");
            writer.write("  \"selected_positive_ratio\": \"" + stats.selectedPositiveRatioText() + "\"\n");
            writer.write("}\n");
        }
    }

    private static void requireIbmAmlHeader(Header header) {
        header.require("Timestamp");
        header.require("From Bank");
        header.require("Account");
        header.require("To Bank");
        header.requireAny("Account2", "Account1", "To Account", "ToAccount");
        header.require("Amount Received");
        header.require("Amount Paid");
        header.require("Payment Format");
        header.require("Is Laundering");
    }

    private static BigDecimal percentile(List<BigDecimal> values, double percentile) {
        if (values.isEmpty()) {
            return ZERO;
        }
        List<BigDecimal> sorted = new ArrayList<BigDecimal>(values);
        Collections.sort(sorted);
        int index = (int) Math.floor((sorted.size() - 1) * percentile);
        if (index < 0) {
            index = 0;
        }
        if (index >= sorted.size()) {
            index = sorted.size() - 1;
        }
        return sorted.get(index).setScale(2, RoundingMode.HALF_UP);
    }

    private static long score(long seed, int rowNo, int salt) {
        long value = seed;
        value ^= ((long) rowNo) * 0x9E3779B97F4A7C15L;
        value ^= ((long) salt) * 0xBF58476D1CE4E5B9L;
        return mix64(value);
    }

    private static long mix64(long value) {
        long z = value + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static String cleanValue(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.trim();
        while (cleaned.length() >= 2) {
            char first = cleaned.charAt(0);
            char last = cleaned.charAt(cleaned.length() - 1);
            if ((first == '\'' && last == '\'') || (first == '"' && last == '"')) {
                cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
            } else {
                break;
            }
        }
        return cleaned;
    }

    private static BigDecimal money(String value) {
        String cleaned = cleanValue(value).replace(",", "");
        if (cleaned.length() == 0) {
            return ZERO;
        }
        return new BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP);
    }

    private static boolean truthy(String value) {
        String cleaned = cleanValue(value).toLowerCase(Locale.ROOT);
        return "1".equals(cleaned) || "true".equals(cleaned) || "yes".equals(cleaned)
                || "y".equals(cleaned) || "laundering".equals(cleaned);
    }

    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values;
    }

    private static Path metadataPath(Path outputFile) {
        return outputFile.resolveSibling(outputFile.getFileName().toString() + ".meta.json");
    }

    private static String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static final class Options {
        private Path inputFile;
        private Path outputFile;
        private int maxContext = DEFAULT_MAX_CONTEXT;
        private int maxHardNegatives = DEFAULT_MAX_HARD_NEGATIVES;
        private int maxBackground = DEFAULT_MAX_BACKGROUND;
        private long seed = DEFAULT_SEED;
        private double hardAmountPercentile = 0.50d;
        private BigDecimal hardAmountThreshold;
        private boolean dryRun;

        private static Options parse(String[] args) {
            Options options = new Options();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--file".equals(arg)) {
                    options.inputFile = Paths.get(requireValue(args, ++i, arg));
                } else if ("--output".equals(arg)) {
                    options.outputFile = Paths.get(requireValue(args, ++i, arg));
                } else if ("--max-context".equals(arg)) {
                    options.maxContext = Integer.parseInt(requireValue(args, ++i, arg));
                } else if ("--max-hard-negatives".equals(arg)) {
                    options.maxHardNegatives = Integer.parseInt(requireValue(args, ++i, arg));
                } else if ("--max-background".equals(arg)) {
                    options.maxBackground = Integer.parseInt(requireValue(args, ++i, arg));
                } else if ("--seed".equals(arg)) {
                    options.seed = Long.parseLong(requireValue(args, ++i, arg));
                } else if ("--hard-amount-percentile".equals(arg)) {
                    options.hardAmountPercentile = Double.parseDouble(requireValue(args, ++i, arg));
                } else if ("--hard-amount-threshold".equals(arg)) {
                    options.hardAmountThreshold = money(requireValue(args, ++i, arg));
                } else if ("--dry-run".equals(arg)) {
                    options.dryRun = true;
                } else if ("--help".equals(arg) || "-h".equals(arg)) {
                    printUsage();
                    System.exit(0);
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }
            if (options.inputFile == null) {
                throw new IllegalArgumentException("--file is required.");
            }
            if (options.outputFile == null) {
                String outputName = "ibm-aml-sampled-" + LocalDateTime.now().format(DEFAULT_OUTPUT_TIME) + ".csv";
                options.outputFile = Paths.get("target", "risk-graph-samples", outputName);
            }
            if (options.maxContext < 0 || options.maxHardNegatives < 0 || options.maxBackground < 0) {
                throw new IllegalArgumentException("Sample size limits must be >= 0.");
            }
            if (options.hardAmountPercentile < 0.0d || options.hardAmountPercentile > 1.0d) {
                throw new IllegalArgumentException("--hard-amount-percentile must be between 0 and 1.");
            }
            return options;
        }

        private static String requireValue(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException(option + " requires a value.");
            }
            return args[index];
        }

        private static void printUsage() {
            System.out.println("Usage:");
            System.out.println("  java -cp \"target/classes;target/dependency/*\" com.bank.tool.RiskGraphSampler "
                    + "--file E:\\Desktop\\iBank\\dataset\\ibm-aml\\HI-Small_Trans.csv "
                    + "[--output E:\\Desktop\\iBank\\dataset\\ibm-aml\\HI-Small_Trans.sampled.csv] "
                    + "[--max-context 100000] [--max-hard-negatives 100000] [--max-background 50000] "
                    + "[--hard-amount-percentile 0.50] [--seed 20260529] [--dry-run]");
        }
    }

    private static final class Header {
        private final Map<String, Integer> indexes;

        private Header(Map<String, Integer> indexes) {
            this.indexes = indexes;
        }

        private static Header from(String headerLine) {
            List<String> values = parseCsvLine(headerLine);
            Map<String, Integer> indexes = new HashMap<String, Integer>();
            Map<String, Integer> seen = new HashMap<String, Integer>();
            for (int i = 0; i < values.size(); i++) {
                String normalized = normalize(values.get(i));
                int count = seen.containsKey(normalized) ? seen.get(normalized) + 1 : 1;
                seen.put(normalized, count);
                if (count == 1) {
                    indexes.put(normalized, i);
                } else {
                    indexes.put(normalized + count, i);
                }
            }
            return new Header(indexes);
        }

        private void require(String column) {
            if (!indexes.containsKey(normalize(column))) {
                throw new IllegalArgumentException("IBM AML CSV missing column: " + column);
            }
        }

        private void requireAny(String... columns) {
            for (String column : columns) {
                if (indexes.containsKey(normalize(column))) {
                    return;
                }
            }
            throw new IllegalArgumentException("IBM AML CSV missing one of columns: " + join(columns));
        }

        private String value(List<String> row, String... columns) {
            for (String column : columns) {
                Integer index = indexes.get(normalize(column));
                if (index != null && index < row.size()) {
                    return row.get(index);
                }
            }
            return "";
        }

        private static String normalize(String value) {
            String source = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < source.length(); i++) {
                char ch = source.charAt(i);
                if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
                    builder.append(ch);
                }
            }
            return builder.toString();
        }

        private static String join(String[] values) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(values[i]);
            }
            return builder.toString();
        }
    }

    private static final class IbmAmlRow {
        private String fromBank;
        private String toBank;
        private String fromNodeKey;
        private String toNodeKey;
        private BigDecimal amountReceived;
        private BigDecimal amountPaid;
        private String paymentFormat;
        private boolean laundering;

        private static IbmAmlRow parse(int rowNo, String line, Header header) {
            List<String> values = parseCsvLine(line);
            IbmAmlRow row = new IbmAmlRow();
            String fromAccount = cleanValue(header.value(values, "Account"));
            String toAccount = cleanValue(header.value(values, "Account2", "Account1", "To Account", "ToAccount"));
            row.fromBank = cleanValue(header.value(values, "From Bank"));
            row.toBank = cleanValue(header.value(values, "To Bank"));
            row.fromNodeKey = row.fromBank + ":" + fromAccount;
            row.toNodeKey = row.toBank + ":" + toAccount;
            row.amountReceived = money(header.value(values, "Amount Received"));
            row.amountPaid = money(header.value(values, "Amount Paid"));
            row.paymentFormat = cleanValue(header.value(values, "Payment Format"));
            row.laundering = truthy(header.value(values, "Is Laundering"));
            if (row.fromBank.length() == 0 || row.toBank.length() == 0
                    || fromAccount.length() == 0 || toAccount.length() == 0) {
                throw new IllegalArgumentException("IBM AML row " + rowNo + " has missing endpoints.");
            }
            return row;
        }
    }

    private static final class BoundedSampler {
        private final int limit;
        private final PriorityQueue<Candidate> heap;

        private BoundedSampler(int limit) {
            this.limit = limit;
            this.heap = new PriorityQueue<Candidate>(Math.max(1, limit), Candidate.LARGEST_SCORE_FIRST);
        }

        private void offer(int rowNo, long score) {
            if (limit <= 0) {
                return;
            }
            Candidate candidate = new Candidate(rowNo, score);
            if (heap.size() < limit) {
                heap.offer(candidate);
                return;
            }
            Candidate worst = heap.peek();
            if (worst != null && candidate.score < worst.score) {
                heap.poll();
                heap.offer(candidate);
            }
        }

        private Set<Integer> rowNumbers() {
            Set<Integer> rowNumbers = new HashSet<Integer>();
            for (Candidate candidate : heap) {
                rowNumbers.add(candidate.rowNo);
            }
            return rowNumbers;
        }
    }

    private static final class Candidate {
        private static final java.util.Comparator<Candidate> LARGEST_SCORE_FIRST =
                new java.util.Comparator<Candidate>() {
                    @Override
                    public int compare(Candidate left, Candidate right) {
                        return Long.compareUnsigned(right.score, left.score);
                    }
                };

        private final int rowNo;
        private final long score;

        private Candidate(int rowNo, long score) {
            this.rowNo = rowNo;
            this.score = score;
        }
    }

    private static final class Selection {
        private final Set<Integer> positiveRows = new HashSet<Integer>();
        private final Set<Integer> contextRows = new HashSet<Integer>();
        private final Set<Integer> hardNegativeRows = new HashSet<Integer>();
        private final Set<Integer> backgroundRows = new HashSet<Integer>();
    }

    private static final class SampleStats {
        private int totalRows;
        private int positiveRows;
        private int positiveAccounts;
        private int contextCandidates;
        private int hardNegativeCandidates;
        private int backgroundCandidates;
        private int selectedRows;
        private int selectedPositiveRows;
        private int selectedContextRows;
        private int selectedHardNegativeRows;
        private int selectedBackgroundRows;
        private BigDecimal hardAmountThreshold = ZERO;
        private final Set<Integer> positiveRowNumbers = new HashSet<Integer>();
        private final Set<String> positiveAccountsSet = new HashSet<String>();
        private final Set<String> positiveBanks = new HashSet<String>();
        private final Set<String> positivePaymentFormats = new HashSet<String>();
        private final List<BigDecimal> positiveAmounts = new ArrayList<BigDecimal>();

        private String selectedPositiveRatioText() {
            if (selectedRows == 0) {
                return "0.0000%";
            }
            BigDecimal ratio = new BigDecimal(selectedPositiveRows)
                    .multiply(new BigDecimal("100"))
                    .divide(new BigDecimal(selectedRows), 4, RoundingMode.HALF_UP);
            return ratio.toPlainString() + "%";
        }
    }
}
