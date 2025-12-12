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

    public String getFeedback(String topic, String resumeText, String persona, String jdText) {
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

        String analysisTitle;
        String analysisInstruction;

        String jdPrompt = "";
        if (jdText != null && !jdText.isBlank()) {
            jdPrompt = "\n\n🚨 **[매우 중요] 채용 공고(JD) 매칭 및 약점 보완 전략** 🚨\n" +
                    "지원자는 아래 **채용 공고**를 보고 지원했습니다.\n" +
                    "--------------------------------------------------\n" +
                    jdText + "\n" +
                    "--------------------------------------------------\n" +
                    "분석 시 다음 **3단계 전략**을 수행하세요:\n" +
                    "1. **요건 분류**: 공고 내용을 '필수 요건(Must)'과 '우대 사항(Preferred)'으로 구분.\n" +
                    "2. **Gap 분석**: 지원자 답변에서 누락된 키워드를 찾으세요.\n" +
                    "3. **대체 전략 제시 (가장 중요)**:\n" +
                    "   - **우대 사항**이 없을 때: '관심과 학습 의지'를 어필하는 문구 제안.\n" +
                    "   - **필수 요건**이 없을 때: 탈락이라고 단정 짓지 말고, **'유사 경험(기초 지식, 다른 언어/툴 사용 경험)'을 들어 '핵심 원리는 이해하고 있어 빠르게 적응 가능하다'는 논리**를 만들도록 조언하세요.";

            analysisTitle = "채용 공고(JD) 적합성 및 합격 전략";
            analysisInstruction = "단순히 '없다'고 지적하는 것을 넘어, **합격을 위한 디펜스(방어) 논리**를 만들어줘.\n" +
                    "   - **필수 요건 미충족 시**: 치명적일 수 있음을 경고하되, **'제가 A는 안 써봤지만, B를 써봤기에 A도 금방 배웁니다'** 식의 구체적인 **대체 설득 논리**를 문장으로 알려줘.\n" +
                    "   - **우대 사항 미충족 시**: 없는 것을 솔직히 인정하되, 입사 후 기여할 수 있는 **잠재력과 태도**를 강조하는 문장 추천.";
        } else {
            // JD 없을 때
            analysisTitle = "질문 적합성 체크 (가장 중요)";
            analysisInstruction = "지원자가 기업의 질문 의도를 정확히 파악했는지, 아니면 동문서답을 하고 있는지 냉철하게 평가해줘.";
        }

        String finalPrompt =
                "역할 설정: " + personaInstruction + "\n\n" +
                        jdPrompt + "\n\n" +
                        "기업 질문: \"" + topic + "\"\n" +
                        "지원자 답변: \"" + resumeText + "\"\n\n" +
                        "위 내용을 분석하여 다음 4가지 항목으로 마크다운 형식의 피드백을 주세요.\n" +
                        "1. **" + analysisTitle + "**: " + analysisInstruction + "\n" +
                        "2. **내용 분석 및 피드백**: 잘한 점과 아쉬운 점을 구체적으로 지적해줘.\n" +
                        "3. **수정 제안**: 아쉬운 부분을 보완할 수 있는 구체적인 문장 예시를 보여줘.\n" +
                        "4. **꼬리 질문**: 이 내용으로 실제 면접을 본다면 물어볼 날카로운 질문 2가지를 뽑아줘." +
                        scorePrompt;

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

    public String rewriteText(String originalText) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        // 교정 전용 프롬프트: '에디터' 페르소나 부여
        String prompt = "당신은 **전문 교열가(Professional Editor)**입니다. 아래 자소서 내용을 다음 기준에 맞춰 수정해 주세요.\n" +
                "1. **문법 및 맞춤법 교정**: 오타나 비문을 완벽하게 수정하세요.\n" +
                "2. **가독성 향상**: 문장을 간결하고 명확하게 다듬으세요.\n" +
                "3. **전문적인 톤앤매너**: 지원자의 강점이 잘 드러나도록 정중하고 신뢰감 있는 어휘를 사용하세요.\n" +
                "4. **길이 유지**: 원본 내용의 핵심을 유지하되, 지나치게 길어지거나 짧아지지 않게 하세요.\n\n" +
                "--- [원본 텍스트] ---\n" +
                originalText + "\n" +
                "---------------------\n" +
                "수정된 텍스트만 출력해 주세요. (사족 금지)";

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
            return "죄송합니다. 문장 교정 중 오류가 발생했습니다.";
        }
    }
}