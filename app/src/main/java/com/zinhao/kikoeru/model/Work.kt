package com.zinhao.kikoeru.model

data class Work(
    val circle: Circle,
    val circle_id: Int,
    val dl_count: Int,
    val id: Int,
    val name: String,
    val nsfw: Boolean,
    val price: Int,
    val rank: List<Rank>,
    val rate_average_2dp: Double,
    val rate_count: Int,
    val rate_count_detail: List<RateCountDetail>,
    val release: String,
    val review_count: Int,
    val tags: List<Tag>,
    val title: String,
    val userRating: Any,
    val vas: List<Va>
)