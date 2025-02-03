package com.example.couponservice.service.v3;

import com.example.couponservice.config.UserIdInterceptor;
import com.example.couponservice.dto.v3.CouponDto;
import com.example.couponservice.entity.Coupon;
import com.example.couponservice.entity.CouponPolicy;
import com.example.couponservice.exception.CouponIssueException;
import com.example.couponservice.exception.CouponNotFoundException;
import com.example.couponservice.repository.CouponRepository;
import com.example.couponservice.service.v2.CouponPolicyService;
import com.example.couponservice.service.v2.CouponStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service("couponServiceV3")
@RequiredArgsConstructor
public class CouponService {
    private static final String COUPON_QUANTITY_KEY = "coupon:quantity:";
    private static final String COUPON_LOCK_KEY = "coupon:lock:";
    private static final long LOCK_WAIT_TIME = 3L;
    private static final long LOCK_LEASE_TIME = 5L;

    private final RedissonClient redissonClient;
    private final CouponRepository couponRepository;
    private final CouponProducer couponProducer;
    private final CouponStateService couponStateService;
    private final CouponPolicyService couponPolicyService;

    public void requestCouponIssue(CouponDto.IssueRequest request) {
        String quantityKey = COUPON_QUANTITY_KEY + request.getCouponPolicyId();
        String lockKey = COUPON_LOCK_KEY + request.getCouponPolicyId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean isLocked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new CouponIssueException("Too many coupon issuance requests. Please try again later.");
            }

            CouponPolicy couponPolicy = couponPolicyService.getCouponPolicy(request.getCouponPolicyId());
            if (couponPolicy == null) {
                throw new IllegalArgumentException("Coupon policy not found.");
            }

            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(couponPolicy.getStartTime()) || now.isAfter(couponPolicy.getEndTime())) {
                throw new IllegalStateException("Coupon issuance is not available during this period.");
            }

            // 수량 체크 및 감소
            RAtomicLong atomicQuantity = redissonClient.getAtomicLong(quantityKey);
            long remainingQuantity = atomicQuantity.decrementAndGet();

            if (remainingQuantity < 0) {
                atomicQuantity.incrementAndGet();
                throw new CouponIssueException("All coupons have been issued.");
            }

            // Send coupon issuance request message via Kafka
            couponProducer.sendCouponIssueRequest(
                    CouponDto.IssueMessage.builder()
                            .policyId(request.getCouponPolicyId())
                            .userId(UserIdInterceptor.getCurrentUserId())
                            .build()
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CouponIssueException("An error occurred while issuing the coupon.");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

    }

    @Transactional
    public void issueCoupon(CouponDto.IssueMessage message) {
        try {
            CouponPolicy policy = couponPolicyService.getCouponPolicy(message.getPolicyId());
            if (policy == null) {
                throw new IllegalArgumentException("Coupon policy not found.");
            }

            Coupon coupon = couponRepository.save(Coupon.builder()
                    .couponPolicy(policy)
                    .userId(message.getUserId())
                    .couponCode(generateCouponCode())
                    .build());

            log.info("Coupon issued successfully: policyId={}, userId={}", message.getPolicyId(), message.getUserId());

        } catch (Exception e) {
            log.error("Failed to issue coupon: {}", e.getMessage());
            throw e;
        }
    }

    @Transactional
    public Coupon useCoupon(Long couponId, Long orderId) {
        Coupon coupon = couponRepository.findByIdWithLock(couponId)
                .orElseThrow(() -> new CouponNotFoundException("Coupon not found or access denied."));

        coupon.use(orderId);
        couponStateService.updateCouponState(coupon);

        return coupon;
    }

    @Transactional
    public Coupon cancelCoupon(Long couponId) {
        Coupon coupon = couponRepository.findByIdAndUserId(couponId, UserIdInterceptor.getCurrentUserId())
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found or access denied."));

        if (!coupon.isUsed()) {
            throw new IllegalStateException("Unused coupons cannot be canceled.");
        }

        coupon.cancel();
        couponStateService.updateCouponState(coupon);

        return coupon;
    }

    private String generateCouponCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
