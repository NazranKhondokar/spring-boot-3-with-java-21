package com.nazran.mapstruct.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "interest_menu")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterestMenu extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "INTEREST_MENU_ID")
    private Long interestMenuId;

    @Column(name = "INTEREST_MENU_NAME")
    private String interestMenuName;

    @Column(name = "INTEREST_MENU_CODE")
    private String interestMenuCode;
}