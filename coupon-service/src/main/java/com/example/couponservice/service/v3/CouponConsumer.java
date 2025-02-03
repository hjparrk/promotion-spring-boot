package com.example.couponservice.service.v3;

import com.example.couponservice.dto.v3.CouponDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponConsumer {
    private final CouponService couponService;
    private static final String TOPIC = "coupon-issue-requests";
    private static final String GROUP_ID = "coupon-service";
    private static final String CONTAINER_FACTORY = "couponKafkaListenerContainerFactory";

    @KafkaListener(topics = TOPIC, groupId = GROUP_ID, containerFactory = CONTAINER_FACTORY)
    public void consumeCouponIssueRequest(CouponDto.IssueMessage message, Acknowledgment ack) {
        try {
            log.info("Received coupon issue request: {}", message);
            couponService.issueCoupon(message);

            // Commit the offset
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process coupon issue request: {}", e.getMessage(), e);
        }
    }
}