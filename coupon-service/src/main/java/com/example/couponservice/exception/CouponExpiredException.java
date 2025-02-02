package com.example.couponservice.exception;

public class CouponExpiredException extends RuntimeException {
    public CouponExpiredException(String message) {
        super(message);
    }

    public CouponExpiredException(Long couponId) {
        super("Expired Coupon: " + couponId);
    }
}