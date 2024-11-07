package com.nazran.mapstruct.mapper;

import com.nazran.mapstruct.dto.InterestMenuDto;
import com.nazran.mapstruct.entity.InterestMenu;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface InterestMenuMapper {

    InterestMenuMapper INSTANCE = Mappers.getMapper(InterestMenuMapper.class);

    @Mapping(target = "interestMenuId", source = "interestMenuId")
    @Mapping(target = "interestMenuName", source = "interestMenuName")
    @Mapping(target = "interestMenuCode", source = "interestMenuCode")
    InterestMenuDto toDto(InterestMenu interestMenu);

    InterestMenu toEntity(InterestMenuDto interestMenuDto);
}

