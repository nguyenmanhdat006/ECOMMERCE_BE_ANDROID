package com.nguyendat.shopee_be.controllers;

import com.nguyendat.shopee_be.dto.UserDetailsDto;
import com.nguyendat.shopee_be.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;

@RestController
@RequestMapping("/api/user")
public class UserDetailController {

    private static final Logger logger = LoggerFactory.getLogger(UserDetailController.class);

    @Autowired
    private UserDetailsService userDetailsService;

    @GetMapping("/profile")
    public ResponseEntity<UserDetailsDto> getUserProfile(Principal principal){
        User user = (User) userDetailsService.loadUserByUsername(principal.getName());
        logger.info("User: {}", user);

        if(null == user){
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        UserDetailsDto userDetailsDto = UserDetailsDto.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .id(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getPrimaryRole())
                .addressList(user.getAddressList())
                .authorityList(user.getRoles()).build();

        logger.info("UserDetailsDto: {}", userDetailsDto);
        return new ResponseEntity<>(userDetailsDto, HttpStatus.OK);

    }
}