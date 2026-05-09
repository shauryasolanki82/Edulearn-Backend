package com.edulearn.discussion.service;

import com.edulearn.discussion.dto.DiscussionDto;
import com.edulearn.discussion.entity.DiscussionThread;
import com.edulearn.discussion.entity.Reply;
import com.edulearn.discussion.exception.DiscussionNotFoundException;
import com.edulearn.discussion.exception.ForbiddenException;
import com.edulearn.discussion.repository.ReplyRepository;
import com.edulearn.discussion.repository.ThreadRepository;
import com.edulearn.discussion.service.impl.DiscussionServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscussionServiceImplTest {

    @Mock ThreadRepository threadRepository;
    @Mock ReplyRepository  replyRepository;
    @InjectMocks DiscussionServiceImpl service;

    private DiscussionThread sampleThread;
    private Reply sampleReply;

    @BeforeEach
    void setUp() {
        sampleThread = DiscussionThread.builder()
                .threadId(1L).courseId(10L).authorId(5L).authorName("Alice")
                .title("How does Spring Boot work?").body("Please explain Spring Boot.")
                .isPinned(false).isClosed(false).replies(new ArrayList<>()).build();

        sampleReply = Reply.builder()
                .replyId(1L).thread(sampleThread).authorId(6L).authorName("Bob")
                .body("Spring Boot auto-configures your application.").upvotes(0).isAccepted(false).build();
    }

    @Test @DisplayName("createThread — persists and returns thread response")
    void createThread_success() {
        when(threadRepository.save(any())).thenReturn(sampleThread);
        when(replyRepository.countByThread_ThreadId(1L)).thenReturn(0L);

        DiscussionDto.ThreadRequest req = new DiscussionDto.ThreadRequest(
                10L, null, "How does Spring Boot work?", "Please explain Spring Boot.");
        DiscussionDto.ThreadResponse resp = service.createThread(5L, "Alice", req);

        assertThat(resp.getTitle()).isEqualTo("How does Spring Boot work?");
        assertThat(resp.getAuthorId()).isEqualTo(5L);
        assertThat(resp.getIsPinned()).isFalse();
    }

    @Test @DisplayName("getThread — returns thread with replies")
    void getThread_success() {
        when(threadRepository.findById(1L)).thenReturn(Optional.of(sampleThread));
        when(replyRepository.findByThread_ThreadIdOrderByUpvotesDescCreatedAtAsc(1L))
                .thenReturn(List.of(sampleReply));
        when(replyRepository.countByThread_ThreadId(1L)).thenReturn(1L);

        DiscussionDto.ThreadWithReplies result = service.getThread(1L);
        assertThat(result.getThread().getTitle()).isEqualTo("How does Spring Boot work?");
        assertThat(result.getReplies()).hasSize(1);
        assertThat(result.getReplies().get(0).getAuthorName()).isEqualTo("Bob");
    }

    @Test @DisplayName("getThread — not found throws DiscussionNotFoundException")
    void getThread_notFound() {
        when(threadRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getThread(99L))
                .isInstanceOf(DiscussionNotFoundException.class);
    }

    @Test @DisplayName("deleteThread — author can delete own thread")
    void deleteThread_authorSuccess() {
        when(threadRepository.findById(1L)).thenReturn(Optional.of(sampleThread));
        service.deleteThread(1L, 5L, "STUDENT");
        verify(threadRepository).delete(sampleThread);
    }

    @Test @DisplayName("deleteThread — non-author throws ForbiddenException")
    void deleteThread_forbidden() {
        when(threadRepository.findById(1L)).thenReturn(Optional.of(sampleThread));
        assertThatThrownBy(() -> service.deleteThread(1L, 99L, "STUDENT"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test @DisplayName("deleteThread — admin can delete any thread")
    void deleteThread_adminSuccess() {
        when(threadRepository.findById(1L)).thenReturn(Optional.of(sampleThread));
        service.deleteThread(1L, 999L, "ADMIN");
        verify(threadRepository).delete(sampleThread);
    }

    @Test @DisplayName("pinThread — sets isPinned=true")
    void pinThread_success() {
        when(threadRepository.findById(1L)).thenReturn(Optional.of(sampleThread));
        when(threadRepository.save(any())).thenReturn(sampleThread);
        when(replyRepository.countByThread_ThreadId(1L)).thenReturn(0L);
        service.pinThread(1L, true);
        assertThat(sampleThread.getIsPinned()).isTrue();
    }

    @Test @DisplayName("closeThread — sets isClosed=true, blocks replies")
    void closeThread_success() {
        when(threadRepository.findById(1L)).thenReturn(Optional.of(sampleThread));
        when(threadRepository.save(any())).thenReturn(sampleThread);
        when(replyRepository.countByThread_ThreadId(1L)).thenReturn(0L);
        service.closeThread(1L, true);
        assertThat(sampleThread.getIsClosed()).isTrue();
    }

    @Test @DisplayName("postReply — creates reply on open thread")
    void postReply_success() {
        when(threadRepository.findById(1L)).thenReturn(Optional.of(sampleThread));
        when(replyRepository.save(any())).thenReturn(sampleReply);

        DiscussionDto.ReplyResponse resp = service.postReply(1L, 6L, "Bob",
                new DiscussionDto.ReplyRequest("Spring Boot auto-configures your application."));
        assertThat(resp.getBody()).isEqualTo("Spring Boot auto-configures your application.");
        assertThat(resp.getUpvotes()).isEqualTo(0);
    }

    @Test @DisplayName("postReply — throws ForbiddenException on closed thread")
    void postReply_closedThread() {
        sampleThread.setIsClosed(true);
        when(threadRepository.findById(1L)).thenReturn(Optional.of(sampleThread));
        assertThatThrownBy(() -> service.postReply(1L, 6L, "Bob",
                new DiscussionDto.ReplyRequest("Some reply")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test @DisplayName("upvoteReply — increments upvotes by 1")
    void upvoteReply_success() {
        when(replyRepository.findById(1L)).thenReturn(Optional.of(sampleReply));
        when(replyRepository.save(any())).thenReturn(sampleReply);
        service.upvoteReply(1L);
        assertThat(sampleReply.getUpvotes()).isEqualTo(1);
    }

    @Test @DisplayName("acceptReply — thread author can accept a reply")
    void acceptReply_authorSuccess() {
        when(replyRepository.findById(1L)).thenReturn(Optional.of(sampleReply));
        when(replyRepository.save(any())).thenReturn(sampleReply);
        service.acceptReply(1L, 5L, "STUDENT"); // authorId=5 matches thread.authorId
        assertThat(sampleReply.getIsAccepted()).isTrue();
    }

    @Test @DisplayName("acceptReply — non-author throws ForbiddenException")
    void acceptReply_forbidden() {
        when(replyRepository.findById(1L)).thenReturn(Optional.of(sampleReply));
        assertThatThrownBy(() -> service.acceptReply(1L, 99L, "STUDENT"))
                .isInstanceOf(ForbiddenException.class);
    }
}
