package com.resumeai.jobmatch.dto;

import jakarta.validation.constraints.NotNull;

public record BookmarkMatchRequest(@NotNull Boolean bookmarked) {
}