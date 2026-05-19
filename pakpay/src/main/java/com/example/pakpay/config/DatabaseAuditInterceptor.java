package com.example.pakpay.config;

import org.hibernate.CallbackException;
import org.hibernate.Interceptor;
import org.hibernate.type.Type;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.example.pakpay.entity.AuditLog;

import java.time.LocalDateTime;

@Component
public class DatabaseAuditInterceptor implements Interceptor {

    // Khas dhyan dein: Yahan se @Autowired hata diya hai humne

    @Override
    public boolean onSave(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {
        if (!(entity instanceof AuditLog)) {
            saveAuditRecord(entity.getClass().getSimpleName(), "INSERT", id);
        }
        return false;
    }

    @Override
    public boolean onFlushDirty(Object entity, Object id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) throws CallbackException {
        if (!(entity instanceof AuditLog)) {
            saveAuditRecord(entity.getClass().getSimpleName(), "UPDATE", id);
        }
        return false;
    }

    @Override
    public void onDelete(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {
        if (!(entity instanceof AuditLog)) {
            saveAuditRecord(entity.getClass().getSimpleName(), "DELETE", id);
        }
    }

    private void saveAuditRecord(String entityName, String action, Object id) {
        try {
            // Context Provider se dynamic tarike se JdbcTemplate nikalna
            JdbcTemplate jdbcTemplate = ApplicationContextProvider.getBean(JdbcTemplate.class);
            
            if (jdbcTemplate != null) {
                String sql = "INSERT INTO audit_logs (entity_name, action, entity_id, timestamp) VALUES (?, ?, ?, ?)";
                jdbcTemplate.update(sql, entityName, action, id != null ? id.toString() : "UNKNOWN", LocalDateTime.now());
            } else {
                System.err.println("Audit Log Failed: Spring context or JdbcTemplate is not ready yet.");
            }
        } catch (Exception e) {
            System.err.println("Failed to write audit log: " + e.getMessage());
        }
    }
}