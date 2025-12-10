package com.teamloci.loci.domain.friend;

import com.teamloci.loci.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_contacts", indexes = {
        @Index(name = "idx_user_contact_phone", columnList = "user_id, phone_number")
})
public class UserContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "phone_search_hash")
    private String phoneSearchHash;

    @Builder
    public UserContact(User user, String name, String phoneNumber, String phoneSearchHash) {
        this.user = user;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.phoneSearchHash = phoneSearchHash;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updatePhoneSearchHash(String phoneSearchHash) {
        this.phoneSearchHash = phoneSearchHash;
    }
}