package com.polifeed.controller;

import com.polifeed.dto.FeedbackDTO;
import com.polifeed.mapper.FeedbackMapper;
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
    private final FeedbackMapper feedbackMapper;

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

        // 1. AI 분석 호출 (전체 응답 받기)
        String fullResponse = geminiService.getFeedback(topic, text, persona, jdText);

        // 2. 응답 분리 (텍스트 vs JSON)
        String feedbackText = fullResponse; // 기본값은 전체
        Map<String, Integer> scores = new HashMap<>();
        // 기본 점수 세팅 (파싱 실패시 0점 방지용)
        scores.put("logic", 5); scores.put("jobFit", 5); scores.put("sincerity", 5);
        scores.put("creativity", 5); scores.put("readability", 5);

        try {
            if (fullResponse.contains("[[JSON_START]]")) {
                String[] parts = fullResponse.split("\\[\\[JSON_START\\]\\]");
                feedbackText = parts[0]; // 앞부분: 마크다운 텍스트

                if (parts.length > 1 && parts[1].contains("[[JSON_END]]")) {
                    String jsonPart = parts[1].split("\\[\\[JSON_END\\]\\]")[0];
                    // JSON 문자열 -> Map 변환
                    Map<String, Integer> parsed = objectMapper.readValue(jsonPart, Map.class);
                    scores.putAll(parsed); // 덮어쓰기
                }
            }
        } catch (Exception e) {
            System.out.println("JSON 파싱 에러(무시하고 진행): " + e.getMessage());
        }

        // 3. DTO 생성 및 값 주입 (생성자 대신 Setter 사용이 안전함)
        FeedbackDTO dto = new FeedbackDTO();
        dto.setUserId(userId);
        dto.setTopic(topic);
        dto.setOriginalText(text);
        dto.setFeedbackText(feedbackText); // JSON 제외한 순수 텍스트
        dto.setPersona(persona);
        dto.setJdText(jdText);
        // 점수 주입
        dto.setScoreLogic(scores.getOrDefault("logic", 0));
        dto.setScoreJobFit(scores.getOrDefault("jobFit", 0));
        dto.setScoreSincerity(scores.getOrDefault("sincerity", 0));
        dto.setScoreCreativity(scores.getOrDefault("creativity", 0));
        dto.setScoreReadability(scores.getOrDefault("readability", 0));

        // DB 저장
        feedbackMapper.saveFeedback(dto);

        // 결과 화면 전달
        model.addAttribute("dto", dto); // dto 통째로 넘김
        model.addAttribute("userName", userId);

        // *상세 페이지로 바로 이동하는 게 데이터 보기에 편합니다 (기존 index 대신 detail 추천)
        return "detail";
    }

    // === 3. 마이페이지 ===
    @GetMapping("/mypage")
    public String myPage(Model model, @AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User == null) return "redirect:/login";
        String userId = oauth2User.getAttribute("name");
        List<FeedbackDTO> list = feedbackMapper.findAllByUserId(userId);
        model.addAttribute("list", list);
        model.addAttribute("userName", userId);
        return "mypage";
    }

    // === 4. 상세 화면 ===
    @GetMapping("/mypage/{id}")
    public String detail(@PathVariable Long id, Model model, @AuthenticationPrincipal OAuth2User oauth2User) {
        FeedbackDTO dto = feedbackMapper.findById(id);
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
                                @AuthenticationPrincipal OAuth2User oauth2User,
                                Model model) {

        String userId = oauth2User.getAttribute("name");

        // 1. AI 다시 분석
        String fullResponse = geminiService.getFeedback(topic, text, persona, jdText);

        // 2. 파싱 로직 (위와 동일)
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

        // DB 업데이트
        feedbackMapper.updateFeedback(dto);

        return "redirect:/mypage/" + id;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}