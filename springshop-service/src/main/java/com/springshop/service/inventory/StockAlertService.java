package com.springshop.service.inventory;

import com.springshop.domain.inventory.Inventory;

import java.util.List;

/**
 * 재고 알림 서비스.
 *
 * <p>임계값 미만 재고에 대해 운영자/관리자에게 알림을 발송한다. 알림 채널은
 * 이메일/슬랙/푸시 등 복합 채널이며, 동일 인벤토리에 대한 알림 폭주를 막기 위해
 * 짧은 윈도우 동안 중복 알림을 억제한다.</p>
 */
public interface StockAlertService {

    /**
     * 임계값 이하 재고 알림 (WARN 레벨).
     */
    void sendLowStockAlert(Inventory inventory);

    /**
     * 위급 재고 알림 (CRITICAL 레벨).
     */
    void sendCriticalAlert(Inventory inventory);

    /**
     * 다수 인벤토리에 대한 일괄 알림.
     */
    int sendBatchAlerts(List<Inventory> inventories);

    /**
     * 알림 억제 등록 (운영자가 임계값을 임시 조정한 경우).
     */
    void suppressAlerts(Long inventoryId, long minutes);

    /**
     * 알림 일일 통계 조회.
     */
    AlertStatistics getDailyStatistics();

    /**
     * 알림 통계.
     */
    record AlertStatistics(int totalAlerts, int lowStockAlerts, int criticalAlerts, int suppressed) {}
}
