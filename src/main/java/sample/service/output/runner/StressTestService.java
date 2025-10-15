package sample.service.output.runner;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.ColumnData;
import com.github.freva.asciitable.HorizontalAlign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import sample.cofinguration.TestProperties;
import sample.service.output.OutputVariant;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Service
@EnableConfigurationProperties(TestProperties.class)
@ConfigurationProperties
@Slf4j
@RequiredArgsConstructor
public class StressTestService {
    private final List<OutputVariant> variants;
    private final TestProperties testProperties;
    private final TransactionTemplate transactionTemplate;

    public String formatResults(List<Map.Entry<String, TestResult>> results) {
        return formatResults(results, null);
    }

    public String formatResults(List<Map.Entry<String, TestResult>> results, Integer multiplier) {
        long baseline = results.getFirst().getValue().count;
        var columns = new ArrayList<ColumnData<Map.Entry<String, TestResult>>>();
        columns.add(new Column().header("Name").dataAlign(HorizontalAlign.LEFT)
                .with(Map.Entry::getKey));
        columns.add(new Column().header("Count").dataAlign(HorizontalAlign.RIGHT)
                .with(e -> String.format("%,d", e.getValue().count)));
        long measureMillis = testProperties.getMeasure().getMain().toMillis();
        columns.add(new Column().header("RPS").dataAlign(HorizontalAlign.RIGHT)
                .with(e -> String.format("%,.2f", ((double) e.getValue().count * 1000) / measureMillis)));
        if (multiplier != null) {
            columns.add(new Column().header("RPS * N").dataAlign(HorizontalAlign.RIGHT)
                    .with(e -> String.format("%,.2f", ((double) e.getValue().count * 1000 * multiplier) / measureMillis)));
        }
        columns.add(new Column().header("RPS %").dataAlign(HorizontalAlign.RIGHT)
                .with(e -> String.format("%.2f %%", (100.0 * e.getValue().count) / baseline)));
        return AsciiTable.getTable(results, columns);
    }

    public List<Map.Entry<String, TestResult>> getRps(int threads,
                                                      Set<String> enabledVariants, Consumer<OutputVariant> testAction) {
        return getRps(threads, enabledVariants, t -> true, testAction);
    }


    public List<Map.Entry<String, TestResult>> getRps(int threads,
                                                      Set<String> enabledVariants, Predicate<OutputVariant> isOutputEnabled, Consumer<OutputVariant> testAction) {
        return variants.stream()
                .filter(isOutputEnabled)
                .filter(o -> enabledVariants.isEmpty() || enabledVariants.contains(o.getName()))
                .map(o -> Map.entry(o.getName(), getRpsForVariant(threads, o, testAction)))
                .toList();
    }

    private TestResult getRpsForVariant(int threads, OutputVariant outputVariant, Consumer<OutputVariant> testAction) {
        var error = new AtomicReference<Throwable>();
        var name = outputVariant.getName();
        var isStarted = new AtomicBoolean();
        var isFinished = new AtomicBoolean();
        var threadResultFutures = new ArrayList<CompletableFuture<ThreadResult>>();
        List<ThreadResult> threadResults;
        for (int i = 0; i < threads; i++) {
            var f = new CompletableFuture<ThreadResult>();
            threadResultFutures.add(f);
            new Thread(() ->
                    runTestThread(outputVariant, testAction, isStarted, isFinished, f)
            ).start();
        }
        try {
            log.info("Starting warm up phase {}", name);
            Thread.sleep(testProperties.getMeasure().getWarmUp().toMillis());
            log.info("Starting measure phase {}", name);
            isStarted.set(true);
            Thread.sleep(testProperties.getMeasure().getMain().toMillis());
            isFinished.set(true);
            log.info("Finished measure phase {}", name);
            threadResults = threadResultFutures.stream().map(threadResultCompletableFuture -> {
                try {
                    return threadResultCompletableFuture.get();
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }).toList();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (error.get() != null) {
            throw new RuntimeException("Failed to execute variant", error.get());
        }
        var dataNs = new long[threadResults.stream().mapToInt(t -> t.latency().size()).sum()];
        int offset = 0;
        for (ThreadResult threadResult : threadResults) {
            offset = threadResult.latency().copy(dataNs, offset);
        }
        Arrays.sort(dataNs);
        int toUse = (int) (testProperties.getMeasure().getLatencyPercentile() * dataNs.length);
        long latency = dataNs[toUse - 1];
        var count = threadResults.stream().mapToInt(t -> t.count).sum();
        return new TestResult(count, latency);
    }

    private void runTestThread(OutputVariant outputVariant,
                               Consumer<OutputVariant> testAction,
                               AtomicBoolean isStarted,
                               AtomicBoolean isFinished,
                               CompletableFuture<ThreadResult> result) {
        try {
            warmUp(outputVariant, testAction, isStarted);
            var latencyTracker = new LatencyTracker();
            int count = measure(outputVariant, testAction, isFinished, latencyTracker);
            latencyTracker.finish();
            result.complete(new ThreadResult(count, latencyTracker));
        } catch (Exception ex) {
            log.error("Failed " + outputVariant.getName(), ex);
            result.completeExceptionally(ex);
        }
    }

    private void warmUp(OutputVariant outputVariant, Consumer<OutputVariant> testAction, AtomicBoolean isStarted) {
        while (!isStarted.get()) {
            transactionTemplate.executeWithoutResult(tx -> testAction.accept(outputVariant));
        }
    }

    private int measure(OutputVariant outputVariant, Consumer<OutputVariant> testAction,
                        AtomicBoolean isFinished, LatencyTracker latencyTracker) {
        int count = 0;
        while (true) {
            transactionTemplate.executeWithoutResult(tx -> {
                long timeNs = System.nanoTime();
                testAction.accept(outputVariant);
                latencyTracker.track(System.nanoTime() - timeNs);
            });
            if (!isFinished.get()) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    record ThreadResult(int count, LatencyTracker latency) {
    }

    public record TestResult(int count, long latency) {
    }
}
