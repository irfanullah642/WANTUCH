package com.example.wantuch.domain.model

data class Notice(
    val id: String,
    val title: String,
    val detail: String,
    val notice_date: String,
    val expiry_date: String?,
    val creator_name: String?,
    val class_name: String?,
    val section_name: String?,
    val created_at: String
)
