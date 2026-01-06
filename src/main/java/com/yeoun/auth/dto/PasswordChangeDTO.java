package com.yeoun.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordChangeDTO {
	
	@NotBlank(message = "현재 비밀번호를 입력하세요.")
    private String currentPassword;

	@NotBlank(message = "새 비밀번호를 입력하세요.")
	@Pattern(
	    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+=-]).{8,20}$",
	    message = "비밀번호는 대/소문자, 숫자, 특수문자를 포함하여 8~20자여야 합니다."
	)
    private String newPassword;

	@NotBlank(message = "새 비밀번호 확인을 입력하세요.")
	@Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다.")
    private String confirmPassword;

}
