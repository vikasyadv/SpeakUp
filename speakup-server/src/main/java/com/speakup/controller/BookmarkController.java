package com.speakup.controller;

import com.speakup.dto.BookmarkDto;
import com.speakup.service.BookmarkService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<BookmarkDto> bookmarkPrompt(@PathVariable Long promptId) {
        BookmarkDto bookmark = bookmarkService.bookmarkPrompt(promptId);
        return ResponseEntity.status(HttpStatus.CREATED).body(bookmark);
    }

    @DeleteMapping("/{promptId}")
    public ResponseEntity<Void> removeBookmark(@PathVariable Long promptId) {
        bookmarkService.removeBookmark(promptId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check/{promptId}")
    public ResponseEntity<Map<String, Boolean>> isBookmarked(@PathVariable Long promptId) {
        boolean bookmarked = bookmarkService.isBookmarked(promptId);
        return ResponseEntity.ok(Map.of("bookmarked", bookmarked));
    }

    @GetMapping
    public ResponseEntity<List<BookmarkDto>> getAllBookmarks() {
        return ResponseEntity.ok(bookmarkService.getAllBookmarks());
    }
}
