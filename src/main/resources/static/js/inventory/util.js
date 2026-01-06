// 단위 환산 기준
const UNIT_CONVERSION_MAP = {
	"KG": 1000,
	"L": 1000 
}

// 발주 단위에서 BOM에서 사용되는 단위로 변환
function convertToBaseUnit(qty, unit) {
	if (!unit || !qty) return qty;
	
	// 단위를 대소문자 구분없이 처리하기 위해 소문자로 변환
	const safeUnit = unit.toUpperCase().trim();
	
	const reuslt = UNIT_CONVERSION_MAP[safeUnit];
	
	if (reuslt) {
		return qty * reuslt;
	} else { // 변환 불가 시 원래 값 반환
		return qty;
	}
}

// 기본 단위에서 발주 단위로 변환
function convertFromBaseUnit(baseQty, targetUnit) {
	const result = UNIT_CONVERSION_MAP[targetUnit];
	
	if (result) {
		return baseQty / result;
	} else {
		return baseQty;
	}
}

// ==================================================
// 스피너 보이기 끄기
function showSpinner() {
	document.getElementById('loading-overlay').style.display = 'flex';
}
function hideSpinner() {
	document.getElementById('loading-overlay').style.display = 'none';
}