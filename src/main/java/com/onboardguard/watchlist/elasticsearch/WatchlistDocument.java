package com.onboardguard.watchlist.elasticsearch;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import java.util.List;

@Data
@Builder
@Document(indexName = "watchlist_entries")
public class WatchlistDocument {

    @Id
    private String id; // Matches PostgreSQL ID

    @Field(type = FieldType.Text, analyzer = "standard")
    private String primaryName;

    @Field(type = FieldType.Keyword)
    private List<String> aliases;

    @Field(type = FieldType.Keyword)
    private String categoryCode;

    @Field(type = FieldType.Keyword)
    private String severity;

    @Field(type = FieldType.Text)
    private String organizationName;

    @Field(type = FieldType.Boolean)
    private Boolean isActive;
}