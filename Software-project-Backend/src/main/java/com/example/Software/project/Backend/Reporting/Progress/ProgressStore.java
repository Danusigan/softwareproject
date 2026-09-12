package com.example.Software.project.Backend.Reporting.Progress;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.*;

@Repository
@DependsOn("progressMigrations")
public class ProgressStore {
    private final EntityManager em;
    public ProgressStore(EntityManager em) { this.em = em; }
    public <T> T find(Class<T> type, Object id) { return em.find(type, id); }
    public List<Map<String, Object>> rows(String sql, Object... args) {
        var query = em.createNativeQuery(sql, Tuple.class);
        for (int i = 0; i < args.length; i++) query.setParameter(i + 1, args[i]);
        @SuppressWarnings("unchecked") List<Tuple> tuples = query.getResultList();
        return tuples.stream().map(tuple -> {
            Map<String, Object> row = new LinkedHashMap<>();
            tuple.getElements().forEach(e -> row.put(e.getAlias().toLowerCase(Locale.ROOT), tuple.get(e)));
            return row;
        }).toList();
    }
    public Map<String, Object> one(String sql, Object... args) {
        return rows(sql, args).stream().findFirst().orElse(Map.of());
    }
    public void execute(String sql, Object... args) {
        var query = em.createNativeQuery(sql);
        for (int i = 0; i < args.length; i++) query.setParameter(i + 1, args[i]);
        query.executeUpdate();
    }
    public static String str(Map<String, Object> row, String key) {
        return row.get(key) == null ? null : row.get(key).toString();
    }
    public static BigDecimal dec(Map<String, Object> row, String key) {
        return row.get(key) == null ? null : new BigDecimal(row.get(key).toString());
    }
    public static boolean bool(Map<String, Object> row, String key) {
        return Boolean.TRUE.equals(row.get(key)) || "1".equals(str(row, key));
    }
    public static int integer(Map<String, Object> row, String key) { return ((Number) row.get(key)).intValue(); }
}
