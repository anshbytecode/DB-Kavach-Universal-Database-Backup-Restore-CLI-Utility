package com.dbbackup.dbms;

import com.dbbackup.model.DbmsType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DbmsAdapterFactory {
    private final Map<DbmsType, DbmsAdapter> adapterMap;

    @Autowired
    public DbmsAdapterFactory(List<DbmsAdapter> adapters) {
        this.adapterMap = adapters.stream()
                .collect(Collectors.toMap(DbmsAdapter::getType, Function.identity()));
    }

    public DbmsAdapter getAdapter(DbmsType dbmsType) {
        DbmsAdapter adapter = adapterMap.get(dbmsType);
        if (adapter == null) {
            throw new IllegalArgumentException("No DBMS adapter found for type: " + dbmsType);
        }
        return adapter;
    }
}
