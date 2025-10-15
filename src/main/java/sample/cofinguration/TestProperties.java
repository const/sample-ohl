package sample.cofinguration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@ConfigurationProperties("test")
@Getter
@Setter
public class TestProperties {
    private PrepareProperties prepare = new PrepareProperties();
    private Measure measure;
    private SingleProperties single = new SingleProperties();
    private ListProperties list = new ListProperties();
    private SpringDataJdbcProperties springDataJdbcProperties = new SpringDataJdbcProperties();


    @Getter
    @Setter
    public static class Measure {
        private double latencyPercentile = 0.95;
        private Duration warmUp = Duration.ofMinutes(1);
        private Duration main = Duration.ofMinutes(3);
    }

    @Getter
    @Setter
    public static class SingleProperties {
        private int threads = 20;
        private int shortListSize = 10000;
        private Set<String> enabledVariants = Set.of();
    }


    @Getter
    @Setter
    public static class ListProperties {
        private int threads = 10;
        private int shortListSize = 100000;
        private List<Integer> batchSizes = List.of(1, 10, 100, 1000, 10000);
        private Set<String> enabledVariants = Set.of();
    }

    @Getter
    @Setter
    public static class PrepareProperties {
        private int batchSize = 1000;
        private int minCompanies = 1_000_000;
    }

    @Getter
    @Setter
    public static class SpringDataJdbcProperties {
        private boolean singleQueryLoadingEnabled = true;
    }
}
