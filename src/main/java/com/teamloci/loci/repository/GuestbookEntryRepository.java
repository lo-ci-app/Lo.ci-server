package com.teamloci.loci.repository;

import com.teamloci.loci.domain.GuestbookEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GuestbookEntryRepository extends JpaRepository<GuestbookEntry, Long> {

    @Query("SELECT ge FROM GuestbookEntry ge " +
            "JOIN FETCH ge.author " +
            "WHERE ge.host.id = :hostId " +
            "ORDER BY ge.createdAt DESC")
    List<GuestbookEntry> findByHostIdWithAuthor(@Param("hostId") Long hostId);
}