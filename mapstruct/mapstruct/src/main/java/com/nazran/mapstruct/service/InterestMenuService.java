package com.nazran.mapstruct.service;

import com.nazran.mapstruct.dto.InterestMenuDto;
import com.nazran.mapstruct.entity.InterestMenu;
import com.nazran.mapstruct.mapper.InterestMenuMapper;
import com.nazran.mapstruct.repository.InterestMenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterestMenuService {

    private final InterestMenuRepository interestMenuRepository;
    private final InterestMenuMapper mapper = InterestMenuMapper.INSTANCE;

    public List<InterestMenuDto> getAllInterestMenus() {
        return interestMenuRepository.findAll()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    public InterestMenuDto createInterestMenu(InterestMenuDto dto) {
        InterestMenu interestMenu = mapper.toEntity(dto);
        InterestMenu savedInterestMenu = interestMenuRepository.save(interestMenu);
        return mapper.toDto(savedInterestMenu);
    }
}
