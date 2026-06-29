package com.example.convert;

import com.example.entity.SysNotification;
import com.example.vo.NotificationVO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationConvert {

    NotificationVO toNotificationVO(SysNotification notification);

    List<NotificationVO> toNotificationVOList(List<SysNotification> notifications);
}
