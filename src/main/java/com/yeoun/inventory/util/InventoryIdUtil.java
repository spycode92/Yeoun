package com.yeoun.inventory.util;

public class InventoryIdUtil {
	// ID 생성
	// maxId : 오늘 날짜의 최대 seq 조회
	public static String generateId(String maxId, String prefix, String date) {
		int nextSeq = 1;
		
		// id가 존재할 경우 1씩 증가
		if (maxId != null) {
			String seqStr = maxId.substring(maxId.lastIndexOf("-") + 1);
			nextSeq = Integer.parseInt(seqStr) + 1;
		}
		
		String seqStr = String.format("%04d", nextSeq);
		
		return prefix + date + "-" + seqStr;
	}
}
