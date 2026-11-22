package com.example.planmanagerapp.data.repository

/** 토글이 409(VERSION_CONFLICT)로 거절되어 로컬을 서버의 최신 상태로 맞췄음을 알린다. */
class VersionConflictException : Exception()

/** 큐에 쌓인 요청을 다시 보낸 결과 */
enum class ResendResult {
    DONE,  // 반영됨: 큐에서 지움
    STALE, // 409·404: 다시 보내도 같으므로 큐에서 지우고 최신 목록을 다시 받아야 함
    RETRY  // 5xx 등: 큐에 남겨 다음에 다시 보냄
}