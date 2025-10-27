package com.jinkops.controller;

import com.jinkops.entity.User;
import com.jinkops.exception.BizException;
import com.jinkops.exception.ErrorCode;
import com.jinkops.service.UserService;
import com.jinkops.vo.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/{id}")
    public ApiResponse<User> getUserById(@PathVariable Long id) {
        return userService.findById(id)
                .map(ApiResponse::success)
                .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));
    }
}
