package com.example.couponservice.exception;

public class CouponNotFoundException extends RuntimeException {
    public CouponNotFoundException(String message) {
        super(message);
    }

    public CouponNotFoundException(Long couponId) {
        super("Coupon not found: " + couponId);
    }
}