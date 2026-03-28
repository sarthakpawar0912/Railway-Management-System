package com.railway.userservice.DTO;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {

    private String token;
    private String type;
    private String email;
    private String role;
    private String message;
}