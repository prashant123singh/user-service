package com.vinsguru.netflux.user.service;

import com.vinsguru.netflux.user.dto.LoginRequest;
import com.vinsguru.netflux.user.dto.LoginResponse;
import com.vinsguru.netflux.user.dto.RegisterRequest;
import com.vinsguru.netflux.user.exceptions.InvalidCredentialsException;
import com.vinsguru.netflux.user.exceptions.UsernameAlreadyExistsException;
import com.vinsguru.netflux.user.mapper.UserMapper;
import com.vinsguru.netflux.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException(request.username());
        }
        var user = UserMapper.toEntity(request, passwordEncoder.encode(request.password()));
        userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {
        var user = authenticate(request);
        return new LoginResponse(
                tokenService.generateToken(user),
                user.name()
        );
    }

    private AuthenticatedUser authenticate(LoginRequest request) {
        try {
            var token = UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password());
            return (AuthenticatedUser) authenticationManager.authenticate(token).getPrincipal();
        } catch (AuthenticationException ex) {
            throw new InvalidCredentialsException();
        }
    }
}
