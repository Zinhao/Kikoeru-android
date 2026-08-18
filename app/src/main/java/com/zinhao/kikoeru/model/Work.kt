package com.zinhao.kikoeru.model

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.json.JSONObject

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
    val vas: List<Va>,
    var host: String?
)
val gson = Gson()
fun JSONObject.toWork():Work{
    return gson.fromJson(this.toString(), Work::class.java)
}