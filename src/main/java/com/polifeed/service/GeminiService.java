package com.polifeed.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
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
    public List<String> createInterviewQuestions(String resumeText, String jdText, String persona) {
        String role = "friendly".equals(persona) ? "호기심 많은 기술 면접관" : "압박 면접관";
        String prompt = "당신은 " + role + "입니다. 자소서/JD를 보고 면접 질문 5개를 뽑아주세요.\n" +
                "조건: 서론이나 번호(1.) 없이 **순수 질문 문장만 5줄** 작성하세요. 한 줄에 질문 하나씩.\n\n[자소서]\n" + resumeText;

        try {
            String text = callGeminiApi(prompt);
            // [수정] 줄바꿈으로 나누고, 혹시 번호(1.)가 붙어있으면 제거하고, 빈 줄은 버림
            List<String> questions = new ArrayList<>();
            for (String line : text.split("\n")) {
                String cleanLine = line.replaceAll("^\\d+\\.\\s*", "").trim(); // "1. 질문" -> "질문"
                if (cleanLine.length() > 10) { // 너무 짧은 건 질문 아님
                    questions.add(cleanLine);
                }
            }
            // 만약 질문이 너무 적으면 기본 질문 채워넣기 (안전장치)
            while (questions.size() < 3) {
                questions.add("우리 회사의 지원 동기는 무엇인가요?");
                questions.add("본인의 장단점은 무엇인가요?");
                questions.add("입사 후 이루고 싶은 목표는?");
            }
            return questions.subList(0, Math.min(questions.size(), 5)); // 최대 5개
        } catch (Exception e) {
            return List.of("자기소개를 해주세요.", "성격의 장단점은?", "지원 동기는?");
        }
    }

    public String evaluateInterviewAnswer(String question, String userAnswer) {
        String prompt = "면접관으로서 지원자의 답변을 평가해주세요.\n" +
                "질문: \"" + question + "\"\n" +
                "답변: \"" + userAnswer + "\"\n\n" +
                "피드백 가이드:\n" +
                "1. **좋은 점**: 구체성, 태도 등.\n" +
                "2. **아쉬운 점**: 부족한 논리, 너무 짧은 답변 등.\n" +
                "3. **모범 답안 예시**: 더 나은 답변 방향 제안.\n" +
                "짧고 굵게 마크다운 형식으로 답변해주세요.";

        return this.callGeminiApi(prompt);
    }

    public String replyToChat(String previousContext, String userMessage, String persona) {
        String role = "friendly".equals(persona) ? "친절한 멘토" : "냉철한 면접관";

        String prompt = "당신은 " + role + "입니다.\n" +
                "상황: 면접 질문에 대해 피드백을 주었는데, 지원자가 이에 대해 추가 질문이나 반론을 제기했습니다.\n" +
                "--- [이전 문맥] ---\n" + previousContext + "\n" +
                "--- [지원자 말] ---\n" + userMessage + "\n" +
                "------------------\n" +
                "지원자의 말에 적절하게 대답해주세요. (다음 면접 질문은 하지 마세요. 대화만 하세요.)";

        return callGeminiApi(prompt);
    }

    private String callGeminiApi(String prompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));

        try {
            Map<String, Object> response = restTemplate.postForObject(url, requestBody, Map.class);

            // 1. 응답 자체가 비어있는 경우
            if (response == null) return "AI 서버 응답이 없습니다.";

            // 2. 후보군(candidates)이 비어있는 경우 (주로 안전 필터에 걸렸을 때)
            if (!response.containsKey("candidates")) {
                // 안전 필터 피드백이 있는지 확인
                if (response.containsKey("promptFeedback")) {
                    return "⚠️ AI가 답변 생성을 거부했습니다. (사유: 안전 필터/민감한 주제)";
                }
                return "AI가 답변을 생성하지 못했습니다.";
            }

            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                return "⚠️ AI 답변 생성 실패 (내용이 너무 짧거나 필터링됨)";
            }

            // 3. 정상적으로 텍스트 추출
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            return (String) parts.get(0).get("text");

        } catch (HttpClientErrorException.TooManyRequests e) {
            return "⛔ 요청이 너무 많습니다. 잠시 후(10초 뒤) 다시 시도해주세요.";
        } catch (Exception e) {
            e.printStackTrace(); // 서버 콘솔에 진짜 에러 원인을 찍어줌
            return "🚫 시스템 오류가 발생했습니다. (" + e.getMessage() + ")";
        }
    }

}