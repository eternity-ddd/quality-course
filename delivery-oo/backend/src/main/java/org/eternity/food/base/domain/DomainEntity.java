package org.eternity.food.base.domain;

import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class DomainEntity<T extends DomainEntity<T, TID>, TID> {
    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }

        if (!other.getClass().equals(getClass())) {
            return false;
        }

        return equals((T)other);
    }

    public boolean equals(T other) {
        if (other == null) {
            return false;
        }

        if (getId() == null) {
            return false;
        }

        return getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getId() == null ? 0 : getId().hashCode();
    }

    abstract public TID getId();
}
