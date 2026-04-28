package com.onboardguard.watchlist.dto;

import com.onboardguard.shared.common.enums.CategoryCode;
import lombok.Data;

@Data
public class WatchlistCategoryDto {
    private CategoryCode categoryCode;
    private String categoryName;
    private String description;
    private Double baseScoreMultiplier;
    private Integer displayOrder;
}