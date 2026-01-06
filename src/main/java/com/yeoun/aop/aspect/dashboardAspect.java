package com.yeoun.aop.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.yeoun.aop.annotation.InventoryDashboard;

import lombok.RequiredArgsConstructor;

@Aspect
@Component
@RequiredArgsConstructor
public class dashboardAspect {
	
	private final SimpMessagingTemplate messagingTemplate;
	
	@AfterReturning("@annotation(InventoryDashboard)")
	public void inventoryDashBoard(JoinPoint joinPoint, InventoryDashboard inventoryDashboard) {
		// 커밋후에 실행하기위해 적용
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
//				System.out.println("어노테이션시작");
				messagingTemplate.convertAndSend("/dashboard/inventory", inventoryDashboard.value());
			}
		
		});
	}
	
}
