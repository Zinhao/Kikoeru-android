package com.zinhao.kikoeru.model

data class Pagination(
    val currentPage: Int,
    val pageSize: Int,
    val totalCount: Int
)