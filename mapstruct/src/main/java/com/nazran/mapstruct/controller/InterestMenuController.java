package com.nazran.mapstruct.controller;

import com.nazran.mapstruct.dto.InterestMenuDto;
import com.nazran.mapstruct.service.InterestMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/interest-menus")
@RequiredArgsConstructor
public class InterestMenuController {

    private final InterestMenuService interestMenuService;

    @GetMapping
    public ResponseEntity<List<InterestMenuDto>> getAllInterestMenus() {
        return ResponseEntity.ok(interestMenuService.getAllInterestMenus());
    }

    @PostMapping
    public ResponseEntity<InterestMenuDto> createInterestMenu(@RequestBody InterestMenuDto dto) {
        return ResponseEntity.ok(interestMenuService.createInterestMenu(dto));
    }
}
