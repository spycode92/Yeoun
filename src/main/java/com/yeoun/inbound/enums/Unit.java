package com.yeoun.inbound.enums;

// 단위 변환 enum
public enum Unit {
	G("g", 1L),
	KG("g", 1_000L),
	ML("ml", 1L),
	L("ml", 1_000L),
	EA("EA", 1L);
	
	private final String baseUnit;
	private final long factor;
	
	Unit(String baseUnit, long factor) {
		this.baseUnit = baseUnit;
		this.factor = factor;
	}
	
	public long toBase(long value) {
		return value * factor;
	}
	
	public String getBaseUint() {
		return baseUnit;
	}
	
	public static Unit from(String unit) {
		return Unit.valueOf(unit.trim().toUpperCase());
	}
}
