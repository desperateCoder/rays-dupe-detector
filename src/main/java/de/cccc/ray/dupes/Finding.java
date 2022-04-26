package de.cccc.ray.dupes;

import java.util.HashSet;
import java.util.Set;

public class Finding {
    public String name;
    public Set<String> foundInFiles = new HashSet<>();

    public Finding(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Finding finding = (Finding) o;
        return finding.name.equals(name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
