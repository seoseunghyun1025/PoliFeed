package com.polifeed.mapper;

import com.polifeed.dto.FeedbackDTO;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface FeedbackMapper {

    // 1. 저장 (INSERT) - topic 추가
    @Insert("INSERT INTO feedback_log (user_id, topic, original_text, feedback_text, created_at) " +
            "VALUES (#{userId}, #{topic}, #{originalText}, #{feedbackText}, NOW())")
    void saveFeedback(FeedbackDTO feedbackDTO);

    // 2. 목록 조회 (SELECT List) - 마이페이지용
    @Select("SELECT * FROM feedback_log WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<FeedbackDTO> findAllByUserId(String userId);

    // 3. 상세 조회 (SELECT One) - 수정 화면용
    @Select("SELECT * FROM feedback_log WHERE id = #{id}")
    FeedbackDTO findById(Long id);

    // 4. ★ 수정 (UPDATE) ★ - 내용을 고치고 다시 피드백 받으면 덮어쓰기
    @Update("UPDATE feedback_log " +
            "SET topic = #{topic}, " +
            "    original_text = #{originalText}, " +
            "    feedback_text = #{feedbackText}, " +
            "    created_at = NOW() " + // 수정일자로 갱신 (선택사항)
            "WHERE id = #{id}")
    void updateFeedback(FeedbackDTO feedbackDTO);
}