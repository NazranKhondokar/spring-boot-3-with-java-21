package com.nazran.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageAttachmentResponse {
    private Integer id;
    private Integer messageId;
    private String fileUrl;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private OffsetDateTime createdAt;
}
