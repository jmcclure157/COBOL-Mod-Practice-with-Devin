package com.carddemo.batch;

import com.carddemo.repository.CardXrefRepository;
import com.carddemo.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs the whole job on small daily files. Own in-memory database, because the job commits its chunks. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:posting-job-test;DB_CLOSE_DELAY=-1")
@DirtiesContext
class PostTransactionsJobTest {

    static final String RECEIVED = "2022-06-10 19:27:53.000000";

    @Autowired JobLauncher jobLauncher;
    @Autowired Job postTransactionsJob;
    @Autowired TransactionRepository transactions;
    @Autowired CardXrefRepository cardXrefs;

    @TempDir Path dir;

    String accountOneCard() {
        return cardXrefs.findFirstByAccountIdOrderByCardNumberAsc(1L).orElseThrow().getCardNumber();
    }

    JobExecution run(String... lines) throws Exception {
        Path daily = Files.write(dir.resolve("dailytran.txt"), List.of(lines));
        return jobLauncher.run(postTransactionsJob, new JobParametersBuilder()
                .addString("dailyFile", daily.toUri().toString())
                .addString("rejectsFile", dir.resolve("dalyrejs.txt").toString())
                .addString("run", UUID.randomUUID().toString())
                .toJobParameters());
    }

    @Test
    void postsGoodRecordsAndWritesRejectsWithTheirReason() throws Exception {
        String good = DailyRecords.line("0000000999999101", accountOneCard(), 1000, RECEIVED);
        String badCard = DailyRecords.line("0000000999999102", "0000000000000000", 1000, RECEIVED);

        JobExecution execution = run(good, badCard, "");

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getExitStatus().getExitCode()).isEqualTo(PostTransactionsJobConfig.COMPLETED_WITH_REJECTS);
        var step = execution.getStepExecutions().iterator().next();
        assertThat(step.getReadCount() - step.getFilterCount()).isEqualTo(2);

        assertThat(transactions.findById("0000000999999101")).isPresent();
        assertThat(transactions.findById("0000000999999102")).isEmpty();

        List<String> rejects = Files.readAllLines(dir.resolve("dalyrejs.txt"));
        assertThat(rejects).hasSize(1);
        assertThat(rejects.get(0)).hasSize(430);
        assertThat(rejects.get(0).substring(0, 350)).isEqualTo(badCard);
        assertThat(rejects.get(0).substring(350).stripTrailing()).isEqualTo("0100INVALID CARD NUMBER FOUND");
    }

    @Test
    void noRejectsEndsAsPlainCompleted() throws Exception {
        JobExecution execution = run(DailyRecords.line("0000000999999201", accountOneCard(), 1000, RECEIVED));

        assertThat(execution.getExitStatus().getExitCode()).isEqualTo(ExitStatus.COMPLETED.getExitCode());
        assertThat(Files.readAllLines(dir.resolve("dalyrejs.txt"))).isEmpty();
    }

    @Test
    void duplicateTransactionIdFailsTheJobAndRollsBackItsChunk() throws Exception {
        String record = DailyRecords.line("0000000999999301", accountOneCard(), 1000, RECEIVED);

        JobExecution execution = run(record, record);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(execution.getAllFailureExceptions())
                .anySatisfy(e -> assertThat(e).hasMessageContaining("ERROR WRITING TO TRANSACTION FILE"));
        assertThat(transactions.findById("0000000999999301")).isEmpty();
    }
}
