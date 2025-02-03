package com.example.couponservice.service.v2;

import com.example.couponservice.config.UserIdInterceptor;
import com.example.couponservice.dto.v1.CouponDto;
import com.example.couponservice.entity.Coupon;
import com.example.couponservice.entity.CouponPolicy;
import com.example.couponservice.enums.DiscountType;
import com.example.couponservice.exception.CouponIssueException;
import com.example.couponservice.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponRedisServiceTest {

    @InjectMocks
    private CouponRedisService couponRedisService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponPolicyService couponPolicyService;

    @Mock
    private RLock rLock;

    @Mock
    private RAtomicLong atomicLong;

    private CouponPolicy couponPolicy;
    private Coupon coupon;
    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_COUPON_ID = 1L;
    private static final Long TEST_POLICY_ID = 1L;

    @BeforeEach
    void setUp() {
        couponPolicy = CouponPolicy.builder()
                .id(TEST_POLICY_ID)
                .name("Test Coupon")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(1000)
                .minimumOrderAmount(10000)
                .maximumDiscountAmount(1000)
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
    @DisplayName("Issue Coupon success")
    void issueCoupon_Success() throws InterruptedException {
        // Given
        CouponDto.IssueRequest request = CouponDto.IssueRequest.builder()
                .couponPolicyId(TEST_POLICY_ID)
                .build();

        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(redissonClient.getAtomicLong(anyString())).thenReturn(atomicLong);
        when(atomicLong.decrementAndGet()).thenReturn(99L);
        when(couponPolicyService.getCouponPolicy(TEST_POLICY_ID)).thenReturn(couponPolicy);
        when(couponRepository.save(any(Coupon.class))).thenReturn(coupon);

        try (MockedStatic<UserIdInterceptor> mockedStatic = mockStatic(UserIdInterceptor.class)) {
            mockedStatic.when(UserIdInterceptor::getCurrentUserId).thenReturn(TEST_USER_ID);

            // When
            Coupon coupon = couponRedisService.issueCoupon(request);

            // Then
            assertThat(coupon.getId()).isEqualTo(TEST_COUPON_ID);
            assertThat(coupon.getUserId()).isEqualTo(TEST_USER_ID);
            verify(couponRepository).save(any(Coupon.class));
            verify(rLock).unlock();
        }
    }

    @Test
    @DisplayName("Issue Coupon fail - Lock Acquisition Fail")
    void issueCoupon_Fail_LockNotAcquired() throws InterruptedException {
        // Given
        CouponDto.IssueRequest request = CouponDto.IssueRequest.builder()
                .couponPolicyId(TEST_POLICY_ID)
                .build();

        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> couponRedisService.issueCoupon(request))
                .isInstanceOf(CouponIssueException.class)
                .hasMessage("Too many coupon issuance requests. Please try again later.");
    }

    @Test
    @DisplayName("Issue Coupon Fail - No Quantity Left")
    void issueCoupon_Fail_NoQuantityLeft() throws InterruptedException {
        // Given
        CouponDto.IssueRequest request = CouponDto.IssueRequest.builder()
                .couponPolicyId(TEST_POLICY_ID)
                .build();

        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(redissonClient.getAtomicLong(anyString())).thenReturn(atomicLong);
        when(atomicLong.decrementAndGet()).thenReturn(-1L);
        when(couponPolicyService.getCouponPolicy(TEST_POLICY_ID)).thenReturn(couponPolicy);

        // When & Then
        assertThatThrownBy(() -> couponRedisService.issueCoupon(request))
                .isInstanceOf(CouponIssueException.class)
                .hasMessage("All coupons have been issued.");

        verify(atomicLong).incrementAndGet();
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("Issue Coupon Fail - Invalid Issuance Period")
    void issueCoupon_Fail_InvalidPeriod() throws InterruptedException {
        // Given
        CouponDto.IssueRequest request = CouponDto.IssueRequest.builder()
                .couponPolicyId(TEST_POLICY_ID)
                .build();

        CouponPolicy expiredPolicy = CouponPolicy.builder()
                .id(TEST_POLICY_ID)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(2))
                .build();

        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(couponPolicyService.getCouponPolicy(TEST_POLICY_ID)).thenReturn(expiredPolicy);

        // When & Then
        assertThatThrownBy(() -> couponRedisService.issueCoupon(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Coupon issuance is not available during this period.");

        verify(rLock).unlock();
    }

}