package sample;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import sample.repository.hibernate.CompanyRepository;
import sample.service.output.runner.ListOnceService;
import sample.service.output.runner.ListRpsService;
import sample.service.output.runner.SingleOnceService;
import sample.service.output.runner.SingeRpsService;
import sample.service.prepare.PrepareDataService;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
@EnableJpaRepositories(basePackageClasses = {CompanyRepository.class})
@EnableTransactionManagement
public class Application implements CommandLineRunner {
    private final PrepareDataService prepareDataService;
    private final SingleOnceService selectSingleService;
    private final SingeRpsService singeStressService;
    private final ListRpsService byIdsStressService;
    private final ListOnceService byIdsSingleService;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) {
        int p = 0;
        while(p < args.length && args[p].startsWith("--")) {
            p++;
        }

        if (args.length - p != 1) {
            printUsage();
            return;
        }

        var command = args[p];
        prepareDataService.prepareData();
        switch (command) {
            case "single-once" -> selectSingleService.testSelectSingle();
            case "single-rps" -> singeStressService.testVariants();
            case "list-once" -> byIdsSingleService.testSelectSingle();
            case "list-rps" -> byIdsStressService.testVariants();
            case null, default -> {
                log.error("Unsupported command: {}", command);
                printUsage();
            }
        }
    }

    private void printUsage() {
        log.error("Usage: app (test-input|...)");
    }
}
