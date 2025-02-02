package com.example.couponservice.controller.v1;

import com.example.couponservice.dto.v1.CouponPolicyDto;
import com.example.couponservice.service.v1.CouponPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/coupon-policies")
@RequiredArgsConstructor
public class CouponPolicyController {

    private final CouponPolicyService couponPolicyService;

    @PostMapping
    public ResponseEntity<CouponPolicyDto.Response> createCouponPolicy(@RequestBody CouponPolicyDto.CreateRequest request) {
        CouponPolicyDto.Response response = couponPolicyService.createCouponPolicy(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CouponPolicyDto.Response> getCouponPolicy(@PathVariable Long id) {
        return ResponseEntity.ok(couponPolicyService.getCouponPolicy(id));
    }

    @GetMapping
    public ResponseEntity<List<CouponPolicyDto.Response>> getAllCouponPolicies() {
        return ResponseEntity.ok(couponPolicyService.getAllCouponPolicies());
    }
}
