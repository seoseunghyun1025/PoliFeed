package com.polifeed.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiService { // 이름은 유지하지만 내부는 GPT입니다.

    @Value("${openapi.api-key}")
    private String apiKey;
    @Value("${openapi.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    // 1. 피드백 생성
    public String getFeedback(String topic, String resumeText, String persona, String jdText) {
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
                    "채용 공고:\n" + jdText + "\n" +
                    "분석 전략:\n" +
                    "1. 필수/우대 사항 구분.\n" +
                    "2. Gap 분석.\n" +
                    "3. 약점 방어 논리(필수 요건 미충족 시 대체 경험 어필) 작성.";
            analysisTitle = "채용 공고(JD) 적합성 및 합격 전략";
            analysisInstruction = "합격을 위한 디펜스(방어) 논리를 구체적으로 만들어주세요.";
        } else {
            analysisTitle = "질문 적합성 체크";
            analysisInstruction = "기업의 질문 의도를 정확히 파악했는지 평가해주세요.";
        }

        String finalPrompt = "역할: " + personaInstruction + "\n\n" +
                jdPrompt + "\n\n" +
                "질문: \"" + topic + "\"\n" +
                "답변: \"" + resumeText + "\"\n\n" +
                "위 내용을 분석하여 다음 4가지 항목으로 마크다운 피드백을 주세요.\n" +
                "1. **" + analysisTitle + "**: " + analysisInstruction + "\n" +
                "2. **내용 분석 및 피드백**: 잘한 점, 아쉬운 점.\n" +
                "3. **수정 제안**: 구체적인 문장 예시.\n" +
                "4. **꼬리 질문**: 날카로운 질문 2가지." +
                scorePrompt;

        return callGptApi(finalPrompt);
    }

    // 2. 문장 교정 (Rewrite)
    public String rewriteText(String originalText) {
        String prompt = "당신은 **전문 교열가(Professional Editor)**입니다. 아래 자소서를 교정해 주세요.\n" +
                "1. 문법/오타 수정\n2. 가독성 향상\n3. 전문적인 톤앤매너\n4. 길이 유지\n\n" +
                "--- [원본] ---\n" + originalText + "\n" +
                "---------------------\n" +
                "수정된 텍스트만 출력해 주세요. (사족 금지)";
        return callGptApi(prompt);
    }

    // 3. 면접 질문 생성
    public List<String> createInterviewQuestions(String resumeText, String jdText, String persona) {
        String role = "friendly".equals(persona) ? "호기심 많은 기술 면접관" : "압박 면접관";
        String prompt = "당신은 " + role + "입니다. 자소서 내용을 바탕으로 면접 질문 5개를 뽑아주세요.\n" +
                "조건: 번호나 서론 없이 **오직 질문 문장만 5줄** 작성하세요.\n\n[자소서]\n" + resumeText;

        try {
            String text = callGptApi(prompt);
            List<String> questions = new ArrayList<>();
            for (String line : text.split("\n")) {
                String cleanLine = line.replaceAll("^\\d+\\.\\s*", "").trim();
                if (cleanLine.length() > 5) {
                    questions.add(cleanLine);
                }
            }
            while (questions.size() < 3) {
                questions.add("우리 회사의 지원 동기는 무엇인가요?");
                questions.add("성격의 장단점은?");
                questions.add("입사 후 목표는?");
            }
            return questions.subList(0, Math.min(questions.size(), 5));
        } catch (Exception e) {
            return List.of("자기소개를 해주세요.", "성격의 장단점은?", "지원 동기는?");
        }
    }

    // 4. 답변 평가
    public String evaluateInterviewAnswer(String question, String userAnswer) {
        String prompt = "면접관으로서 평가해줘.\n질문: " + question + "\n답변: " + userAnswer + "\n\n" +
                "1. 좋은 점\n2. 아쉬운 점\n3. 모범 답안 예시\n짧고 굵게 마크다운으로 답변.";
        return callGptApi(prompt);
    }

    // 5. 챗봇 대화
    public String replyToChat(String previousContext, String userMessage, String persona) {
        String role = "friendly".equals(persona) ? "친절한 멘토" : "냉철한 면접관";
        String prompt = "당신은 " + role + "입니다. 상황: 면접 피드백 중 대화.\n" +
                "이전 문맥: " + previousContext + "\n" +
                "지원자 말: " + userMessage + "\n" +
                "적절하게 대답하세요. (다음 질문 금지, 대화만)";
        return callGptApi(prompt);
    }

    // 6. 히트맵 분석
    public String getHeatmapAnalysis(String resumeText) {
        String prompt = "당신은 면접관입니다. 자소서에서 '중요한 부분(HOT)'과 '지루한 부분(COOL)'을 HTML 태그로 표시하세요.\n" +
                "1. HOT: <span class='heat-hot'>...</span> (구체적 성과, 기술, 직무 경험)\n" +
                "2. COOL: <span class='heat-cool'>...</span> (상투적 표현, 추상적 형용사)\n" +
                "3. 나머지: 그대로.\n" +
                "오직 HTML 태그가 적용된 본문만 출력. 마크다운 금지.\n\n" + resumeText;
        return callGptApi(prompt);
    }

    // ✅ [핵심] OpenAI API 호출 메서드 (Gemini와 방식이 다름)
    private String callGptApi(String prompt) {
        String url = "https://api.openai.com/v1/chat/completions";

        // 1. 헤더 설정 (Authorization 필수)
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 2. 바디 설정 (GPT 형식)
        Map<String, Object> body = new HashMap<>();
        body.put("model", model); // gpt-4o-mini
        body.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
        ));
        body.put("temperature", 0.7);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            // 3. 요청 전송
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !responseBody.containsKey("choices")) {
                return "AI 응답 오류";
            }

            // 4. 응답 파싱 (choices[0].message.content)
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");

        } catch (HttpClientErrorException e) {
            e.printStackTrace();
            return "⛔ OpenAI 오류: " + e.getStatusCode() + " - " + e.getResponseBodyAsString();
        } catch (Exception e) {
            e.printStackTrace();
            return "🚫 시스템 오류: " + e.getMessage();
        }
    }
}