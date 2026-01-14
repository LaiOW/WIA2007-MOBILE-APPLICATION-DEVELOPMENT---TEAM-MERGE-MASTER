package com.example.myapplication

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val id: Long? = null,
    val userName: String? = "",
    val title: String? = null,
    val content: String? = "",
    val category: String? = "General",
    var imageUri: String? = null,
    @SerialName("user_profile_image")
    var userProfileImage: String? = null,
    var likeCount: Int? = 0,
    var isLiked: Boolean? = false
) {
    // Default constructor
    constructor() : this(null, "", null, "", "General", null, null, 0, false)

    // Secondary constructor for content and category only
    constructor(userName: String?, content: String?, category: String?) :
            this(null, userName, null, content, category, null, null, 0, false)

    // Secondary constructor for title, content and category
    constructor(userName: String?, title: String?, content: String?, category: String?) :
            this(null, userName, title, content, category, null, null, 0, false)
}
