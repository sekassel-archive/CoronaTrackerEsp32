package de.uniks.postgres.db;

import java.util.List;
import java.util.Optional;

public interface Dao<T, I> {
    List<T> get(String uuid);
    List<T> getAll();
    Optional<I> save(T t);
    void update(T t);
    void delete(T t);
}
