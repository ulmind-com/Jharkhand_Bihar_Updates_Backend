package com.soumyajit.jharkhand_project.repository;

import com.soumyajit.jharkhand_project.entity.NewsCategory;
import com.soumyajit.jharkhand_project.entity.State;
import com.soumyajit.jharkhand_project.entity.StateNews;
import com.soumyajit.jharkhand_project.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StateNewsRepository extends JpaRepository<StateNews, Long> {
    Page<StateNews> findByStateAndPublishedTrueOrderByCreatedAtDesc(State state, Pageable pageable);


    List<StateNews> findByAuthorOrderByCreatedAtDesc(User author);
    Page<StateNews> findByAuthorOrderByCreatedAtDesc(User author, Pageable pageable);

    @Query("SELECT sn FROM StateNews sn WHERE sn.state.name = :stateName AND sn.published = true ORDER BY sn.createdAt DESC")
    List<StateNews> findByStateNameAndPublishedTrue(@Param("stateName") String stateName);

    List<StateNews> findByStateAndPublishedTrueAndCreatedAtAfterOrderByCreatedAtDesc(
                                                                                       State state,
                                                                                       LocalDateTime after
    );

    long countByAuthor(User author);


    // Future: Get news by state and category
    List<StateNews> findByStateAndCategoryAndPublishedTrueOrderByCreatedAtDesc(
            State state, NewsCategory category);

    // Future: Get recent news by state and category
    List<StateNews> findByStateAndCategoryAndPublishedTrueAndCreatedAtAfterOrderByCreatedAtDesc(
            State state, NewsCategory category, LocalDateTime after);

    // Future: Get all news by category (across all states)
    List<StateNews> findByCategoryAndPublishedTrueOrderByCreatedAtDesc(NewsCategory category);

}
