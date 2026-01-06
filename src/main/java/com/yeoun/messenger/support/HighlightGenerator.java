package com.yeoun.messenger.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class HighlightGenerator {

    // ====================================================
    // 하이라이트 처리 관련 유틸 함수
    public String create(String text, String keyword) {
        if (text == null || keyword == null) return text;

        // (?i)는 대소문자 무시
        return text.replaceAll("(?i)" + Pattern.quote(keyword),
                "<mark>$0</mark>");
    }
}
