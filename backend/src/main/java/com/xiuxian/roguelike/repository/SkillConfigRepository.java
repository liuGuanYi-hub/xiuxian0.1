package com.xiuxian.roguelike.repository;

import com.xiuxian.roguelike.domain.SkillConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkillConfigRepository extends JpaRepository<SkillConfigEntity, String> {
    List<SkillConfigEntity> findByEnabledTrueOrderByCardIdAsc();
}
