package com.example.mulitpleapi.dto;

import lombok.*;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class ProductDTO {
    private Long   id;
    private String name;
    private Double price;
    private Integer stock;
}
