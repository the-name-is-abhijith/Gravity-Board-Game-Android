package com.voiezstudio.gravity.model

import java.io.Serializable

class Table(
    var time: Long = 0,
    var player1: String = "",
    var hostactive: Boolean = false,
    var player2: String = "",
    var guestactive: Boolean = false,
    var buttons: MutableList<MutableList<String>> = mutableListOf(
        mutableListOf("", "", "", "", "", "", ""),
        mutableListOf("", "", "", "", "", "", ""),
        mutableListOf("", "", "", "", "", "", ""),
        mutableListOf("", "", "", "", "", "", ""),
        mutableListOf("", "", "", "", "", "", ""),
        mutableListOf("", "", "", "", "", "", ""),
        mutableListOf("", "", "", "", "", "", "")
    ),
    var winner: String = "",
    var turn: String = listOf("host", "guest").random()

) : Serializable