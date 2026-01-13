package com.teamloci.loci.domain.feedback;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    // List<Feedback> findAllByStatus(FeedbackStatus status);
}