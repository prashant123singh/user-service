package com.vinsguru.netflux.user.mapper;

import com.vinsguru.netflux.user.dto.RegisterRequest;
import com.vinsguru.netflux.user.entity.User;
import com.vinsguru.netflux.user.service.AuthenticatedUser;

public class UserMapper {

    public static User toEntity(RegisterRequest request, String encodedPassword) {
        var user = new User();
        user.setUsername(request.username());
        user.setPassword(encodedPassword);
        user.setName(request.name());
        return user;
    }

    public static AuthenticatedUser toAuthenticatedUser(User user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getName()
        );
    }
}
