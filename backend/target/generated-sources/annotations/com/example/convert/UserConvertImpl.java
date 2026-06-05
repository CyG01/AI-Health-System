package com.example.convert;

import com.example.entity.SysUser;
import com.example.vo.UserInfoVO;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-05T21:08:53+0800",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class UserConvertImpl implements UserConvert {

    @Override
    public UserInfoVO toUserInfoVO(SysUser user) {
        if ( user == null ) {
            return null;
        }

        UserInfoVO userInfoVO = new UserInfoVO();

        userInfoVO.setPhone( desensitizePhone( user.getPhone() ) );
        userInfoVO.setId( user.getId() );
        userInfoVO.setUsername( user.getUsername() );
        userInfoVO.setNickname( user.getNickname() );
        userInfoVO.setAvatar( user.getAvatar() );
        userInfoVO.setGender( user.getGender() );
        userInfoVO.setAge( user.getAge() );
        userInfoVO.setRole( user.getRole() );
        userInfoVO.setCreateTime( user.getCreateTime() );

        return userInfoVO;
    }
}
