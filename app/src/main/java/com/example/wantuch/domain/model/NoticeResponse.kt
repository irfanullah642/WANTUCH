package com.example.wantuch.domain.model

data class NoticeResponse(
    val status: String,
    val notices: List<Notice>? = null,
    val message: String? = null
)
