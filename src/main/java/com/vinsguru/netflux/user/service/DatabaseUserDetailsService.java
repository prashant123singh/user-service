package com.vinsguru.netflux.user.service;

import com.vinsguru.netflux.user.mapper.UserMapper;
import com.vinsguru.netflux.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DatabaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(UserMapper::toAuthenticatedUser)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
