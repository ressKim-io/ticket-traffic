package com.sportstix.game.domain;

import com.sportstix.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stadiums")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stadium extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 200)
    private String address;

    @Column(nullable = false)
    private int totalCapacity;

    @OneToMany(mappedBy = "stadium", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Section> sections = new ArrayList<>();

    @Builder
    private Stadium(String name, String address, int totalCapacity) {
        this.name = name;
        this.address = address;
        this.totalCapacity = totalCapacity;
    }

    public void addSection(Section section) {
        sections.add(section);
    }
}
