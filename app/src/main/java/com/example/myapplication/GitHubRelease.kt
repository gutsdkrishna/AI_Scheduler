package com.example.myapplication

data class GitHubRelease(
        val tag_name: String,
        val name: String,
        val body: String,
        val html_url: String
)
