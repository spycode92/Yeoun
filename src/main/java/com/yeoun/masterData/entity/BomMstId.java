package com.yeoun.masterData.entity;

import java.io.Serializable;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BomMstId implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String prdId; //제품id
	
	private String matId; //원재료id
	
	public BomMstId() {
    }
	
		// 2. 💡 필수: equals() 메서드 재정의
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			BomMstId that = (BomMstId) o;
			return Objects.equals(prdId, that.prdId) && 
			       Objects.equals(matId, that.matId);
		}

		// 3. 💡 필수: hashCode() 메서드 재정의
		@Override
		public int hashCode() {
			return Objects.hash(prdId, matId);
		}
}
