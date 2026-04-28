package com.onboardguard.officer.entity;

import com.onboardguard.shared.common.enums.NoteType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "case_notes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class CaseNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case investigationCase;

    @Column(name = "author_id", nullable = false, updatable = false)
    private Long authorId;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false, updatable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "note_type", nullable = false, updatable = false)
    private NoteType noteType;

    // Strictly Append-Only: Notice there is NO @LastModifiedDate or is_edited flag!
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

//    public enum NoteType {
//        INVESTIGATION,     // General officer thoughts
//        EVIDENCE_LINK,     // URL or reference to a document
//        ESCALATION_MEMO,   // The specific note written when sending to L2
//        SYSTEM_ACTION      // Automated notes (e.g., "SLA breached at 14:00")
//    }
}