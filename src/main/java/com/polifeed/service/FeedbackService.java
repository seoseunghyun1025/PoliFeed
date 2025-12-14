package com.polifeed.service;

import com.polifeed.dto.FeedbackDTO;
import com.polifeed.entity.JobStatus;
import com.polifeed.mapper.FeedbackMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackMapper feedbackMapper;

    // 1. 저장
    @Transactional
    public Long saveFeedback(FeedbackDTO dto) {
        feedbackMapper.saveFeedback(dto);
        return dto.getId();
    }

    // 2. 목록 조회
    public List<FeedbackDTO> findAllByUserId(String userId) {
        return feedbackMapper.findAllByUserId(userId);
    }

    // 3. 상세 조회
    public FeedbackDTO findById(Long id) {
        return feedbackMapper.findById(id);
    }

    // 4. 내용 수정
    @Transactional
    public void updateFeedback(FeedbackDTO dto) {
        feedbackMapper.updateFeedback(dto);
    }

    // 5. 상태 변경 (칸반 보드용)
    @Transactional
    public void updateStatus(Long id, JobStatus status) {
        feedbackMapper.updateStatus(id, status);
    }
}