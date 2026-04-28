package com.onboardguard.officer.repository;

import com.onboardguard.officer.entity.CaseNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CaseNoteRepository extends JpaRepository<CaseNote, Long> {
}
