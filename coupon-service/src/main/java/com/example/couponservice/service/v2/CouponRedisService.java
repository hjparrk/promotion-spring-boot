package com.example.couponservice.service.v2;

import com.example.couponservice.config.UserIdInterceptor;
import com.example.couponservice.dto.v1.CouponDto;
import com.example.couponservice.entity.Coupon;
import com.example.couponservice.entity.CouponPolicy;
import com.example.couponservice.exception.CouponIssueException;
import com.example.couponservice.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponRedisService {

    private final RedissonClient redissonClient;
    private final CouponRepository couponRepository;
    private final CouponPolicyService couponPolicyService;

    private static final String COUPON_QUANTITY_KEY = "coupon:quantity:";
    private static final String COUPON_LOCK_KEY = "coupon:lock:";
    private static final long LOCK_WAIT_TIME = 3;
    private static final long LOCK_LEASE_TIME = 5;

    @Transactional
    public Coupon issueCoupon(CouponDto.IssueRequest request) {
        String quantityKey = COUPON_QUANTITY_KEY + request.getCouponPolicyId();
        String lockKey = COUPON_LOCK_KEY + request.getCouponPolicyId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean isLocked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new CouponIssueException("Too many coupon issuance requests. Please try again later.");
            }

            CouponPolicy couponPolicy = couponPolicyService.getCouponPolicy(request.getCouponPolicyId());

            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(couponPolicy.getStartTime()) || now.isAfter(couponPolicy.getEndTime())) {
                throw new IllegalStateException("Coupon issuance is not available during this period.");
            }

            // Check and decrease quantity
            RAtomicLong atomicQuantity = redissonClient.getAtomicLong(quantityKey);
            long remainingQuantity = atomicQuantity.decrementAndGet();

            if (remainingQuantity < 0) {
                atomicQuantity.incrementAndGet();
                throw new CouponIssueException("All coupons have been issued.");
            }

            // Issue coupon
            return couponRepository.save(Coupon.builder()
                    .couponPolicy(couponPolicy)
                    .userId(UserIdInterceptor.getCurrentUserId())
                    .couponCode(generateCouponCode())
                    .build());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CouponIssueException("An error occurred while issuing the coupon.");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private String generateCouponCode() {
        return java.util.UUID.randomUUID().toString().substring(0, 8);
    }
}
