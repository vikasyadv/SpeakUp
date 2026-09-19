package com.speakup.mapper;

import com.speakup.dto.BookmarkDto;
import com.speakup.model.Bookmark;

public class BookmarkMapper {

    private BookmarkMapper() {
        // Utility class
    }

    public static BookmarkDto toDto(Bookmark bookmark) {
        return new BookmarkDto(
                bookmark.getId(),
                PromptMapper.toDto(bookmark.getPrompt()),
                bookmark.getCreatedAt()
        );
    }
}
