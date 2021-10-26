package com.codeback.web.dto;

import com.codeback.domain.user.User;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserSaveRequestDto {
    private String email;
    private String nickname;
    private String password;
    private boolean email_no_duplicate;
    private boolean email_auth;
    private boolean nickname_no_duplicate;

    @Builder
    public UserSaveRequestDto(String email, String nickname, String password){
        this.email = email;
        this.nickname = nickname;
        this.password = password;
    }

    public  User toEntity(){
        return User.builder()
                .email(email)
                .nickname(nickname)
                .password(password)
                .build();
    }
}