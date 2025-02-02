package com.example.couponservice.service.v1;

import com.example.couponservice.config.UserIdInterceptor;
import com.example.couponservice.dto.v1.CouponDto;
import com.example.couponservice.entity.Coupon;
import com.example.couponservice.entity.CouponPolicy;
import com.example.couponservice.enums.CouponStatus;
import com.example.couponservice.exception.CouponIssueException;
import com.example.couponservice.exception.CouponNotFoundException;
import com.example.couponservice.repository.CouponPolicyRepository;
import com.example.couponservice.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponPolicyRepository couponPolicyRepository;

    private String generateCouponCode() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @Transactional
    public Coupon issueCoupon(CouponDto.IssueRequest request) {
        CouponPolicy couponPolicy = couponPolicyRepository.findByIdWithLock(request.getCouponPolicyId())
                .orElseThrow(() -> new CouponIssueException("Coupon policy not found."));

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(couponPolicy.getStartTime()) || now.isAfter(couponPolicy.getEndTime())) {
            throw new CouponIssueException("This is not a coupon issuance period.");
        }

        long issuedCouponCount = couponRepository.countByCouponPolicyId(couponPolicy.getId());
        if (issuedCouponCount >= couponPolicy.getTotalQuantity()) {
            throw new CouponIssueException("All coupons have been issued.");
        }

        Long userId = UserIdInterceptor.getCurrentUserId();
        String couponCode = generateCouponCode();
        Coupon coupon = Coupon.builder()
                .couponPolicy(couponPolicy)
                .userId(userId)
                .couponCode(couponCode)
                .build();

        return couponRepository.save(coupon);
    }

    @Transactional
    public Coupon useCoupon(Long couponId, Long orderId) {
        Long userId = UserIdInterceptor.getCurrentUserId();
        Coupon coupon = couponRepository.findByIdAndUserId(couponId, userId)
                .orElseThrow(() -> new CouponNotFoundException("Coupon not found or access denied."));
        coupon.use(orderId);
        return coupon;
    }

    @Transactional
    public Coupon cancelCoupon(Long couponId) {
        Long userId = UserIdInterceptor.getCurrentUserId();
        Coupon coupon = couponRepository.findByIdAndUserId(couponId, userId)
                .orElseThrow(() -> new CouponNotFoundException("Coupon not found or access denied."));
        coupon.cancel();
        return coupon;
    }

    @Transactional(readOnly = true)
    public List<CouponDto.Response> getCoupons(CouponDto.ListRequest request) {
        Long userId = UserIdInterceptor.getCurrentUserId();
        CouponStatus status = request.getStatus();
        Pageable pageable = PageRequest.of(
                request.getPage() != null ? request.getPage() : 0,
                request.getSize() != null ? request.getSize() : 10);

        return couponRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                userId, status, pageable).stream()
                .map(CouponDto.Response::from)
                .collect(Collectors.toList());
    }
}
