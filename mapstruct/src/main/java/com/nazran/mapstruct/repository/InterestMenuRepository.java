package com.nazran.mapstruct.repository;

import com.nazran.mapstruct.entity.InterestMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InterestMenuRepository extends JpaRepository<InterestMenu, Long> {
}
