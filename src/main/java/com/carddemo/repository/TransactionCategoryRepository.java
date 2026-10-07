package com.carddemo.repository;

import com.carddemo.model.TransactionCategory;
import com.carddemo.model.TransactionCategoryId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionCategoryRepository extends JpaRepository<TransactionCategory, TransactionCategoryId> {
}
