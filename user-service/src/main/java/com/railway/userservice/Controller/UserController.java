package com.railway.userservice.Controller;

import com.railway.userservice.DTO.LoginResponse;
import com.railway.userservice.Service.UserService;
import com.railway.userservice.Entity.User;
import com.railway.userservice.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService service;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public User register(@RequestBody User user) {
        return service.register(user);
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody User user) {

        User loggedUser = service.login(user.getEmail(), user.getPassword());

        String token = jwtUtil.generateToken(loggedUser.getEmail());

        return new LoginResponse(
                token,
                "Bearer",
                loggedUser.getEmail(),
                loggedUser.getRole().name(),
                "Login successful"
        );
    }

    @GetMapping("/profile")
    public String profile() {
        return "This is secured profile API 🔐";
    }
}