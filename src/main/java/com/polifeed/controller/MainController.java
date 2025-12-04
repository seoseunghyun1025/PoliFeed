package com.polifeed.controller;

import com.polifeed.dto.FeedbackDTO;
import com.polifeed.mapper.FeedbackMapper;
import com.polifeed.service.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final GeminiService geminiService;
    private final FeedbackMapper feedbackMapper;

    // === 1. 메인 화면 (신규 작성) ===
    @GetMapping("/")
    public String main(Model model, @AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User != null) {
            model.addAttribute("userName", oauth2User.getAttribute("name"));
        }
        return "index";
    }

    // === 2. 신규 분석 요청 (INSERT) ===
    @PostMapping("/analyze")
    public String analyze(@RequestParam("topic") String topic, // 주제 받기
                          @RequestParam("resumeText") String text,
                          @AuthenticationPrincipal OAuth2User oauth2User,
                          Model model) {

        if (oauth2User == null) return "redirect:/login";
        String userId = oauth2User.getAttribute("name");

        // AI 분석
        String feedback = geminiService.getFeedback(topic, text);

        // DB 저장 (INSERT)
        FeedbackDTO dto = new FeedbackDTO(null, userId, topic, text, feedback, null);
        feedbackMapper.saveFeedback(dto);

        // 결과 화면으로
        model.addAttribute("topic", topic);
        model.addAttribute("originalText", text);
        model.addAttribute("feedback", feedback);
        model.addAttribute("userName", userId);

        return "index";
    }

    // === 3. 마이페이지 (목록 보기) ===
    @GetMapping("/mypage")
    public String myPage(Model model, @AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User == null) return "redirect:/login";
        String userId = oauth2User.getAttribute("name");

        // 내 기록 다 가져오기
        List<FeedbackDTO> list = feedbackMapper.findAllByUserId(userId);
        model.addAttribute("list", list);
        model.addAttribute("userName", userId);

        return "mypage"; // mypage.html로 이동
    }

    // === 4. 상세/수정 화면 이동 ===
    @GetMapping("/mypage/{id}")
    public String detail(@PathVariable Long id, Model model, @AuthenticationPrincipal OAuth2User oauth2User) {
        // 본인 글인지 확인하는 로직이 있으면 더 좋음
        FeedbackDTO dto = feedbackMapper.findById(id);
        model.addAttribute("dto", dto);
        model.addAttribute("userName", oauth2User.getAttribute("name"));

        return "detail"; // detail.html (여기서 수정 후 재분석)
    }

    // === 5. ★ 재분석 및 업데이트 (UPDATE) ★ ===
    @PostMapping("/analyze/update")
    public String updateAnalyze(@RequestParam("id") Long id,
                                @RequestParam("topic") String topic,
                                @RequestParam("resumeText") String text,
                                @AuthenticationPrincipal OAuth2User oauth2User,
                                Model model) {

        // AI 다시 분석
        String newFeedback = geminiService.getFeedback(topic, text);

        // DB 업데이트 (UPDATE)
        FeedbackDTO dto = new FeedbackDTO(id, oauth2User.getAttribute("name"), topic, text, newFeedback, null);
        feedbackMapper.updateFeedback(dto);

        // 다시 상세 화면으로 (변경된 내용 보여주기)
        return "redirect:/mypage/" + id;
    }
    @GetMapping("/login")
    public String login() {
        return "login"; // templates/login.html을 보여줘라
    }
}