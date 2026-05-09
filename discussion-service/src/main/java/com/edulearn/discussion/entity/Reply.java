package com.edulearn.discussion.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "replies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Reply {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long replyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    private DiscussionThread thread;

    @Column(nullable = false) private Long authorId;
    @Column                   private String authorName;
    @Column(nullable = false, columnDefinition = "TEXT") private String body;
    @Column(nullable = false) @Builder.Default private Boolean isAccepted = false;
    @Column(nullable = false) @Builder.Default private Integer upvotes    = 0;

    @CreationTimestamp @Column(updatable = false) private LocalDateTime createdAt;
}
