package com.polifeed.mapper;

import com.polifeed.dto.FeedbackDTO;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface FeedbackMapper {

    // [수정됨] 점수 컬럼 추가 저장
    @Insert("INSERT INTO feedback_log " +
            "(user_id, topic, original_text, feedback_text, persona, created_at, " +
            "score_logic, score_job_fit, score_sincerity, score_creativity, score_readability) " +
            "VALUES " +
            "(#{userId}, #{topic}, #{originalText}, #{feedbackText}, #{persona}, NOW(), " +
            "#{scoreLogic}, #{scoreJobFit}, #{scoreSincerity}, #{scoreCreativity}, #{scoreReadability})")
    void saveFeedback(FeedbackDTO feedbackDTO);

    @Select("SELECT * FROM feedback_log WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<FeedbackDTO> findAllByUserId(String userId);

    @Select("SELECT * FROM feedback_log WHERE id = #{id}")
    FeedbackDTO findById(Long id);

    // [수정됨] 점수 컬럼도 같이 업데이트
    @Update("UPDATE feedback_log " +
            "SET topic = #{topic}, " +
            "    original_text = #{originalText}, " +
            "    feedback_text = #{feedbackText}, " +
            "    persona = #{persona}, " +
            "    score_logic = #{scoreLogic}, " +
            "    score_job_fit = #{scoreJobFit}, " +
            "    score_sincerity = #{scoreSincerity}, " +
            "    score_creativity = #{scoreCreativity}, " +
            "    score_readability = #{scoreReadability}, " +
            "    created_at = NOW() " +
            "WHERE id = #{id}")
    void updateFeedback(FeedbackDTO feedbackDTO);
}