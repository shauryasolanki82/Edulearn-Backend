package com.edulearn.discussion.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "discussion_threads")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DiscussionThread {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long threadId;

    @Column(nullable = false) private Long courseId;
    @Column                   private Long lessonId;  // optional
    @Column(nullable = false) private Long authorId;
    @Column                   private String authorName;

    @Column(nullable = false) private String title;
    @Column(nullable = false, columnDefinition = "TEXT") private String body;

    @Column(nullable = false) @Builder.Default private Boolean isPinned  = false;
    @Column(nullable = false) @Builder.Default private Boolean isClosed  = false;

    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Reply> replies = new ArrayList<>();

    @CreationTimestamp @Column(updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp private LocalDateTime updatedAt;
}
