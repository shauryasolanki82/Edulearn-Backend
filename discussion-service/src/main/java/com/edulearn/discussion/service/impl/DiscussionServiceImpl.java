package com.edulearn.discussion.service.impl;

import com.edulearn.discussion.dto.DiscussionDto;
import com.edulearn.discussion.entity.DiscussionThread;
import com.edulearn.discussion.entity.Reply;
import com.edulearn.discussion.exception.DiscussionNotFoundException;
import com.edulearn.discussion.exception.ForbiddenException;
import com.edulearn.discussion.repository.ReplyRepository;
import com.edulearn.discussion.repository.ThreadRepository;
import com.edulearn.discussion.service.DiscussionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j
public class DiscussionServiceImpl implements DiscussionService {

    private final ThreadRepository threadRepository;
    private final ReplyRepository  replyRepository;

    @Override @Transactional
    public DiscussionDto.ThreadResponse createThread(Long authorId, String authorName, DiscussionDto.ThreadRequest req) {
        DiscussionThread thread = DiscussionThread.builder()
                .courseId(req.getCourseId()).lessonId(req.getLessonId())
                .authorId(authorId).authorName(authorName)
                .title(req.getTitle()).body(req.getBody()).build();
        DiscussionThread saved = threadRepository.save(thread);
        log.info("Thread created: id={} course={}", saved.getThreadId(), saved.getCourseId());
        return toThreadResponse(saved);
    }

    @Override
    public DiscussionDto.ThreadWithReplies getThread(Long threadId) {
        DiscussionThread t = findThreadOrThrow(threadId);
        List<DiscussionDto.ReplyResponse> replies = replyRepository
                .findByThread_ThreadIdOrderByUpvotesDescCreatedAtAsc(threadId)
                .stream().map(this::toReplyResponse).collect(Collectors.toList());
        return new DiscussionDto.ThreadWithReplies(toThreadResponse(t), replies);
    }

    @Override
    public List<DiscussionDto.ThreadResponse> getThreadsByCourse(Long courseId) {
        return threadRepository.findByCourseIdOrderByIsPinnedDescCreatedAtDesc(courseId)
                .stream().map(this::toThreadResponse).collect(Collectors.toList());
    }

    @Override
    public List<DiscussionDto.ThreadResponse> getThreadsByLesson(Long lessonId) {
        return threadRepository.findByLessonIdOrderByCreatedAtDesc(lessonId)
                .stream().map(this::toThreadResponse).collect(Collectors.toList());
    }

    @Override
    public List<DiscussionDto.ThreadResponse> searchThreads(Long courseId, String keyword) {
        return threadRepository.searchByKeyword(courseId, keyword)
                .stream().map(this::toThreadResponse).collect(Collectors.toList());
    }

    @Override @Transactional
    public void deleteThread(Long threadId, Long requesterId, String role) {
        DiscussionThread t = findThreadOrThrow(threadId);
        if (!"ADMIN".equals(role) && !t.getAuthorId().equals(requesterId)) {
            throw new ForbiddenException("Only the author or admin can delete this thread");
        }
        threadRepository.delete(t);
        log.info("Thread deleted: id={}", threadId);
    }

    @Override @Transactional
    public DiscussionDto.ThreadResponse pinThread(Long threadId, boolean pin) {
        DiscussionThread t = findThreadOrThrow(threadId);
        t.setIsPinned(pin);
        return toThreadResponse(threadRepository.save(t));
    }

    @Override @Transactional
    public DiscussionDto.ThreadResponse closeThread(Long threadId, boolean close) {
        DiscussionThread t = findThreadOrThrow(threadId);
        t.setIsClosed(close);
        return toThreadResponse(threadRepository.save(t));
    }

    @Override @Transactional
    public DiscussionDto.ReplyResponse postReply(Long threadId, Long authorId, String authorName, DiscussionDto.ReplyRequest req) {
        DiscussionThread t = findThreadOrThrow(threadId);
        if (t.getIsClosed()) throw new ForbiddenException("This thread is closed");
        Reply reply = Reply.builder()
                .thread(t).authorId(authorId).authorName(authorName).body(req.getBody()).build();
        Reply saved = replyRepository.save(reply);
        log.info("Reply posted: threadId={} replyId={}", threadId, saved.getReplyId());
        return toReplyResponse(saved);
    }

    @Override
    public List<DiscussionDto.ReplyResponse> getRepliesByThread(Long threadId) {
        findThreadOrThrow(threadId);
        return replyRepository.findByThread_ThreadIdOrderByUpvotesDescCreatedAtAsc(threadId)
                .stream().map(this::toReplyResponse).collect(Collectors.toList());
    }

    @Override @Transactional
    public DiscussionDto.ReplyResponse upvoteReply(Long replyId) {
        Reply r = findReplyOrThrow(replyId);
        r.setUpvotes(r.getUpvotes() + 1);
        return toReplyResponse(replyRepository.save(r));
    }

    @Override @Transactional
    public DiscussionDto.ReplyResponse acceptReply(Long replyId, Long requesterId, String role) {
        Reply r = findReplyOrThrow(replyId);
        DiscussionThread t = r.getThread();
        if (!"ADMIN".equals(role) && !t.getAuthorId().equals(requesterId)) {
            throw new ForbiddenException("Only the thread author or admin can accept a reply");
        }
        r.setIsAccepted(true);
        return toReplyResponse(replyRepository.save(r));
    }

    @Override @Transactional
    public void deleteReply(Long replyId, Long requesterId, String role) {
        Reply r = findReplyOrThrow(replyId);
        if (!"ADMIN".equals(role) && !r.getAuthorId().equals(requesterId)) {
            throw new ForbiddenException("Only the author or admin can delete this reply");
        }
        replyRepository.delete(r);
    }

    private DiscussionThread findThreadOrThrow(Long id) {
        return threadRepository.findById(id).orElseThrow(() -> new DiscussionNotFoundException("Thread not found: " + id));
    }

    private Reply findReplyOrThrow(Long id) {
        return replyRepository.findById(id).orElseThrow(() -> new DiscussionNotFoundException("Reply not found: " + id));
    }

    private DiscussionDto.ThreadResponse toThreadResponse(DiscussionThread t) {
        return DiscussionDto.ThreadResponse.builder()
                .threadId(t.getThreadId()).courseId(t.getCourseId()).lessonId(t.getLessonId())
                .authorId(t.getAuthorId()).authorName(t.getAuthorName())
                .title(t.getTitle()).body(t.getBody())
                .isPinned(t.getIsPinned()).isClosed(t.getIsClosed())
                .replyCount((int) replyRepository.countByThread_ThreadId(t.getThreadId()))
                .createdAt(t.getCreatedAt()).updatedAt(t.getUpdatedAt()).build();
    }

    private DiscussionDto.ReplyResponse toReplyResponse(Reply r) {
        return DiscussionDto.ReplyResponse.builder()
                .replyId(r.getReplyId())
                .threadId(r.getThread() != null ? r.getThread().getThreadId() : null)
                .authorId(r.getAuthorId()).authorName(r.getAuthorName()).body(r.getBody())
                .isAccepted(r.getIsAccepted()).upvotes(r.getUpvotes()).createdAt(r.getCreatedAt()).build();
    }
}
