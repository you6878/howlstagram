package com.company.howl.howlstagram.model

data class AlarmDTO (
        var destinationUid: String? = null,
        var userId: String? = null,
        var uid: String? = null,
        var kind: Int = 0, //0 : 좋아요, 1: 메세지, 2: 팔로우
        var message: String? = null,
        var timestamp: Long? = null
)