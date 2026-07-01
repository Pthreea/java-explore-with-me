package ru.practicum.ewm.util;

import ru.practicum.ewm.admin.dto.NewUserRequest;
import ru.practicum.ewm.admin.dto.UserDto;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.pub.dto.UserShortDto;

public class UserMapper {

    public static UserDto toUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    public static User toUser(NewUserRequest dto) {
        return User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .build();
    }

    public static UserShortDto toUserShortDto(User user) {
        return UserShortDto.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
    }
}
