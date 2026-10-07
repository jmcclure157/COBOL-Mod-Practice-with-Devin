package com.carddemo.batch;

import com.carddemo.seed.CardDemoDataLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.mapping.PassThroughLineMapper;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * POSTTRAN.jcl / CBTRN02C as a Spring Batch job. Job parameters replace the JCL DD statements:
 * {@code dailyFile} = DALYTRAN (input), {@code rejectsFile} = DALYREJS (new output file).
 */
@Configuration
public class PostTransactionsJobConfig {

    public static final String JOB_NAME = "postTransactionsJob";
    /** CBTRN02C sets RETURN-CODE 4 when any record was rejected. */
    public static final String COMPLETED_WITH_REJECTS = "COMPLETED WITH REJECTS";

    private static final Logger log = LoggerFactory.getLogger(PostTransactionsJobConfig.class);

    @Bean
    public Job postTransactionsJob(JobRepository jobRepository, Step postTransactionsStep) {
        return new JobBuilder(JOB_NAME, jobRepository).start(postTransactionsStep).build();
    }

    @Bean
    public Step postTransactionsStep(JobRepository jobRepository,
                                     PlatformTransactionManager transactionManager,
                                     FlatFileItemReader<String> dailyTransactionReader,
                                     PostingItemWriter postingItemWriter,
                                     FlatFileItemWriter<String> rejectsWriter) {
        return new StepBuilder("postTransactionsStep", jobRepository)
                .<String, DailyTransaction>chunk(100, transactionManager)
                .reader(dailyTransactionReader)
                .processor(line -> line.isBlank()
                        ? null
                        : new DailyTransaction(line, CardDemoDataLoader.parseTransaction(line)))
                .writer(postingItemWriter)
                .stream(rejectsWriter)
                .listener(countsListener())
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<String> dailyTransactionReader(
            @Value("#{jobParameters['dailyFile']}") String dailyFile, ResourceLoader resourceLoader) {
        return new FlatFileItemReaderBuilder<String>()
                .name("dalytran")
                .resource(resourceLoader.getResource(dailyFile))
                .encoding("US-ASCII")
                .lineMapper(new PassThroughLineMapper())
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<String> rejectsWriter(@Value("#{jobParameters['rejectsFile']}") String rejectsFile) {
        return new FlatFileItemWriterBuilder<String>()
                .name("dalyrejs")
                .resource(new FileSystemResource(rejectsFile))
                .encoding("US-ASCII")
                .lineAggregator(new PassThroughLineAggregator<>())
                .build();
    }

    /** The COBOL's closing DISPLAYs and return code. */
    private static StepExecutionListener countsListener() {
        return new StepExecutionListener() {
            @Override
            public ExitStatus afterStep(StepExecution step) {
                if (!ExitStatus.COMPLETED.getExitCode().equals(step.getExitStatus().getExitCode())) {
                    return null;
                }
                long rejected = step.getExecutionContext().getLong(PostingItemWriter.REJECT_COUNT, 0);
                log.info("TRANSACTIONS PROCESSED :{}", step.getReadCount() - step.getFilterCount());
                log.info("TRANSACTIONS REJECTED  :{}", rejected);
                return rejected > 0 ? new ExitStatus(COMPLETED_WITH_REJECTS) : null;
            }
        };
    }
}
