package com.carddemo.batch;

import com.carddemo.repository.TransactionRepository;
import com.carddemo.seed.CardDemoDataLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * Runs POSTTRAN once after the sample files are loaded, so TRANSACT holds what the mainframe job would post.
 * Like the mainframe batch window, this happens before the online side is up: lifecycle phases start in order,
 * and the web server only starts after this phase, so no request can see or change data mid-posting.
 */
@Component
@ConditionalOnProperty(name = {"carddemo.seed.enabled", "carddemo.posting.run-at-startup"},
        havingValue = "true", matchIfMissing = true)
public class PostTransactionsAtStartup implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(PostTransactionsAtStartup.class);

    private final JobLauncher jobLauncher;
    private final Job postTransactionsJob;
    private final TransactionRepository transactions;
    private final String dailyFile;
    private final String rejectsFile;
    private volatile boolean running;

    public PostTransactionsAtStartup(JobLauncher jobLauncher,
                                     Job postTransactionsJob,
                                     TransactionRepository transactions,
                                     @Value("${carddemo.seed.location:classpath:carddemo-data/}dailytran.txt") String dailyFile,
                                     @Value("${carddemo.posting.rejects-file:target/dalyrejs.txt}") String rejectsFile) {
        this.jobLauncher = jobLauncher;
        this.postTransactionsJob = postTransactionsJob;
        this.transactions = transactions;
        this.dailyFile = dailyFile;
        this.rejectsFile = rejectsFile;
    }

    @Override
    public int getPhase() {
        return CardDemoDataLoader.PHASE + 1;
    }

    /**
     * Skipped when TRANSACT already has rows. Because the web server is not up yet, only an earlier posting run can
     * have written them; the database is rebuilt on each start (create-drop), so no half-posted state survives.
     */
    @Override
    public void start() {
        if (transactions.count() > 0) {
            log.info("TRANSACT already holds posted transactions, skipping POSTTRAN");
            running = true;
            return;
        }
        try {
            JobExecution execution = jobLauncher.run(postTransactionsJob, new JobParametersBuilder()
                    .addString("dailyFile", dailyFile)
                    .addString("rejectsFile", rejectsFile)
                    .addLong("run.id", System.currentTimeMillis())
                    .toJobParameters());
            if (execution.getStatus() != BatchStatus.COMPLETED) {
                throw new IllegalStateException("POSTTRAN abended: " + execution.getExitStatus().getExitDescription());
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("POSTTRAN could not be started", e);
        }
        running = true;
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
