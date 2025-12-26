class StatusBadgeRenderer {
  constructor(props) {
    const el = document.createElement('div');
    el.style.textAlign = 'center';
    this.el = el;
    this.render(props);
  }

  getElement() {
    return this.el;
  }

  render(props) {
    const value = String(props.value || '');
    const row = props.grid.getRow(props.rowKey);
    const finishDateStr = row.finish_date; 
    
    // 날짜 계산 (오늘 기준 3일 이내 체크)
    const today = new Date();
    today.setHours(0, 0, 0, 0); 
    const finishDate = new Date(finishDateStr);
    finishDate.setHours(0, 0, 0, 0);

    const diffTime = finishDate.getTime() - today.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

    // 기본 스타일 세팅
    let style = { bg: '#F1F3F5', text: '#868E96', icon: '', border: 'none' };

    if (value.includes('대기')) {
      if (finishDateStr && diffDays >= 0 && diffDays <= 3) {
        // [임박 대기] 3일 이내: 살구색 배경 + 주황색 테두리 + 시계 아이콘
        style = { 
          bg: '#FFF4E5', 
          text: '#D9480F', 
          icon: '🕒', 
          border: '1px solid #FF922B' 
        };
      } else {
        // [일반 대기] 파란색 배지
        style = { bg: '#D0EBFF', text: '#228BE6', icon: '', border: 'none' };
      }
    } 
    else if (value === '완료' || value.includes('승인')) {
      // [완료/승인] 초록색 배지 (이전 스타일 복구)
      style = { bg: '#D3F9D8', text: '#40C057', icon: '', border: 'none' };
    } 
    else if (value === '반려') {
      // [반려] 빨간색 배지
      style = { bg: '#FFE3E3', text: '#FA5252', icon: '', border: 'none' };
    }

    this.el.innerHTML = `
      <span style="
        background-color: ${style.bg};
        color: ${style.text};
        border: ${style.border};
        
        /* 크기 고정 및 중앙 정렬 */
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 85px;           /* 모든 배지 너비 통일 */
        height: 24px;          /* 높이 고정 */
        border-radius: 20px;
        box-sizing: border-box; /* 테두리가 있어도 85px 유지 */

        /* 폰트 설정 유지 */
        font-size: 12px;
        font-weight: 600;
        line-height: 1;
      ">
        ${style.icon ? `<span style="margin-right: 4px; display: flex; align-items: center;">${style.icon}</span>` : ''}
        ${value}
      </span>
    `;
  }
}