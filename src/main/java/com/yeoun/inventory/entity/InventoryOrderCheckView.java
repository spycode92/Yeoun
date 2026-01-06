package com.yeoun.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "V_INVENTORY_ORDER_CHECK")
public class InventoryOrderCheckView {
	@Id
    @Column(name = "ITEM_ID")
    private String itemId;

    @Column(name = "ITEM_NAME")
    private String itemName;

    @Column(name = "EXPECT_IV_QTY")
    private Long expectIvQty;

    @Column(name = "PRODUCT_PLAN_QTY")
    private Long productPlanQty;

    @Column(name = "OUTBOUND_PLAN_QTY")
    private Long outboundPlanQty;

    @Column(name = "SAFETY_QTY")
    private Long safetyQty;

    @Column(name = "EXPECT_IB_QTY")
    private Long expectIbQty;
    
    @Column(name = "ITEM_UNIT")
    private String itemUnit;
}
