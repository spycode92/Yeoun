package com.yeoun.inventory.specification;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.yeoun.inventory.entity.Inventory;
import com.yeoun.masterData.entity.MaterialMst;
import com.yeoun.masterData.entity.ProductMst;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

public class InventorySpecs {
	
    public static Specification<Inventory> lotNoContains(String lotNo) {
        return (root, query, cb) ->
            (lotNo == null || lotNo.isBlank())
                ? null
                : cb.like(root.get("lotNo"), "%" + lotNo + "%");
    }

    public static Specification<Inventory> statusEq(String status) {
        return (root, query, cb) ->
            (status == null || status.isBlank())
                ? null
                : cb.equal(root.get("ivStatus"), status);
    }

    public static Specification<Inventory> itemTypeEq(String itemType) {
        return (root, query, cb) ->
            (itemType == null || itemType.isBlank())
                ? null
                : cb.equal(root.get("itemType"), itemType);
    }

    public static Specification<Inventory> zoneEq(String zone) {
        return (root, query, cb) ->
            (zone == null || zone.isBlank())
                ? null
                : cb.equal(root.join("warehouseLocation", JoinType.LEFT).get("zone"), zone);
    }

    public static Specification<Inventory> rackEq(String rack) {
        return (root, query, cb) ->
            (rack == null || rack.isBlank())
                ? null
                : cb.equal(root.join("warehouseLocation", JoinType.LEFT).get("rack"), rack);
    }

    public static Specification<Inventory> ibDateGoe(LocalDateTime from) {
        return (root, query, cb) ->
            (from == null) ? null : cb.greaterThanOrEqualTo(root.get("ibDate"), from);
    }

    public static Specification<Inventory> ibDateLoe(LocalDateTime to) {
        return (root, query, cb) ->
            (to == null) ? null : cb.lessThanOrEqualTo(root.get("ibDate"), to);
    }

    public static Specification<Inventory> expirationDateGoe(LocalDateTime from) {
        return (root, query, cb) ->
            (from == null) ? null : cb.greaterThanOrEqualTo(root.get("expirationDate"), from);
    }

    public static Specification<Inventory> expirationDateLoe(LocalDateTime to) {
        return (root, query, cb) ->
            (to == null) ? null : cb.lessThanOrEqualTo(root.get("expirationDate"), to);
    }

    // 상품명 검색 (RAW/SUB/FG 공통) – 단순 OR LIKE 예시
    public static Specification<Inventory> prodNameContains(String prodName) {
        return (root, query, cb) -> {
            if (prodName == null || prodName.isBlank()) return null;

            Join<Inventory, MaterialMst> matJoin =
                root.join("materialMst", JoinType.LEFT);
            Join<Inventory, ProductMst> prdJoin =
                root.join("productMst", JoinType.LEFT);

            return cb.or(
                cb.like(matJoin.get("matName"), "%" + prodName + "%"),
                cb.like(prdJoin.get("prdName"), "%" + prodName + "%")
            );
        };
    }
}
