package com.onboardguard.watchlist.repository;

import com.onboardguard.shared.common.enums.CategoryCode;
import com.onboardguard.watchlist.entity.WatchlistCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchlistCategoryRepository extends JpaRepository<WatchlistCategory , Long> {

    /**
     * Used by AdminWatchlistServiceImpl when creating or updating a watchlist entry.
     * Replaces the previous findAll().stream().filter() pattern which loaded every
     * category into memory just to find one.
     */
    Optional<WatchlistCategory> findByCode(CategoryCode code);

    /**
     * Used by WatchlistServiceImpl.getActiveCategories().
     * Filter pushed to DB instead of Java stream.
     */
    List<WatchlistCategory> findByIsActiveTrue();
}
