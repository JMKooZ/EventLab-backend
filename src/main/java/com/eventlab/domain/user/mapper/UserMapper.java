package com.eventlab.domain.user.mapper;

import com.eventlab.domain.user.dto.UserResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


@Mapper
public interface UserMapper {

    List<UserResponse> findAll();

    int insertUser(@Param("email") String email, @Param("passwordHash") String passwordHash, @Param("nickname") String nickname);

    UserResponse findByEmail(@Param("email") String email);

    int countByEmail(@Param("email") String email);
}