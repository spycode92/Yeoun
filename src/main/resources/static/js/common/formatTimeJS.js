
// 2025-11-22 오후 5:11
function formatTimeFull(date = new Date()) {
	const year 	  = date.getFullYear();
	const month   = String(date.getMonth() + 1).padStart(2, "0");
	const day	  = String(date.getDate()).padStart(2, "0");
	
	let hours	  = date.getHours();
	const minutes = String(date.getMinutes()).padStart(2, "0");
	const ampm	  = hours >= 12 ? "오후" : "오전";
	hours = hours % 12 || 12;
	
    return `${year}-${month}-${day} ${ampm} ${hours}:${minutes}`;
}

// 오후 5:11
function formatTimeOnly(date = new Date()) {
	let hours	  = date.getHours();
	const minutes = String(date.getMinutes()).padStart(2, "0");
	const ampm	  = hours >= 12 ? "오후" : "오전";
	hours = hours % 12 || 12;
	
    return `${ampm} ${hours}:${minutes}`;
}

function formatTimeOnly24(d) {
	const date = new Date(d);
	const h = String(date.getHours()).padStart(2, '0');
	const m = String(date.getMinutes()).padStart(2, '0');
	return `${h}:${m}`;
}
