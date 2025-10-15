package sample.service.output.runner;

import java.util.ArrayList;

public final class LatencyTracker {
    private final static int BATCH_SIZE = 10000;
    private int count;
    private long[] dataNs;
    private final ArrayList<Tracked> saved = new ArrayList<>();


    public void track(long timeNs) {
        if (dataNs == null) {
            dataNs = new long[BATCH_SIZE];
            count = 0;
        } else if (count == BATCH_SIZE) {
            saved.add(new Tracked(dataNs, count));
            dataNs = new long[BATCH_SIZE];
            count = 0;
        }
        dataNs[count++] = timeNs;
    }

    public int copy(long[] target, int offset) {
        ensureFinished();
        for (Tracked tracked : saved) {
            int n = tracked.count;
            System.arraycopy(tracked.dataNs, 0, target, offset, n);
            offset += n;
        }
        return offset;
    }

    public void finish() {
        if (dataNs != null) {
            saved.add(new Tracked(dataNs, count));
            dataNs = null;
            count = 0;
        }
    }

    public int size() {
        ensureFinished();
        return saved.stream().mapToInt(Tracked::count).sum();
    }

    private void ensureFinished() {
        if (dataNs != null) {
            throw new IllegalStateException("Not finished");
        }
    }

    public record Tracked(long[] dataNs, int count) {
    }
}
