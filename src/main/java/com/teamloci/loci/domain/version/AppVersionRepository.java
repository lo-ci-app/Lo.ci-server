package com.teamloci.loci.domain.version;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppVersionRepository extends JpaRepository<AppVersion, Long> {
    Optional<AppVersion> findTopByOsTypeOrderByIdDesc(OsType osType);
}