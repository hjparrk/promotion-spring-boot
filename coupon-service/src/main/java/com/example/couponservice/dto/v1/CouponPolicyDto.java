package com.example.couponservice.dto.v1;

import com.example.couponservice.entity.CouponPolicy;
import com.example.couponservice.enums.DiscountType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class CouponPolicyDto {

    @Getter
    @Builder
    public static class CreateRequest {

        @NotBlank(message = "Coupon policy name is required.")
        private String name;

        private String description;

        @NotNull(message = "Discount type is required.")
        private DiscountType discountType;

        @NotNull(message = "Discount value is required.")
        @Min(value = 1, message = "Discount value must be at least 1.")
        private Integer discountValue;

        @NotNull(message = "Minimum order amount is required.")
        @Min(value = 0, message = "Minimum order amount must be at least 0.")
        private Integer minimumOrderAmount;

        @NotNull(message = "Maximum discount amount is required.")
        @Min(value = 1, message = "Maximum discount amount must be at least 1.")
        private Integer maximumDiscountAmount;

        @NotNull(message = "Total quantity is required.")
        @Min(value = 1, message = "Total quantity must be at least 1.")
        private Integer totalQuantity;

        @NotNull(message = "Start time is required.")
        private LocalDateTime startTime;

        @NotNull(message = "End time is required.")
        private LocalDateTime endTime;

        public CouponPolicy toEntity() {
            return CouponPolicy.builder()
                    .name(name)
                    .description(description)
                    .discountType(discountType)
                    .discountValue(discountValue)
                    .minimumOrderAmount(minimumOrderAmount)
                    .maximumDiscountAmount(maximumDiscountAmount)
                    .totalQuantity(totalQuantity)
                    .startTime(startTime)
                    .endTime(endTime)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String name;
        private String description;
        private DiscountType discountType;
        private Integer discountValue;
        private Integer minimumOrderAmount;
        private Integer maximumDiscountAmount;
        private Integer totalQuantity;
        private Integer issuedQuantity;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(CouponPolicy couponPolicy) {
            return Response.builder()
                    .id(couponPolicy.getId())
                    .name(couponPolicy.getName())
                    .description(couponPolicy.getDescription())
                    .discountType(couponPolicy.getDiscountType())
                    .discountValue(couponPolicy.getDiscountValue())
                    .minimumOrderAmount(couponPolicy.getMinimumOrderAmount())
                    .maximumDiscountAmount(couponPolicy.getMaximumDiscountAmount())
                    .totalQuantity(couponPolicy.getTotalQuantity())
                    .startTime(couponPolicy.getStartTime())
                    .endTime(couponPolicy.getEndTime())
                    .createdAt(couponPolicy.getCreatedAt())
                    .updatedAt(couponPolicy.getUpdatedAt())
                    .build();
        }
    }
}
