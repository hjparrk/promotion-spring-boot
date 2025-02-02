package com.example.couponservice.service.v1;

import com.example.couponservice.dto.v1.CouponPolicyDto;
import com.example.couponservice.exception.CouponPolicyNotFoundException;
import com.example.couponservice.repository.CouponPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponPolicyService {

    private final CouponPolicyRepository couponPolicyRepository;

    @Transactional
    public CouponPolicyDto.Response createCouponPolicy(
            CouponPolicyDto.CreateRequest request
    ) {
        return CouponPolicyDto.Response.from(couponPolicyRepository.save(request.toEntity()));
    }


    @Transactional(readOnly = true)
    public CouponPolicyDto.Response getCouponPolicy(Long id) {
        return couponPolicyRepository.findById(id)
                .map(CouponPolicyDto.Response::from)
                .orElseThrow(() -> new CouponPolicyNotFoundException("Coupon policy not found."));
    }

    @Transactional(readOnly = true)
    public List<CouponPolicyDto.Response> getAllCouponPolicies() {
        return couponPolicyRepository.findAll().stream()
                .map(CouponPolicyDto.Response::from)
                .collect(Collectors.toList());
    }
}
