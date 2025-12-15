package com.polifeed.mapper;

import com.polifeed.dto.FeedbackDTO;
import com.polifeed.entity.JobStatus;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface FeedbackMapper {

    // [수정됨] 점수 컬럼 추가 저장
    @Insert("INSERT INTO feedback_log " +
            "(user_id, topic, original_text, feedback_text, persona, jd_text, created_at, " +
            "score_logic, score_job_fit, score_sincerity, score_creativity, score_readability, status) " + // status 추가
            "VALUES " +
            "(#{userId}, #{topic}, #{originalText}, #{feedbackText}, #{persona}, #{jdText}, NOW(), " +
            "#{scoreLogic}, #{scoreJobFit}, #{scoreSincerity}, #{scoreCreativity}, #{scoreReadability}, #{status})") // #{status} 추가
    @Options(useGeneratedKeys = true, keyProperty = "id") // ID 자동 생성된 거 DTO에 채워주기
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
            "    jd_text = #{jdText}," +
            "    status = #{status}, " +
            "    score_logic = #{scoreLogic}, " +
            "    score_job_fit = #{scoreJobFit}, " +
            "    score_sincerity = #{scoreSincerity}, " +
            "    score_creativity = #{scoreCreativity}, " +
            "    score_readability = #{scoreReadability}, " +
            "    created_at = NOW() " +
            "WHERE id = #{id}")
    void updateFeedback(FeedbackDTO feedbackDTO);

    // [수정됨 3] ★ 여기가 핵심! 어노테이션 추가
    @Update("UPDATE feedback_log SET status = #{status} WHERE id = #{id}")
    void updateStatus(@Param("id") Long id, @Param("status") JobStatus status);

    @Delete("DELETE FROM feedback_log WHERE id = #{id}")
    void deleteFeedback(Long id);
}