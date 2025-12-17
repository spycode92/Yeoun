// zone 삭제
async function deleteZone(zoneName) {
	const deleteZoneConfirm = confirm(`[ZONE ${zoneName}] 전체를 삭제하시겠습니까?\n포함된 Rack과 Cell이 모두 삭제됩니다.`);
	
	if (!deleteZoneConfirm) return;
	
	try {
		const res = await fetch(`/api/inventories/zones/${zoneName}`, {
			method: "DELETE", 
			headers : {
				[csrfHeader]: csrfToken,
				"Content-Type": "application/json"
			}
		});
		
		if (!res.ok) {
			throw new Error("서버 오류");
		}
		
		alert("구역이 삭제되었습니다.");
		
		// 새로고침
		await initDashboard();
	} catch (error) {
		console.error(error);
		alert("서버 오류 발생!");
	}
}

// rack 삭제
async function deleteRack(zone, rack) {
	const deleteRackConfirm = confirm(`[ZONE ${zone} - Rack ${rack}]을 삭제하시겠습니까?`);
	
	if (!deleteRackConfirm) return;
	
	try {
		const res = await fetch(`/api/inventories/racks?zone=${zone}&rack=${rack}`, {
			method: "DELETE",
			headers : {
				[csrfHeader]: csrfToken,
				"Content-Type": "application/json"
			}
		});
		
		if (!res.ok) {
			const errorMessage = await res.text(); 
			throw new Error(errorMessage || "서버 오류");
		}
		
		alert("RACK이 삭제되었습니다.");
		
		// 새로고침
		await initDashboard();
	} catch (error) {
		console.error(error);
		alert(error.message);
	}
}