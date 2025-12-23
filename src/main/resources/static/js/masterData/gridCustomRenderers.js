/**
 * tui ui 수정스타일변경과 영어표기를 따로 만든 클래스
 */
//전자결재
class StatusBadgeRenderer {
  constructor(props) {
    const el = document.createElement('div');
    el.style.textAlign = 'center'; // 가운데 정렬
    
    this.el = el;
    this.render(props);
  }

  getElement() {
    return this.el;
  }

  render(props) {
    const value = String(props.value || '');
    let style = { bg: '#F1F3F5', text: '#868E96' }; // 기본 회색

    // '대기'라는 단어만 포함되어 있으면 무조건 파랑 적용
    if (value.includes('대기')) {
      style = { bg: '#D0EBFF', text: '#228BE6' };
    } 
    else if (value === '완료' || value.includes('승인')) {
      style = { bg: '#D3F9D8', text: '#40C057' }; // 초록
    } 
    else if (value === '반려') {
      style = { bg: '#FFE3E3', text: '#FA5252' }; // 빨강
    }

   this.el.innerHTML = `
      <span style="
        background-color: ${style.bg};
        color: ${style.text};
        padding: 4px 12px;
        border-radius: 20px; /* 알약 모양 */
        font-size: 12px;
        font-weight: 600;
        display: inline-block;
        min-width: 50px;
        line-height: 1.4;
      ">
        ${value}
      </span>
    `;
  }
}

// 영어 한글 표기 mes - CODE_MAP
const CODE_MAP = {
	//제품유형
    'FINISHED_GOODS': '완제품',
    'SEMI_FINISHED_GOODS': '반제품',
	
	//상태코드
    'Y': '활성',
    'N': '비활성',
    'PRD_ACTIVE': '활성',
    'PRD_INACTIVE': '비활성',
    'PRD_DISCONTINUED': '단종',
    'PRD_SEASONAL': '시즌상품',
    'PRD_OUT_OF_STOCK': '재고없음',
    'PRD_IN_STOCK': '재고있음',
	
	//원재료 유형
    'RAW': '원재료',
    'SUB': '부자재',
    'PKG': '포장재',
    'WIP': '공정중', // 또는 '재공품' (Work-in-Process)
    'FIN': '완제품', // 또는 '생산품' (Finished Goods)
    'BOX': '박스',
    'UNIT_BOX': '박스',
	
	//bom 그룹 타입 유형
	'STD':	'정규/양산 BOM',
	'ALT':	'대체 BOM',
	'SMP':	'샘플/시험 BOM',
	'DEV':	'개발/연구 BOM',
	'RWRK':	'재작업 BOM',
	
	//향수 유형
	'LIQUID': '액체향수', 
	'SOLID': '고체향수',
	
	//안전재고 - 정책방식
	'FIXED_QTY':'고정 계산방식',
	'DAYS_COVER': '일수기반',
	
	//품질항목기준 - 대상구분
	'FINISHED_QC':'완제품',
    'qc_pH':'pH',
    'qc_cps':'cps',
    'qc_ml':'ml'

};

class StatusModifiedRenderer {
    constructor(props) {
        const el = document.createElement('div');
        el.className = 'tui-grid-cell-content-renderer'; 
        this.el = el;
        this.grid = props.grid; 
        
        this.render(props);
    }
	
    static getKoreanText(englishValue) {
        return CODE_MAP[englishValue] || englishValue; 
    }

    getElement() {
        return this.el;
    }

    // ⭐ 수정: props를 인자로 받아 editor 유무를 확인합니다.
    formatStatusBadge(status, hasEditor) {
        const map = {
            "Y": { text: "활성", cls: "status-badge bg-primary" },
            "N": { text: "비활성", cls: "status-badge bg-warning" },
        };
        const item = map[status] || { text: status, cls: "status-badge" };
        
        // editor 설정이 있을 때만 화살표 추가
        const arrow = hasEditor ? `<span style="font-size:8px;opacity:0.6;padding-left:3px;">▼</span>` : '';
        
        return `<span class="${item.cls}" style="padding: 4px 8px; border-radius: 4px; color: white;">${item.text}${arrow}</span> `;
    }
	
