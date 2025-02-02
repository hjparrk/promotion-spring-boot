package com.example.couponservice.exception;


public class CouponAlreadyUsedException extends RuntimeException {
    public CouponAlreadyUsedException(String message) {
        super(message);
    }

    public CouponAlreadyUsedException(Long couponId) {
        super("Already used coupon: " + couponId);
    }
}