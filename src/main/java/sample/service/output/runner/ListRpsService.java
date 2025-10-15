package sample.service.output.runner;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.ColumnData;
import com.github.freva.asciitable.HorizontalAlign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import sample.cofinguration.TestProperties;
import sample.service.output.OutputVariant;

import java.util.*;
import java.util.function.Function;

@Service
@Slf4j
@RequiredArgsConstructor
@EnableConfigurationProperties(TestProperties.class)
public class ListRpsService {
    private final TestProperties testProperties;
    private final TransactionTemplate transactionTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final StressTestService stressTestService;


    public void testVariants() {
        var shortList = loadShortList();
        var tables = new ArrayList<Map.Entry<Integer, String>>();
        var summary = new LinkedHashMap<String, Map<Integer, Double>>();
        var summaryLatency = new LinkedHashMap<String, Map<Integer, Double>>();
        var properties = testProperties.getList();
        var batchSizes = properties.getBatchSizes();
        var rnd = new Random();
        for (int batchSize : batchSizes) {
            log.info("Calculating case {}", batchSize);
            var results = stressTestService.getRps(
                    properties.getThreads(),
                    properties.getEnabledVariants(), OutputVariant::isListSupported, outputVariant -> {
                        var positions = new HashSet<Integer>();
                        while (positions.size() < batchSize) {
                            positions.add(rnd.nextInt(properties.getShortListSize()));
                        }
                        outputVariant.findList(positions.stream().map(shortList::get).toList());
                    }
            );
            for (Map.Entry<String, StressTestService.TestResult> result : results) {
                summary.computeIfAbsent(result.getKey(), k -> new LinkedHashMap<>())
                        .put(batchSize, (result.getValue().count() * batchSize * 1000.0) / testProperties.getMeasure().getMain().toMillis());
                summaryLatency.computeIfAbsent(result.getKey(), k -> new LinkedHashMap<>())
                        .put(batchSize, result.getValue().latency() / 1_000_000.0);
            }

            tables.add(Map.entry(batchSize, stressTestService.formatResults(results, batchSize)));
        }

        for (Map.Entry<Integer, String> table : tables) {
            System.out.println("Size: " + table.getKey());
            System.out.println(table.getValue());
        }

        System.out.println("Summary");
        var columns = new ArrayList<ColumnData<String>>();
        columns.add(new Column().header("Name").dataAlign(HorizontalAlign.LEFT).with(Function.identity()));
        for (int batchSize : batchSizes) {
            columns.add(new Column().header("%,d".formatted(batchSize)).dataAlign(HorizontalAlign.LEFT)
                    .with(v -> "%,.2f".formatted(summary.get(v).get(batchSize))));
        }
        System.out.println(AsciiTable.getTable(summary.keySet().stream().toList(), columns));

        System.out.println("For spreadsheet");
        System.out.print("Name");
        for (int batchSize : batchSizes) {
            System.out.print(";" + batchSize);
        }
        System.out.println();
        for (Map.Entry<String, Map<Integer, Double>> entry : summary.entrySet()) {
            System.out.print(entry.getKey());
            for (int batchSize : batchSizes) {
                System.out.printf(";%.2f", entry.getValue().get(batchSize));
            }
            System.out.println();
        }
        System.out.println("Latency diagram data");
        System.out.print("Name");
        for (int batchSize : batchSizes) {
            System.out.print(";" + batchSize);
        }
        System.out.println();
        for (Map.Entry<String, Map<Integer, Double>> entry : summaryLatency.entrySet()) {
            System.out.print(entry.getKey());
            for (int batchSize : batchSizes) {
                System.out.printf(";%.5f", entry.getValue().get(batchSize));
            }
            System.out.println();
        }
    }

    private List<UUID> loadShortList() {
        return transactionTemplate.execute(tx -> jdbcTemplate.query("""
                        select company_id
                        from company c tablesample system_rows(?)
                        """,
                (rs, i) -> rs.getObject(1, UUID.class),
                testProperties.getList().getShortListSize()));
    }
}
