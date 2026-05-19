package com.springshop.domain.notification;

import com.springshop.domain.base.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 알림 템플릿 엔티티.
 *
 * <p>알림 종류(type)별로 제목과 본문 템플릿을 관리한다. {@code {{변수명}}} 형식의 자리표시자를
 * 사용하며 렌더링 시 사용자/주문/상품 데이터로 치환된다. 운영자가 UI 에서 직접 수정 가능하며,
 * 활성 템플릿만 알림 발송에 사용된다.</p>
 *
 * <p>변수 목록(variables)을 별도 칼럼에 저장하여 운영자가 사용 가능한 자리표시자를 쉽게
 * 확인할 수 있도록 한다.</p>
 *
 * @author SpringShop Domain Team
 */
@Entity
@Table(
        name = "notification_templates",
        uniqueConstraints = @UniqueConstraint(name = "uk_notif_template_type", columnNames = "type"),
        indexes = {
                @Index(name = "idx_notif_template_active", columnList = "is_active")
        }
)
public class NotificationTemplate extends BaseAuditEntity {

    @Column(name = "type", length = 50, nullable = false)
    private String type;

    @Column(name = "title_template", length = 500, nullable = false)
    private String titleTemplate;

    @Column(name = "content_template", columnDefinition = "TEXT", nullable = false)
    private String contentTemplate;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /**
     * 콤마로 구분된 변수명 목록.
     */
    @Column(name = "variables", length = 1000)
    private String variablesCsv;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "channel", length = 30)
    private String channel = "WEB";

    protected NotificationTemplate() {
        super();
    }

    private NotificationTemplate(String type, String titleTemplate, String contentTemplate) {
        super();
        this.type = Objects.requireNonNull(type, "type 필수");
        this.titleTemplate = Objects.requireNonNull(titleTemplate, "titleTemplate 필수");
        this.contentTemplate = Objects.requireNonNull(contentTemplate, "contentTemplate 필수");
        this.isActive = true;
    }

    public static NotificationTemplate create(String type, String titleTemplate, String contentTemplate) {
        return new NotificationTemplate(type, titleTemplate, contentTemplate);
    }

    /**
     * 주문 확인 예시 (text block 활용).
     */
    public static NotificationTemplate sampleOrderConfirmation() {
        String title = "[{{userName}}님] 주문이 확인되었습니다";
        String content = """
                안녕하세요 {{userName}}님,

                주문번호 {{orderNumber}} 주문이 확인되었습니다.

                  - 결제금액: {{amount}}원
                  - 결제수단: {{paymentMethod}}
                  - 배송지: {{address}}

                감사합니다.
                SpringShop 드림.
                """;
        NotificationTemplate t = new NotificationTemplate("ORDER_CONFIRMED", title, content);
        t.updateVariables(List.of("userName", "orderNumber", "amount", "paymentMethod", "address"));
        return t;
    }

    /**
     * 재고 알림 예시.
     */
    public static NotificationTemplate sampleStockAlert() {
        String title = "[재고 부족] {{productName}}";
        String content = """
                {{productName}} 상품의 재고가 {{remaining}}개 남았습니다.

                추가 입고 또는 알림 발송 검토가 필요합니다.
                """;
        NotificationTemplate t = new NotificationTemplate("STOCK_ALERT", title, content);
        t.updateVariables(List.of("productName", "remaining"));
        return t;
    }

    /**
     * 템플릿 렌더링. variables 의 키를 자리표시자로 치환한다.
     *
     * @return key="title"/"content" 의 결과 맵
     */
    public Map<String, String> render(Map<String, String> variables) {
        Map<String, String> safeVars = variables == null ? Collections.emptyMap() : variables;
        String renderedTitle = renderOne(titleTemplate, safeVars);
        String renderedContent = renderOne(contentTemplate, safeVars);
        Map<String, String> result = new LinkedHashMap<>();
        result.put("title", renderedTitle);
        result.put("content", renderedContent);
        return result;
    }

    private String renderOne(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() == null ? "" : entry.getValue();
            result = result.replace(placeholder, value);
        }
        return result;
    }

    /**
     * 사용 가능한 변수 목록.
     */
    public List<String> getVariables() {
        if (variablesCsv == null || variablesCsv.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(variablesCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    /**
     * 변수 목록 업데이트.
     */
    public void updateVariables(List<String> variables) {
        if (variables == null || variables.isEmpty()) {
            this.variablesCsv = null;
            return;
        }
        this.variablesCsv = String.join(",", variables);
    }

    /**
     * 템플릿 본문 검증: 필수 변수가 모두 포함되어 있는지.
     */
    public Map<String, Boolean> validatePlaceholders() {
        Map<String, Boolean> result = new HashMap<>();
        for (String variable : getVariables()) {
            String placeholder = "{{" + variable + "}}";
            result.put(variable, titleTemplate.contains(placeholder) || contentTemplate.contains(placeholder));
        }
        return result;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void updateTemplates(String titleTemplate, String contentTemplate) {
        this.titleTemplate = Objects.requireNonNull(titleTemplate, "titleTemplate 필수");
        this.contentTemplate = Objects.requireNonNull(contentTemplate, "contentTemplate 필수");
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void updateChannel(String channel) {
        this.channel = channel;
    }

    public String getType() {
        return type;
    }

    public String getTitleTemplate() {
        return titleTemplate;
    }

    public String getContentTemplate() {
        return contentTemplate;
    }

    public boolean isActive() {
        return isActive;
    }

    public String getDescription() {
        return description;
    }

    public String getChannel() {
        return channel;
    }
}
