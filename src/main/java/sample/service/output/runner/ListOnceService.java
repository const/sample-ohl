package sample.service.output.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import sample.cofinguration.TestProperties;
import sample.service.output.OutputVariant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@EnableConfigurationProperties(TestProperties.class)
public class ListOnceService {
    private static final int SHORT_LIST_SIZE = 10;
    private final TransactionTemplate transactionTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final List<OutputVariant> outputVariants;
    private final ObjectMapper objectMapper;
    private final TestProperties testProperties;

    public void testSelectSingle() {
        var shortList = loadShortList();
        log.info("Test Parameters: {}", shortList.stream().map(u -> "'" + u + "'").collect(Collectors.joining(", ")));
        outputVariants.stream()
                .filter(OutputVariant::isListSupported)
                .filter(v -> testProperties.getList().getEnabledVariants().isEmpty() || testProperties.getList().getEnabledVariants().contains(v.getName()))
                .forEach(v -> transactionTemplate.executeWithoutResult(tx -> {
                    if (!testProperties.getList().getEnabledVariants().isEmpty() && !testProperties.getList().getEnabledVariants().contains(v.getName())) {
                        return;
                    }
                    log.info("Starting variant: {}", v.getName());
                    try {
                        var dir = Paths.get("./target/sample/list/");
                        Files.createDirectories(dir);
                        var r = v.findList(shortList);
                        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(r);
                        log.info("Output for {} is {}", v.getName(), json);
                        var filePath = dir.resolve(v.getName() + ".json");
                        Files.writeString(filePath, json);
                    } catch (IOException e) {
                        log.error("Failed variant for " + v.getName(), e);
                    }
                }));
    }

    private List<UUID> loadShortList() {
        return transactionTemplate.execute(tx -> jdbcTemplate.query("""
                        select company_id
                        from company c tablesample system_rows(?)
                        """,
                (rs, i) -> rs.getObject(1, UUID.class),
                SHORT_LIST_SIZE));
    }
}
