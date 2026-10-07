package com.carddemo.batch;

import com.carddemo.repository.TransactionRepository;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Runs POSTTRAN once after the sample files are loaded, so TRANSACT holds what the mainframe job would post. */
@Component
@Order(2)
@ConditionalOnProperty(name = {"carddemo.seed.enabled", "carddemo.posting.run-at-startup"},
        havingValue = "true", matchIfMissing = true)
public class PostTransactionsAtStartup implements ApplicationRunner {

    private final JobLauncher jobLauncher;
    private final Job postTransactionsJob;
    private final TransactionRepository transactions;
    private final String dailyFile;
    private final String rejectsFile;

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
    public void run(ApplicationArguments args) throws Exception {
        if (transactions.count() > 0) {
            return;
        }
        JobExecution execution = jobLauncher.run(postTransactionsJob, new JobParametersBuilder()
                .addString("dailyFile", dailyFile)
                .addString("rejectsFile", rejectsFile)
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters());
        if (execution.getStatus() != BatchStatus.COMPLETED) {
            throw new IllegalStateException("POSTTRAN abended: " + execution.getExitStatus().getExitDescription());
        }
    }
}
