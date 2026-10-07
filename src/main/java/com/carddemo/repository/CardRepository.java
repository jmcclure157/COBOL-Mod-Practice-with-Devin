package com.carddemo.repository;

import com.carddemo.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, String> {
}
