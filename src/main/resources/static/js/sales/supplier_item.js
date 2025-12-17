document.addEventListener("DOMContentLoaded", () => {

    const materialSelect = document.getElementById("materialSelect");
    const unitSelect = document.getElementById("unitSelect");

    if (!materialSelect || !unitSelect) return;

    // 전체 단위 옵션을 미리 백업
    const allUnitOptions = Array.from(unitSelect.options).map(opt => ({
        value: opt.value,
        text: opt.text
    }));

    materialSelect.addEventListener("change", () => {

        const selectedOption =
            materialSelect.options[materialSelect.selectedIndex];

        const matUnit =
            selectedOption.dataset.matUnit?.trim().toUpperCase();

        if (!matUnit) return;

        // 기준 단위 → 허용 단위 매핑
        const unitMap = {
			"KG": ["kg", "g"],
			"G": ["kg", "g"],
			"BOX": ["BOX"],
			"ML": ["ml", "L"],
			"L": ["L", "ml"],
			"EA": ["EA"]
        };

        const allowedUnits = unitMap[matUnit] || [matUnit];

        // 🔥 select 초기화
        unitSelect.innerHTML = "";

        // 기본 placeholder
        const placeholder = document.createElement("option");
        placeholder.value = "";
        placeholder.textContent = "단위 선택";
        unitSelect.appendChild(placeholder);

        // 🔥 허용 단위만 다시 추가
        allUnitOptions.forEach(opt => {
            if (allowedUnits.includes(opt.value)) {
                const optionEl = document.createElement("option");
                optionEl.value = opt.value;
                optionEl.textContent = opt.text;
                unitSelect.appendChild(optionEl);
            }
        });

        // 기본값 자동 선택 (기준 단위)
        if (allowedUnits.includes(matUnit)) {
            unitSelect.value = matUnit;
        }
    });
});
