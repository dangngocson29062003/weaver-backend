package com.weaver.weaver_backend.mapper;

import com.weaver.weaver_backend.dto.response.user.NotificationResponse;
import com.weaver.weaver_backend.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface NotificationMapper {
    List<NotificationResponse> toResponseList(List<Notification> notifications);
}
