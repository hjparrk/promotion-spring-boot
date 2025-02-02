package com.example.couponservice.service.v1;

import com.example.couponservice.config.UserIdInterceptor;
import com.example.couponservice.dto.v1.CouponDto;
import com.example.couponservice.entity.Coupon;
import com.example.couponservice.entity.CouponPolicy;
import com.example.couponservice.enums.CouponStatus;
import com.example.couponservice.enums.DiscountType;
import com.example.couponservice.exception.CouponNotFoundException;
import com.example.couponservice.repository.CouponPolicyRepository;
import com.example.couponservice.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @InjectMocks
    private CouponService couponService;

    @Mock
    private CouponRepository couponRepository;
    @Mock
    private CouponPolicyRepository couponPolicyRepository;

    private CouponPolicy couponPolicy;
    private Coupon coupon;
    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_COUPON_ID = 1L;
    private static final Long TEST_ORDER_ID = 1L;

    @BeforeEach
    void setUp() {
        couponPolicy = CouponPolicy.builder()
                .id(1L)
                .name("테스트 쿠폰")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(1000)
                .minimumOrderAmount(10000)
                .maximumDiscountAmount(1000)
                .totalQuantity(100)
                .startTime(LocalDateTime.now().minusDays(1))
                .endTime(LocalDateTime.now().plusDays(1))
                .build();

        coupon = Coupon.builder()
                .id(TEST_COUPON_ID)
                .userId(TEST_USER_ID)
                .couponPolicy(couponPolicy)
                .couponCode("TEST123")
                .build();
    }

    @Test
    @DisplayName("Issue Coupon Success")
    void issueCoupon_Success() {
        // Given
        CouponDto.IssueRequest request = CouponDto.IssueRequest.builder()
                .couponPolicyId(1L)
                .build();

        when(couponPolicyRepository.findByIdWithLock(any())).thenReturn(Optional.of(couponPolicy));
        when(couponRepository.countByCouponPolicyId(any())).thenReturn(0L);
        when(couponRepository.save(any())).thenReturn(coupon);

        try (MockedStatic<UserIdInterceptor> mockedStatic = mockStatic(UserIdInterceptor.class)) {
            mockedStatic.when(UserIdInterceptor::getCurrentUserId).thenReturn(TEST_USER_ID);

            // When
            CouponDto.Response response = couponService.issueCoupon(request);

            // Then
            assertThat(response.getId()).isEqualTo(TEST_COUPON_ID);
            assertThat(response.getUserId()).isEqualTo(TEST_USER_ID);
            verify(couponRepository).save(any());
        }
    }

    @Test
    @DisplayName("Use Coupon Success")
    void useCoupon_Success() {
        // Given
        when(couponRepository.findByIdAndUserId(TEST_COUPON_ID, TEST_USER_ID))
                .thenReturn(Optional.of(coupon));

        try (MockedStatic<UserIdInterceptor> mockedStatic = mockStatic(UserIdInterceptor.class)) {
            mockedStatic.when(UserIdInterceptor::getCurrentUserId).thenReturn(TEST_USER_ID);

            // When
            CouponDto.Response response = couponService.useCoupon(TEST_COUPON_ID, TEST_ORDER_ID);

            // Then
            assertThat(response.getId()).isEqualTo(TEST_COUPON_ID);
            assertThat(response.getOrderId()).isEqualTo(TEST_ORDER_ID);
            assertThat(response.getStatus()).isEqualTo(CouponStatus.USED);
        }
    }

    @Test
    @DisplayName("Use Coupon Fail - Not Found Or Unauthorized")
    void useCoupon_Fail_NotFoundOrUnauthorized() {
        // Given
        when(couponRepository.findByIdAndUserId(TEST_COUPON_ID, TEST_USER_ID))
                .thenReturn(Optional.empty());

        try (MockedStatic<UserIdInterceptor> mockedStatic = mockStatic(UserIdInterceptor.class)) {
            mockedStatic.when(UserIdInterceptor::getCurrentUserId).thenReturn(TEST_USER_ID);

            // When & Then
            assertThatThrownBy(() -> couponService.useCoupon(TEST_COUPON_ID, TEST_ORDER_ID))
                    .isInstanceOf(CouponNotFoundException.class)
                    .hasMessage("Coupon not found or access denied.");
        }
    }

    @Test
    @DisplayName("Cancel Coupon Success")
    void cancelCoupon_Success() {
        // Given
        coupon.use(TEST_ORDER_ID); // Set coupon as USED first
        when(couponRepository.findByIdAndUserId(TEST_COUPON_ID, TEST_USER_ID))
                .thenReturn(Optional.of(coupon));

        try (MockedStatic<UserIdInterceptor> mockedStatic = mockStatic(UserIdInterceptor.class)) {
            mockedStatic.when(UserIdInterceptor::getCurrentUserId).thenReturn(TEST_USER_ID);

            // When
            CouponDto.Response response = couponService.cancelCoupon(TEST_COUPON_ID);

            // Then
            assertThat(response.getId()).isEqualTo(TEST_COUPON_ID);
            assertThat(response.getStatus()).isEqualTo(CouponStatus.CANCELLED);
        }
    }

    @Test
    @DisplayName("Cancel Coupon Fail - Not Found Or Unauthorized")
    void cancelCoupon_Fail_NotFoundOrUnauthorized() {
        // Given
        when(couponRepository.findByIdAndUserId(TEST_COUPON_ID, TEST_USER_ID))
                .thenReturn(Optional.empty());

        try (MockedStatic<UserIdInterceptor> mockedStatic = mockStatic(UserIdInterceptor.class)) {
            mockedStatic.when(UserIdInterceptor::getCurrentUserId).thenReturn(TEST_USER_ID);

            // When & Then
            assertThatThrownBy(() -> couponService.cancelCoupon(TEST_COUPON_ID))
                    .isInstanceOf(CouponNotFoundException.class)
                    .hasMessage("Coupon not found or access denied.");
        }
    }

    @Test
    @DisplayName("Get Coupons List Success")
    void getCoupons_Success() {
        // Given
        List<Coupon> coupons = List.of(coupon);
        Page<Coupon> couponPage = new PageImpl<>(coupons);
        when(couponRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                eq(TEST_USER_ID), any(), any(PageRequest.class))).thenReturn(couponPage);

        CouponDto.ListRequest request = CouponDto.ListRequest.builder()
                .status(CouponStatus.AVAILABLE)
                .page(0)
                .size(10)
                .build();

        try (MockedStatic<UserIdInterceptor> mockedStatic = mockStatic(UserIdInterceptor.class)) {
            mockedStatic.when(UserIdInterceptor::getCurrentUserId).thenReturn(TEST_USER_ID);

            // When
            List<CouponDto.Response> responses = couponService.getCoupons(request);

            // Then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getId()).isEqualTo(TEST_COUPON_ID);
            assertThat(responses.get(0).getUserId()).isEqualTo(TEST_USER_ID);
        }
    }
}