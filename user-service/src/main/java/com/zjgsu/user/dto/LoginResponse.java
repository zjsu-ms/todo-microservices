package com.zjgsu.user.dto;

import com.zjgsu.user.model.User;

/**
 * 登录响应DTO
 */
public class LoginResponse {
    private String token;
    private UserInfo user;

    public LoginResponse() {
    }

    public LoginResponse(String token, User user) {
        this.token = token;
        this.user = new UserInfo(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserInfo getUser() {
        return user;
    }

    public void setUser(UserInfo user) {
        this.user = user;
    }

    /**
     * 用户信息（不包含密码）
     */
    public static class UserInfo {
        private Long id;
        private String username;
        private String email;
        private String role;

        public UserInfo() {
        }

        public UserInfo(Long id, String username, String email, String role) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.role = role;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }
}
