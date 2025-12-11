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

    public String getFeedback(String topic, String resumeText, String persona) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        String personaInstruction = "";
        if ("strict".equals(persona)) {
            personaInstruction = "당신은 **대기업 인사팀장(15년차)**입니다. 매우 깐깐하고 보수적입니다.\n" +
                    "- 답변의 오타, 비문, 근거 없는 주장을 호되게 지적하세요.\n" +
                    "- 칭찬보다는 **탈락 사유**를 중심으로 냉철하게 비판하세요.\n" +
                    "- **4번 꼬리 질문**에서는 지원자를 당황하게 만들 수 있는 **'압박 질문'**을 던지세요.";
        } else if ("friendly".equals(persona)) {
            personaInstruction = "당신은 **스타트업의 친절한 개발 팀장(CTO)**입니다. 지원자의 잠재력을 봅니다.\n" +
                    "- 부족한 점은 **'어떻게 고치면 좋을지'** 구체적인 예시로 친절하게 조언하세요.\n" +
                    "- 지원자의 경험에서 **기술적 장점**을 찾아 칭찬해주세요.\n" +
                    "- **4번 꼬리 질문**에서는 실무 능력을 확인하는 **'기술/협업 심층 질문'**을 던지세요.";
        } else {
            personaInstruction = "당신은 **전문 취업 컨설턴트**입니다.\n" +
                    "- 균형 잡힌 시각으로 장점과 단점을 객관적으로 분석하세요.\n" +
                    "- **4번 꼬리 질문**에서는 일반적인 면접 예상 질문을 던지세요.";
        }

        // [추가됨] 점수 평가 요청 프롬프트
        String scorePrompt = "\n\n" +
                "--- [마지막 요청사항] ---\n" +
                "피드백 작성이 끝나면, 맨 마지막 줄에 지원자의 역량을 5가지 항목(논리력, 직무적합성, 성실성, 창의성, 가독성)으로 평가하여 " +
                "10점 만점 기준의 점수를 아래 **JSON 포맷으로만** 추가해 주세요. " +
                "JSON 앞뒤에는 파싱을 위해 '[[JSON_START]]' 와 '[[JSON_END]]' 태그를 반드시 붙여주세요.\n" +
                "예시: [[JSON_START]]{\"logic\": 8, \"jobFit\": 7, \"sincerity\": 9, \"creativity\": 6, \"readability\": 8}[[JSON_END]]";

        String finalPrompt =
                "역할 설정: " + personaInstruction + "\n\n" +
                        "기업에서 지원자에게 아래와 같은 **자기소개서 문항(질문)**을 제시했습니다.\n\n" +
                        "✅ **기업의 질문**: \"" + topic + "\"\n" +
                        "✅ **지원자 답변**: \"" + resumeText + "\"\n\n" +
                        "위 역할(페르소나)에 맞춰 지원자의 답변을 분석하고, 아래 **4가지 항목**에 따라 마크다운 형식으로 피드백을 주세요.\n\n" +
                        "1. **질문 적합성 체크 (가장 중요)**: 지원자가 기업의 질문 의도를 정확히 파악했는지, 아니면 동문서답을 하고 있는지 평가해줘.\n" +
                        "2. **내용 분석 및 피드백**: 잘한 점과 아쉬운 점을 구체적으로 지적해줘.\n" +
                        "3. **수정 제안**: 아쉬운 부분을 보완할 수 있는 구체적인 문장 예시를 보여줘.\n" +
                        "4. **꼬리 질문**: 이 내용으로 실제 면접을 본다면 물어볼 날카로운 질문 2가지를 뽑아줘." +
                        scorePrompt; // 여기에 붙임

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(
                Map.of("parts", List.of(
                        Map.of("text", finalPrompt)
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
            return "죄송합니다. AI 분석 중 오류가 발생했습니다.";
        }
    }
}