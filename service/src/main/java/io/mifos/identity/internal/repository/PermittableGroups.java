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
public class PermittableGroups {
  static final String TABLE_NAME = "isis_permittable_groups";
  static final String IDENTIFIER_COLUMN = "identifier";
  static final String PERMITTABLES_COLUMN = "permittables";

  static final String TYPE_NAME = "isis_permittable_group";
  static final String PATH_FIELD = "path";
  static final String METHOD_FIELD = "method";
  static final String SOURCE_GROUP_ID_FIELD = "source_group_id";

  private final CassandraSessionProvider cassandraSessionProvider;
  private final TenantAwareEntityTemplate tenantAwareEntityTemplate;
  private final TenantAwareCassandraMapperProvider tenantAwareCassandraMapperProvider;

  @Autowired
  PermittableGroups(
          final CassandraSessionProvider cassandraSessionProvider,
          final TenantAwareEntityTemplate tenantAwareEntityTemplate,
          final TenantAwareCassandraMapperProvider tenantAwareCassandraMapperProvider) {
    this.cassandraSessionProvider = cassandraSessionProvider;
    this.tenantAwareEntityTemplate = tenantAwareEntityTemplate;
    this.tenantAwareCassandraMapperProvider = tenantAwareCassandraMapperProvider;
  }

  public void buildTable() {
    final String type_statement =
            SchemaBuilder.createType(TYPE_NAME)
                    .addColumn(PATH_FIELD, DataType.text())
                    .addColumn(METHOD_FIELD, DataType.text())
                    .addColumn(SOURCE_GROUP_ID_FIELD, DataType.text())
                    .buildInternal();
    cassandraSessionProvider.getTenantSession().execute(type_statement);

    final String table_statement =
            SchemaBuilder.createTable(TABLE_NAME)
                    .addPartitionKey(IDENTIFIER_COLUMN, DataType.text())
                    .addUDTListColumn(PERMITTABLES_COLUMN, SchemaBuilder.frozen(TYPE_NAME))
                    .buildInternal();

    cassandraSessionProvider.getTenantSession().execute(table_statement);

  }

  public void add(final PermittableGroupEntity instance) {
    tenantAwareEntityTemplate.save(instance);
  }

  public Optional<PermittableGroupEntity> get(final String identifier)
  {
    final PermittableGroupEntity instance =
            tenantAwareCassandraMapperProvider.getMapper(PermittableGroupEntity.class).get(identifier);

    if (instance != null) {
      Assert.notNull(instance.getIdentifier());
    }

    return Optional.ofNullable(instance);
  }

  public List<PermittableGroupEntity> getAll() {
    final Session tenantSession = cassandraSessionProvider.getTenantSession();
    final Mapper<PermittableGroupEntity> entityMapper = tenantAwareCassandraMapperProvider.getMapper(PermittableGroupEntity.class);

    final Statement statement = QueryBuilder.select().all().from(TABLE_NAME);

    return entityMapper.map(tenantSession.execute(statement)).all()
            .stream().collect(Collectors.toList());
  }
}
