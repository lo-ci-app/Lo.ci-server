package com.teamloci.loci.domain;

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

    @Builder
    public UserContact(User user, String name, String phoneNumber) {
        this.user = user;
        this.name = name;
        this.phoneNumber = phoneNumber;
    }

    public void updateName(String name) {
        this.name = name;
    }
}