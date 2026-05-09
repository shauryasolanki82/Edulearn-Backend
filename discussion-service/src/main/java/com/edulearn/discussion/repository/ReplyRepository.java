package com.edulearn.discussion.repository;

import com.edulearn.discussion.entity.Reply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReplyRepository extends JpaRepository<Reply, Long> {
    List<Reply> findByThread_ThreadIdOrderByUpvotesDescCreatedAtAsc(Long threadId);
    List<Reply> findByAuthorId(Long authorId);
    long countByThread_ThreadId(Long threadId);
}
