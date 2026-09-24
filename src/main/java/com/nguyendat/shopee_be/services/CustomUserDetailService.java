package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.entities.User;
import com.nguyendat.shopee_be.repositories.UserDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

//userDetailService : interface để thao tác với dữ liệu người dùng -> trả về đối tượng userDetails chứa thông tìn đăng kí/đăng nhập của user
@Service
public class CustomUserDetailService implements UserDetailsService {

    @Autowired
    private UserDetailRepository userDetailRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user= userDetailRepository.findByEmail(username);
        if(null == user){
            throw new UsernameNotFoundException("User Not Found with userName "+username);
        }
        return user;
    }
}