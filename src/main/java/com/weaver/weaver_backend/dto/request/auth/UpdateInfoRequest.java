package com.weaver.weaver_backend.dto.request.auth;

public record UpdateInfoRequest (
     String nickname,
     String firstName,
     String lastName,
     String phone
) {

}
