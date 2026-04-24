package com.example.mulitpleapi.dto;
import lombok.*;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class UserDTO {
    private Long   id;
    private String name;
    private String email;
    private String department;
}