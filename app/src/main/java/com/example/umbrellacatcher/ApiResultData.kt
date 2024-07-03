package com.example.umbrellacatcher

import java.io.Serializable
// [(TMP, 34), (SKY, 1), (PTY, 0), (POP, 0), (PCP, 강수없음)]

data class ApiResultData(
    val address: String,
    val tmp: String,
    val sky: String,
    val pty: String,
    val pop: String,
    val pcp: String
): Serializable
