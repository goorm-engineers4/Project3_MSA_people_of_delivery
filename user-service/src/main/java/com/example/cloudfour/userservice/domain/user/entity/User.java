package com.example.cloudfour.userservice.domain.user.entity;


import com.example.cloudfour.modulecommon.entity.BaseEntity;
import com.example.cloudfour.userservice.domain.user.enums.Role;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "p_user")
public class User extends BaseEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(unique = false)
    private String pendingEmail;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String number;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = CascadeType.ALL)
    @Builder.Default
    private List<UserAddress> addresses = new ArrayList<>();

    public static class UserBuilder {
        private UserBuilder id(UUID id) {
            throw new UnsupportedOperationException("id 수동 생성 불가");
        }
    }

    public void changePassword(String encodedPassword) { this.password = encodedPassword; }
    public void changeNickname(String nickname) { this.nickname = nickname; }
    public void changeNumber(String number) { this.number = number; }

    public void requestEmailChange(String newEmail) { this.pendingEmail = newEmail; }

    public void confirmEmailChange() {
        if (this.pendingEmail == null) return;
        this.email = this.pendingEmail;
        this.pendingEmail = null;
        this.emailVerified = true;
    }

    public void markEmailVerified() { this.emailVerified = true; }
}