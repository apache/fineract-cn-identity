/*
 * Copyright 2017 The Mifos Initiative.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mifos.identity.internal.repository;

import com.datastax.driver.core.DataType;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.schemabuilder.SchemaBuilder;
import com.datastax.driver.mapping.Mapper;
import io.mifos.core.cassandra.core.CassandraSessionProvider;
import io.mifos.core.cassandra.core.TenantAwareCassandraMapperProvider;
import io.mifos.core.cassandra.core.TenantAwareEntityTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
@Component
public class Roles {
  static final String TABLE_NAME = "isis_roles";
  static final String IDENTIFIER_COLUMN = "identifier";
  static final String PERMISSIONS_COLUMN = "permissions";

  private final CassandraSessionProvider cassandraSessionProvider;
  private final TenantAwareEntityTemplate tenantAwareEntityTemplate;
  private final TenantAwareCassandraMapperProvider tenantAwareCassandraMapperProvider;

  @Autowired
  Roles(
          final CassandraSessionProvider cassandraSessionProvider,
          final TenantAwareEntityTemplate tenantAwareEntityTemplate,
          final TenantAwareCassandraMapperProvider tenantAwareCassandraMapperProvider) {
    this.cassandraSessionProvider = cassandraSessionProvider;
    this.tenantAwareEntityTemplate = tenantAwareEntityTemplate;
    this.tenantAwareCassandraMapperProvider = tenantAwareCassandraMapperProvider;
  }

  public void buildTable() {

    final String table_statement =
            SchemaBuilder.createTable(TABLE_NAME)
                    .addPartitionKey(IDENTIFIER_COLUMN, DataType.text())
                    .addUDTListColumn(PERMISSIONS_COLUMN, SchemaBuilder.frozen(Permissions.TYPE_NAME))
                    .buildInternal();

    cassandraSessionProvider.getTenantSession().execute(table_statement);

  }

  public void add(final RoleEntity instance) {
    tenantAwareEntityTemplate.save(instance);
  }

  public void change(final RoleEntity instance) {
    tenantAwareEntityTemplate.save(instance);
  }

  public Optional<RoleEntity> get(final String identifier)
  {
    final RoleEntity instance =
            tenantAwareCassandraMapperProvider.getMapper(RoleEntity.class).get(identifier);

    if (instance != null) {
      Assert.notNull(instance.getIdentifier());
      Assert.notNull(instance.getPermissions());
    }

    return Optional.ofNullable(instance);
  }

  public void delete(final RoleEntity instance) {
    tenantAwareEntityTemplate.delete(instance);
  }

  public List<RoleEntity> getAll()
  {
    final Session tenantSession = cassandraSessionProvider.getTenantSession();
    final Mapper<RoleEntity> entityMapper = tenantAwareCassandraMapperProvider.getMapper(RoleEntity.class);

    final Statement statement = QueryBuilder.select().all().from(TABLE_NAME);

    return entityMapper.map(tenantSession.execute(statement)).all()
            .stream().collect(Collectors.toList());
  }
}
