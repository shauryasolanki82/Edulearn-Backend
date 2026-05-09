package com.edulearn.discussion.controller;

import com.edulearn.discussion.dto.DiscussionDto;
import com.edulearn.discussion.service.DiscussionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Discussion Forum", description = "Course discussion threads, replies, upvoting, and moderation")
public class DiscussionResource {

    private final DiscussionService discussionService;

    // ── Thread endpoints ──────────────────────────────────────────────────────

    @PostMapping("/threads")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a new discussion thread")
    public ResponseEntity<DiscussionDto.ThreadResponse> createThread(
            @Valid @RequestBody DiscussionDto.ThreadRequest request, HttpServletRequest req) {
        Long authorId   = (Long)   req.getAttribute("userId");
        String authorName = (String) req.getAttribute("userName");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(discussionService.createThread(authorId, authorName, request));
    }

    @GetMapping("/threads/{threadId}")
    @Operation(summary = "Get a thread with all its replies (public)")
    public ResponseEntity<DiscussionDto.ThreadWithReplies> getThread(@PathVariable Long threadId) {
        return ResponseEntity.ok(discussionService.getThread(threadId));
    }

    @GetMapping("/threads/course/{courseId}")
    @Operation(summary = "Get all threads for a course, pinned first (public)")
    public ResponseEntity<List<DiscussionDto.ThreadResponse>> getThreadsByCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(discussionService.getThreadsByCourse(courseId));
    }

    @GetMapping("/threads/lesson/{lessonId}")
    @Operation(summary = "Get all threads for a specific lesson (public)")
    public ResponseEntity<List<DiscussionDto.ThreadResponse>> getThreadsByLesson(@PathVariable Long lessonId) {
        return ResponseEntity.ok(discussionService.getThreadsByLesson(lessonId));
    }

    @GetMapping("/threads/course/{courseId}/search")
    @Operation(summary = "Search threads in a course by keyword (public)")
    public ResponseEntity<List<DiscussionDto.ThreadResponse>> searchThreads(
            @PathVariable Long courseId, @RequestParam String keyword) {
        return ResponseEntity.ok(discussionService.searchThreads(courseId, keyword));
    }

    @DeleteMapping("/threads/{threadId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete a thread (author or admin only)")
    public ResponseEntity<DiscussionDto.ApiResponse> deleteThread(
            @PathVariable Long threadId, HttpServletRequest req) {
        Long userId = (Long)   req.getAttribute("userId");
        String role = (String) req.getAttribute("role");
        discussionService.deleteThread(threadId, userId, role);
        return ResponseEntity.ok(DiscussionDto.ApiResponse.builder()
                .success(true).message("Thread deleted").build());
    }

    @PutMapping("/threads/{threadId}/pin")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Pin or unpin a thread (Instructor/Admin)")
    public ResponseEntity<DiscussionDto.ThreadResponse> pinThread(
            @PathVariable Long threadId, @RequestParam boolean pin) {
        return ResponseEntity.ok(discussionService.pinThread(threadId, pin));
    }

    @PutMapping("/threads/{threadId}/close")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Close or reopen a thread (Instructor/Admin)")
    public ResponseEntity<DiscussionDto.ThreadResponse> closeThread(
            @PathVariable Long threadId, @RequestParam boolean close) {
        return ResponseEntity.ok(discussionService.closeThread(threadId, close));
    }

    // ── Reply endpoints ───────────────────────────────────────────────────────

    @PostMapping("/threads/{threadId}/replies")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Post a reply to a thread")
    public ResponseEntity<DiscussionDto.ReplyResponse> postReply(
            @PathVariable Long threadId,
            @Valid @RequestBody DiscussionDto.ReplyRequest request,
            HttpServletRequest req) {
        Long authorId    = (Long)   req.getAttribute("userId");
        String authorName = (String) req.getAttribute("userName");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(discussionService.postReply(threadId, authorId, authorName, request));
    }

    @GetMapping("/threads/{threadId}/replies")
    @Operation(summary = "Get all replies for a thread, sorted by upvotes (public)")
    public ResponseEntity<List<DiscussionDto.ReplyResponse>> getReplies(@PathVariable Long threadId) {
        return ResponseEntity.ok(discussionService.getRepliesByThread(threadId));
    }

    @PostMapping("/replies/{replyId}/upvote")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upvote a reply")
    public ResponseEntity<DiscussionDto.ReplyResponse> upvoteReply(@PathVariable Long replyId) {
        return ResponseEntity.ok(discussionService.upvoteReply(replyId));
    }

    @PutMapping("/replies/{replyId}/accept")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Accept a reply as the best answer (thread author or admin)")
    public ResponseEntity<DiscussionDto.ReplyResponse> acceptReply(
            @PathVariable Long replyId, HttpServletRequest req) {
        Long userId = (Long)   req.getAttribute("userId");
        String role = (String) req.getAttribute("role");
        return ResponseEntity.ok(discussionService.acceptReply(replyId, userId, role));
    }

    @DeleteMapping("/replies/{replyId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete a reply (author or admin only)")
    public ResponseEntity<DiscussionDto.ApiResponse> deleteReply(
            @PathVariable Long replyId, HttpServletRequest req) {
        Long userId = (Long)   req.getAttribute("userId");
        String role = (String) req.getAttribute("role");
        discussionService.deleteReply(replyId, userId, role);
        return ResponseEntity.ok(DiscussionDto.ApiResponse.builder()
                .success(true).message("Reply deleted").build());
    }
}
