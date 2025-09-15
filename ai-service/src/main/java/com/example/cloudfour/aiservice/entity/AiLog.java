package com.example.cloudfour.aiservice.entity;

import com.example.cloudfour.aiservice.exception.AiLogErrorCode;
import com.example.cloudfour.aiservice.exception.AiLogException;
import com.example.cloudfour.modulecommon.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

import java.util.UUID;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "p_ailog")
public class AiLog extends BaseEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String result;

    @Column(name = "success")
    private Boolean success;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "request_type")
    private String requestType;

    public static class AiLogBuilder{
        private AiLogBuilder id(UUID id) {
            throw new AiLogException(AiLogErrorCode.CREATE_FAILED);
        }
    }
}