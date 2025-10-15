package sample.service.prepare;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.format.annotation.DurationFormat;
import org.springframework.format.datetime.standard.DurationFormatterUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import sample.cofinguration.TestProperties;
import sample.dto.CompanyDto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
@Slf4j
@EnableConfigurationProperties(TestProperties.class)
public class PrepareDataServiceImpl implements PrepareDataService {
    private final TransactionTemplate transactionTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final TestProperties testProperties;


    @Override
    public void prepareData() {
        jdbcTemplate.execute("create extension if not exists tsm_system_rows;");
        String ddl;
        try {
            ddl = IOUtils.resourceToString("/database/db.sql", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        jdbcTemplate.execute(ddl);

        Integer current = transactionTemplate.execute(
                tx -> jdbcTemplate.query("select count(*) from company", rs -> {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                    return 0;
                }));
        assert current != null;
        int toCreate = testProperties.getPrepare().getMinCompanies() - current;
        if (toCreate > 0) {
            var session = new PrepareDataSession(current);
            Instant start = Instant.now();
            int created = 0;
            int total = current + toCreate;
            while (toCreate > 0) {
                int batchSize = Math.min(toCreate, testProperties.getPrepare().getBatchSize());
                var passedTime = Duration.between(start, Instant.now());
                var estimatedRemaining = created == 0 ? Duration.ZERO : passedTime.multipliedBy(toCreate).dividedBy(created);
                toCreate -= batchSize;
                log.info("Adding data %s - %s of %s (%02.2f %%, estimated remaining: %s)".formatted(
                        current + created, current + created + batchSize - 1, total,
                        ((double) created) * 100 / total,
                        DurationFormatterUtils.print(estimatedRemaining.truncatedTo(ChronoUnit.SECONDS), DurationFormat.Style.COMPOSITE)));
                var data = session.prepareBatch(batchSize);
                transactionTemplate.executeWithoutResult(tx -> save(data));
                created += batchSize;
            }
            log.info("Preparing data finished: {}",
                    DurationFormatterUtils.print(Duration.between(start, Instant.now()).truncatedTo(ChronoUnit.SECONDS), DurationFormat.Style.COMPOSITE));
        } else {
            log.info("No data creation is needed: {} companies already exists and minimum is {}", current, testProperties.getPrepare().getMinCompanies());
        }
    }

    private void save(List<CompanyDto> data) {
        saveCompany(data);
        saveVerification(data);
        saveOffices(data);
        saveContactPersons(data);
        saveContactDetails(data);
    }

    private void saveContactDetails(List<CompanyDto> data) {
        // language=sql
        var sql = """
                INSERT INTO public.contact_detail
                (contact_person_id, contact_type, value)
                VALUES(?, ?, ?);
                """;
        var r = jdbcTemplate.batchUpdate(sql, data.stream()
                .flatMap(c -> c.getContactPersons().stream())
                .flatMap(p -> p.getDetails()
                        .stream()
                        .map(d -> new Object[]{
                                p.getId(),
                                d.getType().name(),
                                d.getValue()
                        })
                ).toList());
        log.debug("Saved {} contact details", IntStream.of(r).sum());
    }

    private void saveContactPersons(List<CompanyDto> data) {
        // language=sql
        var sql = """
                INSERT INTO public.contact_person
                (contact_person_id, "name", "position", company_id)
                VALUES(?, ?, ?, ?);
                """;
        var r = jdbcTemplate.batchUpdate(sql, data.stream()
                .flatMap(c -> c.getContactPersons()
                        .stream()
                        .map(p -> new Object[]{
                                p.getId(),
                                p.getName(),
                                p.getPosition(),
                                c.getId()
                        })
                ).toList());
        log.debug("Saved {} contact persons", IntStream.of(r).sum());
    }

    private void saveOffices(List<CompanyDto> data) {
        // language=sql
        var sql = """
                INSERT INTO public.company_office
                (office_id, address, city, "name", company_id)
                VALUES(?, ?, ?, ?, ?);
                """;
        var r = jdbcTemplate.batchUpdate(sql, data.stream()
                .flatMap(c -> c.getOffices()
                        .stream()
                        .map(o -> new Object[]{
                                o.getId(),
                                o.getAddress(),
                                o.getCity(),
                                o.getName(),
                                c.getId()
                        })
                ).toList());
        log.debug("Saved {} offices", IntStream.of(r).sum());
    }

    private void saveVerification(List<CompanyDto> data) {
        // language=sql
        var sql = """
                INSERT INTO public.verification_info
                ("comment", status, "timestamp", username, company_id)
                VALUES(?, ?, ?, ?, ?);
                """;
        var r = jdbcTemplate.batchUpdate(sql, data.stream()
                .filter(c -> c.getVerified() != null)
                .map(c ->
                        new Object[]{
                                c.getVerified().getComment(),
                                c.getVerified().getStatus().name(),
                                c.getVerified().getTimestamp().atOffset(ZoneOffset.UTC),
                                c.getVerified().getUser(),
                                c.getId()}
                ).toList());
        log.debug("Saved {} verifications", IntStream.of(r).sum());
    }

    private void saveCompany(List<CompanyDto> data) {
        // language=sql
        var sql = """
                INSERT INTO public.company
                (company_id, description, industry, "name", url)
                VALUES(?, ?, ?, ?, ?);
                """;
        var r = jdbcTemplate.batchUpdate(sql, data.stream()
                .map(c ->
                        new Object[]{c.getId(), c.getDescription(), c.getIndustry(), c.getName(), c.getUrl()}
                ).toList());
        log.debug("Saved {} companies", IntStream.of(r).sum());

    }

}
