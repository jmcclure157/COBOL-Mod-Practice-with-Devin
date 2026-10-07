package com.carddemo.batch;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Posts each record in file order and writes the rejected ones to DALYREJS (record + validation trailer). */
@Component
@StepScope
public class PostingItemWriter implements ItemWriter<DailyTransaction> {

    static final String REJECT_COUNT = "rejectCount";

    private final TransactionPostingService posting;
    private final FlatFileItemWriter<String> rejectsWriter;
    private final StepExecution stepExecution;

    public PostingItemWriter(TransactionPostingService posting,
                             FlatFileItemWriter<String> rejectsWriter,
                             @Value("#{stepExecution}") StepExecution stepExecution) {
        this.posting = posting;
        this.rejectsWriter = rejectsWriter;
        this.stepExecution = stepExecution;
    }

    @Override
    public void write(Chunk<? extends DailyTransaction> chunk) throws Exception {
        Chunk<String> rejected = new Chunk<>();
        for (DailyTransaction daily : chunk) {
            posting.post(daily.transaction())
                    .ifPresent(reject -> rejected.add("%-350.350s".formatted(daily.record()) + reject.trailer()));
        }
        if (!rejected.isEmpty()) {
            rejectsWriter.write(rejected);
            var context = stepExecution.getExecutionContext();
            context.putLong(REJECT_COUNT, context.getLong(REJECT_COUNT, 0) + rejected.size());
        }
    }
}
