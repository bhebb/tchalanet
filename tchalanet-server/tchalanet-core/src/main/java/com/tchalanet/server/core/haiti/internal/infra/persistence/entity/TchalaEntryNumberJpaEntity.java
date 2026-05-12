package com.tchalanet.server.core.haiti.internal.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "tchala_entry_number")
@Getter
@Setter
public class TchalaEntryNumberJpaEntity {

    @EmbeddedId
    private Pk pk = new Pk();

    @MapsId("entryId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entry_id", nullable = false)
    private TchalaEntryJpaEntity entry;

    public String getLang() {
        return pk.lang;
    }

    public void setLang(String lang) {
        pk.lang = lang;
    }

    public short getNumber() {
        return pk.number;
    }

    public void setNumber(short number) {
        pk.number = number;
    }

    public void setEntry(TchalaEntryJpaEntity entry) {
        this.entry = entry;
        this.pk.entryId = entry == null ? null : entry.getId();
    }

    @Embeddable
    @Getter
    @Setter
    public static class Pk implements Serializable {

        @Column(name = "entry_id", nullable = false)
        private UUID entryId;

        @Column(name = "lang", nullable = false, length = 8)
        private String lang;

        @Column(name = "number", nullable = false)
        private short number;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk pk)) return false;
            return number == pk.number
                && Objects.equals(entryId, pk.entryId)
                && Objects.equals(lang, pk.lang);
        }

        @Override
        public int hashCode() {
            return Objects.hash(entryId, lang, number);
        }
    }
}
