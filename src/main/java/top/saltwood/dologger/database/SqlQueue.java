package top.saltwood.dologger.database;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import top.saltwood.dologger.Config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class SqlQueue {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long POLL_TIMEOUT_MS = 100L;
    private static final long BUSY_LOG_INTERVAL_MS = 10_000L;

    private final LinkedBlockingQueue<Runnable> queue;
    private final Object producerLock = new Object();
    private final DatabaseManager databaseManager;
    private final Thread workerThread;
    private final AtomicBoolean accepting = new AtomicBoolean(true);
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean processingTask = new AtomicBoolean(false);
    private final AtomicLong completedTasks = new AtomicLong();
    private final AtomicLong failedTasks = new AtomicLong();
    private final AtomicLong rejectedTasks = new AtomicLong();
    private final AtomicLong nextBusyLogAt = new AtomicLong();

    public SqlQueue(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.queue = new LinkedBlockingQueue<>(Math.max(1, Config.queueCapacity));
        this.workerThread = new Thread(this::processQueue, "DoLogger-SQL-Queue");
        this.workerThread.setDaemon(true);
    }

    public void start() {
        workerThread.start();
        LOGGER.info("DoLogger: SQL queue started");
    }

    public boolean enqueue(Runnable task) {
        if (task == null) {
            return false;
        }

        synchronized (producerLock) {
            if (!accepting.get()) {
                return false;
            }

            if (!databaseManager.isAvailable()) {
                rejectedTasks.incrementAndGet();
                databaseManager.logBoundedError("Database unavailable, rejecting queued task");
                return false;
            }

            if (queue.size() + 1 > busyThreshold() || queue.remainingCapacity() < 1) {
                rejectedTasks.incrementAndGet();
                logBusy(1);
                return false;
            }

            return queue.offer(task);
        }
    }

    public boolean enqueue(SqlTask task) {
        if (task == null) {
            return false;
        }

        return enqueue(wrap(task));
    }

    public int enqueueBatch(List<? extends Runnable> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return 0;
        }

        synchronized (producerLock) {
            if (!accepting.get()) {
                return 0;
            }

            if (!databaseManager.isAvailable()) {
                rejectedTasks.addAndGet(tasks.size());
                databaseManager.logBoundedError("Database unavailable, rejecting batch of " + tasks.size() + " tasks");
                return 0;
            }

            int taskCount = 0;
            for (Runnable task : tasks) {
                if (task == null) {
                    continue;
                }
                taskCount++;
            }

            if (taskCount == 0) {
                return 0;
            }

            if (queue.size() + taskCount > busyThreshold() || queue.remainingCapacity() < taskCount) {
                rejectedTasks.addAndGet(taskCount);
                logBusy(taskCount);
                return 0;
            }

            int added = 0;
            for (Runnable task : tasks) {
                if (task == null) {
                    continue;
                }

                if (queue.offer(task)) {
                    added++;
                }
            }

            return added;
        }
    }

    public int enqueueSqlBatch(List<SqlTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return 0;
        }

        return enqueueBatch(tasks.stream().map(this::wrap).toList());
    }

    public int shutdown() {
        accepting.set(false);
        long timeoutMs = Math.max(0L, Config.queueFlushTimeout);
        long deadline = System.currentTimeMillis() + timeoutMs;

        while ((processingTask.get() || !queue.isEmpty()) && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(50L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        int dropped = queue.size();
        if (dropped > 0) {
            queue.clear();
            LOGGER.warn("DoLogger: Shutdown flush dropped {} pending tasks", dropped);
        }

        running.set(false);
        workerThread.interrupt();

        long joinTimeout = Math.max(100L, deadline - System.currentTimeMillis());
        try {
            workerThread.join(joinTimeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        LOGGER.info(
                "DoLogger: SQL queue stopped. Completed: {}, Failed: {}, Dropped: {}",
                completedTasks.get(),
                failedTasks.get(),
                dropped
        );
        LOGGER.info("DoLogger: SQL queue rejected {} tasks while running", rejectedTasks.get());

        return dropped;
    }

    public int getPendingCount() {
        return queue.size();
    }

    public boolean isBusy() {
        return queue.size() >= busyThreshold();
    }

    public long getRejectedCount() {
        return rejectedTasks.get();
    }

    private int busyThreshold() {
        return Math.max(1, Math.min(Config.queueBusyThreshold, Config.queueCapacity));
    }

    private void logBusy(int tasks) {
        long now = System.currentTimeMillis();
        long next = nextBusyLogAt.get();
        if (now < next || !nextBusyLogAt.compareAndSet(next, now + BUSY_LOG_INTERVAL_MS)) {
            return;
        }
        databaseManager.logBoundedError("SQL queue busy, rejecting " + tasks + " async task(s); pending=" + queue.size() + ", threshold=" + busyThreshold());
    }

    private void processQueue() {
        while (running.get() || !queue.isEmpty()) {
            try {
                Runnable task = queue.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (task == null) {
                    continue;
                }

                processingTask.set(true);
                try {
                    task.run();
                    completedTasks.incrementAndGet();
                } catch (RuntimeException e) {
                    failedTasks.incrementAndGet();
                    if (!(e.getCause() instanceof SQLException)) {
                        databaseManager.logBoundedError("Queued task failed: " + e.getMessage());
                    }
                } finally {
                    processingTask.set(false);
                }
            } catch (InterruptedException e) {
                if (!running.get()) {
                    break;
                }
            }
        }
    }

    private Runnable wrap(SqlTask task) {
        return () -> {
            if (!databaseManager.isAvailable()) {
                failedTasks.incrementAndGet();
                databaseManager.logBoundedError("Database unavailable, skipping queued SQL task");
                return;
            }

            try (Connection connection = databaseManager.getConnection()) {
                task.execute(connection);
            } catch (SQLException e) {
                databaseManager.logBoundedError("SQL task failed: " + e.getMessage());
                throw new RuntimeException(e);
            }
        };
    }

    @FunctionalInterface
    public interface SqlTask {
        void execute(Connection connection) throws SQLException;
    }
}
