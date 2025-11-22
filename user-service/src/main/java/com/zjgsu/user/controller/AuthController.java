package com.zjgsu.user.controller;

import com.zjgsu.user.common.ApiResponse;
import com.zjgsu.user.dto.LoginRequest;
import com.zjgsu.user.dto.LoginResponse;
import com.zjgsu.user.model.User;
import com.zjgsu.user.service.UserService;
import com.zjgsu.user.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        // 1. 验证用户名密码
        User user = userService.authenticate(request.getUsername(), request.getPassword());

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(401, "用户名或密码错误"));
        }

        // 2. 生成JWT Token
        String token = jwtUtil.generateToken(
                user.getId().toString(),
                user.getUsername(),
                user.getRole()
        );

        // 3. 返回Token和用户信息
        LoginResponse response = new LoginResponse(token, user);
        return ResponseEntity.ok(new ApiResponse<>(200, "登录成功", response));
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(@RequestBody User user) {
        // 设置默认角色
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("USER");
        }

        User createdUser = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(createdUser));
    }
}
