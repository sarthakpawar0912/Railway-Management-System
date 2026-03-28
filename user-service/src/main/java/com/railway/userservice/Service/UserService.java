package com.railway.userservice.Service;


import com.railway.userservice.Entity.User;

public interface UserService {


     User register(User user);


    User login(String email, String password);


    User getByEmail(String email);
}