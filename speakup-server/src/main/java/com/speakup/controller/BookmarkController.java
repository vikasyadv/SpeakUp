package com.speakup.controller;

import com.speakup.dto.BookmarkDto;
import com.speakup.security.CallerContext;
import com.speakup.security.UserPrincipal;
import com.speakup.service.BookmarkService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/bookmarks")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @PostMapping("/{promptId}")
    public ResponseEntity<BookmarkDto> bookmarkPrompt(
            @PathVariable Long promptId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId) {
        CallerContext caller = CallerContext.resolve(principal, guestId);
        BookmarkDto bookmark = bookmarkService.bookmarkPrompt(promptId, caller);
        return ResponseEntity.status(HttpStatus.CREATED).body(bookmark);
    }

    @DeleteMapping("/{promptId}")
    public ResponseEntity<Void> removeBookmark(
            @PathVariable Long promptId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId) {
        CallerContext caller = CallerContext.resolve(principal, guestId);
        bookmarkService.removeBookmark(promptId, caller);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check/{promptId}")
    public ResponseEntity<Map<String, Boolean>> isBookmarked(
            @PathVariable Long promptId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId) {
        CallerContext caller = CallerContext.resolve(principal, guestId);
        boolean bookmarked = bookmarkService.isBookmarked(promptId, caller);
        return ResponseEntity.ok(Map.of("bookmarked", bookmarked));
    }

    @GetMapping
    public ResponseEntity<List<BookmarkDto>> getAllBookmarks(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId) {
        CallerContext caller = CallerContext.resolve(principal, guestId);
        return ResponseEntity.ok(bookmarkService.getAllBookmarks(caller));
    }
}
