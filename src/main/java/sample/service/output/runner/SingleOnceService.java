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

@Service
@Slf4j
@RequiredArgsConstructor
@EnableConfigurationProperties(TestProperties.class)
public class SingleOnceService {
    private final TransactionTemplate transactionTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final List<OutputVariant> outputVariants;
    private final ObjectMapper objectMapper;
    private final TestProperties testProperties;


    public void testSelectSingle() {
        var sampleUUID = findAnyFullObjectUUD();
        log.info("Test Parameters: '{}'", sampleUUID);

        outputVariants.stream()
                .filter(OutputVariant::isSingleSupported)
                .filter(v -> testProperties.getSingle().getEnabledVariants().isEmpty() || testProperties.getSingle().getEnabledVariants().contains(v.getName()))
                .forEach(v -> transactionTemplate.executeWithoutResult(tx -> {
                    if (!testProperties.getList().getEnabledVariants().isEmpty() && !testProperties.getList().getEnabledVariants().contains(v.getName())) {
                        return;
                    }
                    log.info("Starting variant: {}", v.getName());
                    try {
                        var dir = Paths.get("./target/sample/single/");
                        Files.createDirectories(dir);
                        var r = v.findSingle(sampleUUID).orElseThrow(() -> new IllegalStateException("Value is not found for variant " + v.getName()));
                        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(r);
                        log.info("Output for {} is {}", v.getName(), json);
                        var filePath = dir.resolve(v.getName() + ".json");
                        Files.writeString(filePath, json);
                    } catch (IOException e) {
                        log.error("Failed variant for " + v.getName(), e);
                    }
                }));
    }

    /**
     * @return UUID of object where all sub objects present
     */
    private UUID findAnyFullObjectUUD() {
        return transactionTemplate.execute(tx -> jdbcTemplate.query(
                // language=sql
                """
                        select c.company_id from company c
                        where exists (select 1 from verification_info vi where vi.company_id = c.company_id)
                        and (select count(*) from company_office co where co.company_id = c.company_id) > 2
                        and (
                            select count(*) from contact_person cp
                            where cp.company_id = c.company_id
                            and (
                                select count(*) from contact_detail cd
                                where cd.contact_person_id = cp.contact_person_id
                            ) > 2
                        ) > 2
                        limit 1
                        """,
                rs -> {
                    if (!rs.next()) {
                        throw new IllegalStateException("Sample object not found");
                    }
                    return UUID.fromString(rs.getString(1));
                }
        ));
    }
}
