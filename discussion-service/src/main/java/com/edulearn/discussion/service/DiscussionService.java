package com.edulearn.discussion.service;

import com.edulearn.discussion.dto.DiscussionDto;
import java.util.List;

public interface DiscussionService {
    DiscussionDto.ThreadResponse   createThread(Long authorId, String authorName, DiscussionDto.ThreadRequest req);
    DiscussionDto.ThreadWithReplies getThread(Long threadId);
    List<DiscussionDto.ThreadResponse> getThreadsByCourse(Long courseId);
    List<DiscussionDto.ThreadResponse> getThreadsByLesson(Long lessonId);
    List<DiscussionDto.ThreadResponse> searchThreads(Long courseId, String keyword);
    void deleteThread(Long threadId, Long requesterId, String role);
    DiscussionDto.ThreadResponse   pinThread(Long threadId, boolean pin);
    DiscussionDto.ThreadResponse   closeThread(Long threadId, boolean close);
    DiscussionDto.ReplyResponse    postReply(Long threadId, Long authorId, String authorName, DiscussionDto.ReplyRequest req);
    List<DiscussionDto.ReplyResponse> getRepliesByThread(Long threadId);
    DiscussionDto.ReplyResponse    upvoteReply(Long replyId);
    DiscussionDto.ReplyResponse    acceptReply(Long replyId, Long requesterId, String role);
    void deleteReply(Long replyId, Long requesterId, String role);
}
