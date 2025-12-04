package com.polifeed.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor // 기본 생성자 자동 생성
@AllArgsConstructor // 모든 필드를 넣는 생성자 자동 생성 (MainController에서 씀)
public class FeedbackDTO {
    private Long id;
    private String userId;
    private String topic;
    private String originalText;
    private String feedbackText;
    private LocalDateTime createdAt;
}