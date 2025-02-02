package com.example.couponservice.entity;

import com.example.couponservice.enums.CouponStatus;
import com.example.couponservice.exception.CouponAlreadyUsedException;
import com.example.couponservice.exception.CouponExpiredException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "coupons")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_policy_id")
    private CouponPolicy couponPolicy;

    private Long userId;
    private String couponCode;

    @Enumerated(EnumType.STRING)
    private CouponStatus status;

    private Long orderId;
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;

    @Builder
    public Coupon(Long id, CouponPolicy couponPolicy, Long userId, String couponCode) {
        this.id = id;
        this.couponPolicy = couponPolicy;
        this.userId = userId;
        this.couponCode = couponCode;
        this.status = CouponStatus.AVAILABLE;
    }

    public void use(Long orderId) {
        if (status == CouponStatus.USED) {
            throw new CouponAlreadyUsedException("Already used coupon.");
        }

        if (isExpired()) {
            throw new CouponExpiredException("Expired coupon.");
        }

        this.status = CouponStatus.USED;
        this.orderId = orderId;
        this.usedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (status != CouponStatus.USED) {
            throw new IllegalStateException("Un-used coupon.");
        }

        this.status = CouponStatus.CANCELLED;
        this.orderId = null;
        this.usedAt = null;
    }

    public boolean isExpired() {
        LocalDateTime now = LocalDateTime.now();
        return now.isBefore(couponPolicy.getStartTime()) || now.isAfter(couponPolicy.getEndTime());
    }

    public boolean isUsed() {
        return status == CouponStatus.USED;
    }
}