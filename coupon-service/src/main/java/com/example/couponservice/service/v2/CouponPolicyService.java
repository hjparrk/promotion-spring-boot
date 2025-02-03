package com.example.couponservice.service.v2;

import com.example.couponservice.dto.v1.CouponPolicyDto;
import com.example.couponservice.entity.CouponPolicy;
import com.example.couponservice.exception.CouponPolicyNotFoundException;
import com.example.couponservice.repository.CouponPolicyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service("couponPolicyServiceV2")
@RequiredArgsConstructor
public class CouponPolicyService {

    private final CouponPolicyRepository couponPolicyRepository;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    private static final String COUPON_QUANTITY_KEY = "coupon:quantity:";
    private static final String COUPON_POLICY_KEY = "coupon:policy:";


    @Transactional
    public CouponPolicy createCouponPolicy(
            CouponPolicyDto.CreateRequest request
    ) throws JsonProcessingException {
        // Save the policy to DB
        CouponPolicy couponPolicy = request.toEntity();
        CouponPolicy savedPolicy = couponPolicyRepository.save(couponPolicy);

        // Set the initial quantity
        String quantityKey = COUPON_QUANTITY_KEY + savedPolicy.getId();
        RAtomicLong atomicQuantity = redissonClient.getAtomicLong(quantityKey);
        atomicQuantity.set(savedPolicy.getTotalQuantity());

        // Save the policy to redis
        String policyKey = COUPON_POLICY_KEY + savedPolicy.getId();
        String policyJson = objectMapper.writeValueAsString(CouponPolicyDto.Response.from(savedPolicy));
        RBucket<String> bucket = redissonClient.getBucket(policyKey);
        bucket.set(policyJson);

        return savedPolicy;
    }

    @Transactional(readOnly = true)
    public CouponPolicy getCouponPolicy(Long id) {
        String policyKey = COUPON_POLICY_KEY + id;
        RBucket<String> bucket = redissonClient.getBucket(policyKey);
        String policyJson = bucket.get();

        if (policyJson != null) {
            return parseJson(policyJson);
        }

        return couponPolicyRepository.findById(id)
                .orElseThrow(() -> new CouponPolicyNotFoundException("Coupon policy not found."));
    }

    @Transactional(readOnly = true)
    public List<CouponPolicy> getAllCouponPolicies() {
        return couponPolicyRepository.findAll();
    }

    private CouponPolicy parseJson(String policyJson) {
        try {
            return objectMapper.readValue(policyJson, CouponPolicy.class);
        } catch (JsonProcessingException e) {
            log.error("Error parsing coupon policy JSON.", e);
            return null;
        }
    }


}
