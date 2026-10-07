package com.carddemo.repository;

import com.carddemo.model.DisclosureGroup;
import com.carddemo.model.DisclosureGroupId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisclosureGroupRepository extends JpaRepository<DisclosureGroup, DisclosureGroupId> {
}
