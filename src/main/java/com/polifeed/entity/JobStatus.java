package com.polifeed.entity;

public enum JobStatus {
    PREPARE,     // 작성중
    APPLIED,     // 지원 완료
    DOC_PASS,    // 서류 합격
    INTERVIEW,   // 면접 진행
    FINAL_PASS,  // 최종 합격
    REJECTED     // 최종 결과
}