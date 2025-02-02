package com.example.couponservice.repository;

import com.example.couponservice.entity.Coupon;
import com.example.couponservice.entity.CouponPolicy;
import com.example.couponservice.enums.CouponStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT COUNT(c) FROM Coupon c WHERE c.couponPolicy.id = :policyId")
    Long countByCouponPolicyId(@Param("policyId") Long policyId);

    Page<Coupon> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, CouponStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.id = :id")
    Optional<CouponPolicy> findByIdWithLock(Long id);
}
