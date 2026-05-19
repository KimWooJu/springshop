package com.springshop.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Locale;
import java.util.Map;

/**
 * 요청 Locale 설정 인터셉터.
 *
 * <p>{@code Accept-Language} 헤더를 파싱하여 지원 로케일로 매핑하고,
 * {@link LocaleContextHolder}에 설정한다. 지원하지 않는 언어는 기본값(ko-KR)을 사용한다.
 *
 * <p>지원 로케일: ko-KR (한국어), en-US (영어), ja-JP (일본어), zh-CN (중국어 간체)
 */
@Component
@Slf4j
public class LocaleInterceptor implements HandlerInterceptor {

    private static final Locale DEFAULT_LOCALE = Locale.KOREA;

    /** 언어 태그 → 지원 Locale 매핑 */
    private static final Map<String, Locale> SUPPORTED_LOCALES = Map.of(
            "ko", Locale.KOREA,
            "ko-KR", Locale.KOREA,
            "en", Locale.US,
            "en-US", Locale.US,
            "ja", Locale.JAPAN,
            "ja-JP", Locale.JAPAN,
            "zh", Locale.SIMPLIFIED_CHINESE,
            "zh-CN", Locale.SIMPLIFIED_CHINESE
    );

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        Locale locale = resolveLocale(request);
        LocaleContextHolder.setLocale(locale);

        log.debug("로케일 설정 - Accept-Language={}, resolved={}",
                request.getHeader("Accept-Language"), locale);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        LocaleContextHolder.resetLocaleContext();
    }

    /**
     * Accept-Language 헤더를 파싱하여 지원 로케일을 결정한다.
     * q-value 기반 우선순위는 단순화하여 첫 번째 일치를 사용한다.
     */
    private Locale resolveLocale(HttpServletRequest request) {
        String acceptLanguage = request.getHeader("Accept-Language");
        if (!StringUtils.hasText(acceptLanguage)) {
            return DEFAULT_LOCALE;
        }

        // "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7" 형식 파싱
        for (String tag : acceptLanguage.split(",")) {
            String cleanTag = tag.split(";")[0].trim();
            Locale matched = SUPPORTED_LOCALES.get(cleanTag);
            if (matched != null) {
                return matched;
            }
            // 기본 언어만으로 재시도 (예: "en-GB" → "en")
            if (cleanTag.contains("-")) {
                String baseTag = cleanTag.split("-")[0];
                matched = SUPPORTED_LOCALES.get(baseTag);
                if (matched != null) return matched;
            }
        }
        return DEFAULT_LOCALE;
    }
}
