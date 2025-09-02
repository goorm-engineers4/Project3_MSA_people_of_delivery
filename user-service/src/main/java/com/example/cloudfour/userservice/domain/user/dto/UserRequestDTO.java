package com.example.cloudfour.userservice.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class UserRequestDTO {
    public record UserUpdateRequestDTO(
            @Size(min=2, max=20)
            String nickname,

            @Pattern(regexp="^[0-9\\-]{7,20}$", message="전화번호 형식이 올바르지 않습니다.")
            String number
    ) {}

    public record AddressRequestDTO(
            @NotBlank
            @Size(min = 2, max = 500)
            String address
    ) {}

}
