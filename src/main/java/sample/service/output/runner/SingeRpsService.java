package sample.service.output.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import sample.cofinguration.TestProperties;
import sample.service.output.OutputVariant;

import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@EnableConfigurationProperties(TestProperties.class)
public class SingeRpsService {
    private final TestProperties testProperties;
    private final TransactionTemplate transactionTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final StressTestService stressTestService;

    public void testVariants() {
        var shortList = loadShortList();
        var rnd = new Random();
        var properties = testProperties.getSingle();
        var results = stressTestService.getRps(
                properties.getThreads(),
                properties.getEnabledVariants(),
                OutputVariant::isSingleSupported,
                outputVariant -> outputVariant.findSingle(shortList.get(rnd.nextInt(shortList.size()))).orElseThrow());
        System.out.println(stressTestService.formatResults(results));
        System.out.println("For spreadsheet");
        System.out.print("Name;RPS\n");
        double multiplier = 1 / (double) testProperties.getMeasure().getMain().toSeconds();
        for (var result : results) {
            System.out.printf("%s;%.2f\n", result.getKey(), result.getValue().count() * multiplier);
        }
        System.out.println("Latency diagram data");
        System.out.println("Name;Latency (microseconds)");
        for (var result : results) {
            System.out.printf("%s;%.2f\n", result.getKey(), result.getValue().latency() / 1000.0 );
        }
    }

    private List<UUID> loadShortList() {
        return transactionTemplate.execute(tx -> jdbcTemplate.query("""
                        select company_id
                        from company c tablesample system_rows(?)
                        """,
                (rs, i) -> rs.getObject(1, UUID.class),
                testProperties.getSingle().getShortListSize()));
    }
}
