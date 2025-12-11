package com.polifeed.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackDTO {
    private Long id;
    private String userId;
    private String topic;
    private String originalText;
    private String feedbackText;
    private LocalDateTime createdAt;
    private String persona;

    // [추가됨] 5가지 분석 점수 (기본값 0)
    private int scoreLogic;      // 논리력
    private int scoreJobFit;     // 직무적합성
    private int scoreSincerity;  // 성실성
    private int scoreCreativity; // 창의성
    private int scoreReadability;// 가독성
}