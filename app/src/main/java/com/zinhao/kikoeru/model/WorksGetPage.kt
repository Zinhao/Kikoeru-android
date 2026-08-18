package com.zinhao.kikoeru.model

data class WorksGetPage(
    val pagination: Pagination,
    val works: List<Work>
)