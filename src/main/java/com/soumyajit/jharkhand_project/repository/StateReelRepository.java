package com.soumyajit.jharkhand_project.repository;

import com.soumyajit.jharkhand_project.entity.State;
import com.soumyajit.jharkhand_project.entity.StateReel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for state_reels table.
 * Fully isolated — does not touch state_news in any way.
 */
@Repository
public interface StateReelRepository extends JpaRepository<StateReel, Long> {

    Page<StateReel> findByStateAndPublishedTrueOrderByCreatedAtDesc(State state, Pageable pageable);

    @Query("SELECT r FROM StateReel r WHERE r.state.name = :stateName AND r.published = true ORDER BY r.createdAt DESC")
    Page<StateReel> findByStateNameAndPublishedTrue(@Param("stateName") String stateName, Pageable pageable);
}
