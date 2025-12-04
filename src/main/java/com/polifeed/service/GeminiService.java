package com.polifeed.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiService {

    @Value("${GEMINI_API_KEY}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    // ★ 수정됨: 파라미터에 'String topic' 추가 ★
    public String getFeedback(String topic, String resumeText) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        String prompt = "너는 10년 차 시니어 개발자 면접관이자 채용 담당자야.\n" +
                "기업에서 지원자에게 아래와 같은 **자기소개서 문항(질문)**을 제시했어.\n\n" +
                "✅ **기업의 질문**: \"" + topic + "\"\n\n" +
                "지원자가 위 질문에 대해 작성한 답변을 분석해서 피드백을 줘.\n" +
                "1. **질문 적합성 체크 (가장 중요)**: 지원자가 기업의 질문 의도를 정확히 파악했는지, 아니면 동문서답을 하고 있는지 평가해줘.\n" +
                "2. **내용 분석 및 피드백**: 잘한 점과 아쉬운 점을 구체적으로 지적해줘.\n" +
                "3. **수정 제안**: 아쉬운 부분을 보완할 수 있는 구체적인 문장 예시를 보여줘.\n" +
                "4. **꼬리 질문**: 이 내용으로 실제 면접을 본다면 물어볼 날카로운 질문 2가지를 뽑아줘.\n\n" +
                "--- ✍️ 지원자 답변 내용 ---\n" + resumeText;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(
                Map.of("parts", List.of(
                        Map.of("text", prompt)
                ))
        ));

        try {
            Map<String, Object> response = restTemplate.postForObject(url, requestBody, Map.class);
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            return (String) parts.get(0).get("text");

        } catch (Exception e) {
            e.printStackTrace();
            return "죄송합니다. AI 분석 중 오류가 발생했습니다. (" + e.getMessage() + ")";
        }
    }
}