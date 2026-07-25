package com.cloudstorage.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class LoginRequest {
    private String login;
    private String password;
}