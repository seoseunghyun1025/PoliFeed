package com.polifeed.controller;

import com.polifeed.dto.FeedbackDTO;
import com.polifeed.entity.JobStatus;
import com.polifeed.service.FeedbackService;
import com.polifeed.service.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final GeminiService geminiService;
    private final FeedbackService feedbackService;

    // [추가] JSON 파싱용 객체
    private final ObjectMapper objectMapper = new ObjectMapper();

    // === 1. 메인 화면 ===
    @GetMapping("/")
    public String main(Model model, @AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User != null) {
            model.addAttribute("userName", oauth2User.getAttribute("name"));
        }
        return "index";
    }

    // === 2. 신규 분석 요청 (파싱 로직 추가) ===
    @PostMapping("/analyze")
    public String analyze(@RequestParam("topic") String topic,
                          @RequestParam("resumeText") String text,
                          @RequestParam("persona") String persona,
                          @RequestParam(value = "jdText", required = false) String jdText,
                          @AuthenticationPrincipal OAuth2User oauth2User,
                          Model model) {

        if (oauth2User == null) return "redirect:/login";
        String userId = oauth2User.getAttribute("name");

        // 1. AI 분석 호출
        String fullResponse = geminiService.getFeedback(topic, text, persona, jdText);

        // 2. 응답 분리 (텍스트 vs JSON)
        String feedbackText = fullResponse;
        Map<String, Integer> scores = new HashMap<>();
        scores.put("logic", 5); scores.put("jobFit", 5); scores.put("sincerity", 5);
        scores.put("creativity", 5); scores.put("readability", 5);

        try {
            if (fullResponse.contains("[[JSON_START]]")) {
                String[] parts = fullResponse.split("\\[\\[JSON_START\\]\\]");
                feedbackText = parts[0];

                if (parts.length > 1 && parts[1].contains("[[JSON_END]]")) {
                    String jsonPart = parts[1].split("\\[\\[JSON_END\\]\\]")[0];
                    Map<String, Integer> parsed = objectMapper.readValue(jsonPart, Map.class);
                    scores.putAll(parsed);
                }
            }
        } catch (Exception e) {
            System.out.println("JSON 파싱 에러(무시하고 진행): " + e.getMessage());
        }

        // 3. DTO 생성
        FeedbackDTO dto = new FeedbackDTO();
        dto.setUserId(userId);
        dto.setTopic(topic);
        dto.setOriginalText(text);
        dto.setFeedbackText(feedbackText);
        dto.setPersona(persona);
        dto.setJdText(jdText);

        // 점수 주입
        dto.setScoreLogic(scores.getOrDefault("logic", 0));
        dto.setScoreJobFit(scores.getOrDefault("jobFit", 0));
        dto.setScoreSincerity(scores.getOrDefault("sincerity", 0));
        dto.setScoreCreativity(scores.getOrDefault("creativity", 0));
        dto.setScoreReadability(scores.getOrDefault("readability", 0));

        // [NEW] 기본 상태 설정 (혹시 DTO에 기본값이 안 들어가 있다면 여기서 설정)
        dto.setStatus(JobStatus.PREPARE);

        // [NEW] DB 저장 (서비스 사용) - 저장 후 ID를 받아옴
        Long savedId = feedbackService.saveFeedback(dto);

        // 저장 후 상세 페이지로 리다이렉트 (새로고침 시 중복 제출 방지)
        return "redirect:/mypage/" + savedId;
    }

    // === 3. 마이페이지 ===
    @GetMapping("/mypage")
    public String myPage(Model model, @AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User == null) return "redirect:/login";
        String userId = oauth2User.getAttribute("name");
        List<FeedbackDTO> list = feedbackService.findAllByUserId(userId);
        model.addAttribute("list", list);
        model.addAttribute("userName", userId);
        return "mypage";
    }

    // === 4. 상세 화면 ===
    @GetMapping("/mypage/{id}")
    public String detail(@PathVariable Long id, Model model, @AuthenticationPrincipal OAuth2User oauth2User) {
        FeedbackDTO dto = feedbackService.findById(id);
        model.addAttribute("dto", dto);
        model.addAttribute("userName", oauth2User.getAttribute("name"));
        return "detail";
    }

    // === 5. 재분석 (파싱 로직 추가) ===
    @PostMapping("/analyze/update")
    public String updateAnalyze(@RequestParam("id") Long id,
                                @RequestParam("topic") String topic,
                                @RequestParam("resumeText") String text,
                                @RequestParam("persona") String persona,
                                @RequestParam(value = "jdText", required = false) String jdText,
                                @RequestParam(value = "status", required = false) String status,
                                @AuthenticationPrincipal OAuth2User oauth2User,
                                Model model) {

        String userId = oauth2User.getAttribute("name");

        // 1. AI 다시 분석
        String fullResponse = geminiService.getFeedback(topic, text, persona, jdText);

        // 2. 파싱 로직
        String feedbackText = fullResponse;
        Map<String, Integer> scores = new HashMap<>();
        scores.put("logic", 5); scores.put("jobFit", 5); scores.put("sincerity", 5);
        scores.put("creativity", 5); scores.put("readability", 5);

        try {
            if (fullResponse.contains("[[JSON_START]]")) {
                String[] parts = fullResponse.split("\\[\\[JSON_START\\]\\]");
                feedbackText = parts[0];
                if (parts.length > 1 && parts[1].contains("[[JSON_END]]")) {
                    String jsonPart = parts[1].split("\\[\\[JSON_END\\]\\]")[0];
                    Map<String, Integer> parsed = objectMapper.readValue(jsonPart, Map.class);
                    scores.putAll(parsed);
                }
            }
        } catch (Exception e) {}

        // 3. DTO 업데이트
        FeedbackDTO dto = new FeedbackDTO();
        dto.setId(id);
        dto.setUserId(userId);
        dto.setTopic(topic);
        dto.setOriginalText(text);
        dto.setFeedbackText(feedbackText);
        dto.setPersona(persona);
        dto.setJdText(jdText);
        dto.setScoreLogic(scores.getOrDefault("logic", 0));
        dto.setScoreJobFit(scores.getOrDefault("jobFit", 0));
        dto.setScoreSincerity(scores.getOrDefault("sincerity", 0));
        dto.setScoreCreativity(scores.getOrDefault("creativity", 0));
        dto.setScoreReadability(scores.getOrDefault("readability", 0));

        if (status != null && !status.isEmpty()) {
            try {
                dto.setStatus(JobStatus.valueOf(status));
            } catch (IllegalArgumentException e) {
                // 에러 나면 기본값 유지
            }
        }

        // [NEW] DB 업데이트 (서비스 호출)
        feedbackService.updateFeedback(dto);

        // URL은 /detail/{id} 또는 /mypage/{id} 중 사용하는 걸로 리다이렉트
        return "redirect:/mypage/" + id;
    }

    @PostMapping("/api/rewrite")
    @ResponseBody // HTML이 아니라 데이터(String)만 반환
    public String rewrite(@RequestBody Map<String, String> request) {
        String originalText = request.get("originalText");

        // 서비스 호출 (글 다듬기)
        return geminiService.rewriteText(originalText);
    }

    @GetMapping("/interview/{id}")
    public String interviewPage(@PathVariable Long id, Model model, @AuthenticationPrincipal OAuth2User oauth2User) {
        FeedbackDTO dto = feedbackService.findById(id);

        // 본인 글 아니면 차단 (간단 체크)
        if (!dto.getUserId().equals(oauth2User.getAttribute("name"))) {
            return "redirect:/mypage";
        }

        model.addAttribute("dto", dto);
        model.addAttribute("userName", oauth2User.getAttribute("name"));
        return "interview"; // interview.html로 이동
    }

    @PostMapping("/api/interview/init")
    @ResponseBody
    public List<String> initInterview(@RequestBody Map<String, String> request) {
        // 화면에서 넘겨준 persona 값도 같이 전달
        return geminiService.createInterviewQuestions(
                request.get("resumeText"),
                request.get("jdText"),
                request.get("persona") // [NEW] 추가됨
        );
    }

    @PostMapping("/api/interview/feedback")
    @ResponseBody
    public String feedbackInterview(@RequestBody Map<String, String> request) {
        return geminiService.evaluateInterviewAnswer(request.get("question"), request.get("answer"));
    }

    @PostMapping("/api/interview/chat")
    @ResponseBody
    public String chatInterview(@RequestBody Map<String, String> request) {
        return geminiService.replyToChat(
                request.get("context"),
                request.get("message"),
                request.get("persona")
        );
    }

    @PostMapping("/api/analysis/heatmap")
    @ResponseBody  // ★ 이게 있어야 화면 이동 안 하고 '텍스트'만 보냅니다!
    public String analyzeHeatmap(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        return geminiService.getHeatmapAnalysis(text);
    }

    @PostMapping("/mypage/delete/{id}") // DELETE 대신 POST를 사용하여 폼 전송에 대응
    public String deleteFeedback(@PathVariable Long id) {
        feedbackService.deleteFeedback(id);
        return "redirect:/mypage";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}