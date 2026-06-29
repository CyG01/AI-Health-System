package com.example.convert;

import com.example.entity.SysUser;
import com.example.vo.UserInfoVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface UserConvert {

    @Mapping(target = "phone", source = "phone", qualifiedByName = "desensitizePhone")
    UserInfoVO toUserInfoVO(SysUser user);

    @Named("desensitizePhone")
    default String desensitizePhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