    render(props) {
        const value = props.value;
        const rowKey = props.rowKey;
        const columnInfo = props.columnInfo;
        
        // ⭐ 1. Editor 설정 여부 확인 (핵심!)
        const hasEditor = !!columnInfo.editor; 
        const isSelect = props.columnInfo.renderer?.options?.isSelect;

        const koreanText = StatusModifiedRenderer.getKoreanText(value);

        let isCreated = false;
        let isUpdated = false;

        if (this.grid) {
            const { createdRows, updatedRows } = this.grid.getModifiedRows();
            isCreated = createdRows.some(r => String(r.rowKey) === String(rowKey));
            isUpdated = updatedRows.some(r => String(r.rowKey) === String(rowKey));
        }

        const hasValue = value !== null && value !== undefined && value !== '';
        const displayText = (!hasValue && isCreated) ? '' : koreanText;

        let contentHTML = '';
        const showArrow = hasEditor;
        
        // ⭐ 수정된 부분: Y/N 컬럼인지 확인하는 로직 (값 유무와 상관없이)
        const isYNColumn = columnInfo.name === 'useYn' || columnInfo.name === 'status'; // 프로젝트의 Y/N 컬럼명을 넣으세요

        if (value === 'Y' || value === 'N') {
            // 1. 이미 값이 있는 경우 (기존 배지 로직)
            contentHTML = this.formatStatusBadge(value, showArrow);
        } else if (isYNColumn && isCreated) {
            // 2. ⭐ Y/N 컬럼인데 신규 행이라서 아직 값이 없는 경우
            // 빈 텍스트 상태의 배지 모양이나 화살표가 포함된 레이아웃을 그려줍니다.
            const arrow = showArrow ? `<span style="font-size:10px;opacity:0.6;">▼</span>` : '';
            contentHTML = `
                <div style="width:100%; height:100%; padding:0px 10px; box-sizing:border-box; display:flex; justify-content:space-between; align-items:center; cursor:pointer;">
                    <span></span> ${arrow}
                </div>
            `;
        } else if (isSelect) {
            // 3. 일반 Select 컬럼
            const arrow = showArrow ? `<span style="font-size:10px;opacity:0.6;">▼</span>` : '';
            contentHTML = `
                <div style="width:100%; height:100%; padding:0px 10px; box-sizing:border-box; display:flex; justify-content:space-between; align-items:center; cursor:pointer;">
                    <span>${displayText}</span>
                    ${arrow}
                </div>
            `;
        } else {
            contentHTML = displayText;
        }

        this.el.innerHTML = contentHTML;

        // 하이라이트 로직 (기존과 동일)
        const shouldHighlight = isUpdated || (isCreated && hasValue);
        const highlightClassName = 'modified-cell-highlight';

        if (shouldHighlight) {
            this.el.classList.add(highlightClassName);
        } else {
            this.el.classList.remove(highlightClassName);
        }
    }
}

// 숫자와 소수점 입력 가능하도록 하는 커스텀 에디터 클래스
class NumberOnlyEditor {
  constructor(props) {
    const el = document.createElement('input');
    el.type = 'text'; // type을 text로 두어야 input 이벤트 제어가 용이합니다.
    this.el = el;

    // 초기 값 설정
    this.el.value = props.value || '';

    // 입력 이벤트를 감지하여 숫자와 소수점만 남기는 함수 바인딩
    this.el.addEventListener('input', this.handleInput.bind(this));
  }

  getElement() {
    return this.el;
  }

  getValue() {
    // 최종적으로 저장될 때도 유효한 숫자 형태만 반환합니다.
    return this.sanitizeNumber(this.el.value);
  }
  
  // 유효한 숫자 형태로 정리하는 메서드
  sanitizeNumber(value) {
    // 숫자와 소수점만 남기고, 소수점이 여러 개면 첫 번째만 남김
    let cleaned = value.replace(/[^0-9.]/g, '');
    
    // 소수점이 여러 개 있는 경우 첫 번째만 남기기
    const parts = cleaned.split('.');
    if (parts.length > 2) {
      cleaned = parts[0] + '.' + parts.slice(1).join('');
    }
    
    return cleaned;
  }
  
  // 입력 시마다 실행되어 숫자와 소수점만 남기는 함수
  handleInput() {
    const currentValue = this.el.value;
    const sanitized = this.sanitizeNumber(currentValue);
    
    // 값이 변경되었을 때만 업데이트 (커서 위치 유지)
    if (currentValue !== sanitized) {
      const cursorPos = this.el.selectionStart;
      this.el.value = sanitized;
      // 커서 위치 복원 (제거된 문자 수만큼 조정)
      const removedChars = currentValue.length - sanitized.length;
      this.el.setSelectionRange(cursorPos - removedChars, cursorPos - removedChars);
    }
  }
}


