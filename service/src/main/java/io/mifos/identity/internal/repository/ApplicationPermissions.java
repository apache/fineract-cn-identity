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
import com.datastax.driver.core.schemabuilder.Create;
import com.datastax.driver.core.schemabuilder.SchemaBuilder;
import com.datastax.driver.mapping.Mapper;
import io.mifos.core.cassandra.core.CassandraSessionProvider;
import io.mifos.core.cassandra.core.TenantAwareCassandraMapperProvider;
import io.mifos.core.cassandra.core.TenantAwareEntityTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
@Component
public class ApplicationPermissions {
  static final String TABLE_NAME = "isis_application_permissions";
  static final String APPLICATION_IDENTIFIER_COLUMN = "application_identifier";
  static final String PERMITTABLE_GROUP_IDENTIFIER_COLUMN = "permittable_group_identifier";
  static final String PERMISSION_COLUMN = "permission";

  private final CassandraSessionProvider cassandraSessionProvider;
  private final TenantAwareEntityTemplate tenantAwareEntityTemplate;
  private final TenantAwareCassandraMapperProvider tenantAwareCassandraMapperProvider;

  @Autowired
  public ApplicationPermissions(final CassandraSessionProvider cassandraSessionProvider,
                                final TenantAwareEntityTemplate tenantAwareEntityTemplate,
                                final TenantAwareCassandraMapperProvider tenantAwareCassandraMapperProvider) {
    this.cassandraSessionProvider = cassandraSessionProvider;
    this.tenantAwareEntityTemplate = tenantAwareEntityTemplate;
    this.tenantAwareCassandraMapperProvider = tenantAwareCassandraMapperProvider;
  }

  public void buildTable() {

    final Create createTableStatement =
            SchemaBuilder.createTable(TABLE_NAME)
                    .addPartitionKey(APPLICATION_IDENTIFIER_COLUMN, DataType.text())
                    .addClusteringColumn(PERMITTABLE_GROUP_IDENTIFIER_COLUMN, DataType.text())
                    .addUDTColumn(PERMISSION_COLUMN, SchemaBuilder.frozen(Permissions.TYPE_NAME));

    cassandraSessionProvider.getTenantSession().execute(createTableStatement);

  }

  public void add(final ApplicationPermissionEntity entity) {
    tenantAwareEntityTemplate.save(entity);
  }

  public boolean exists(final String applicationIdentifier, final String permittableGroupIdentifier) {
    return tenantAwareEntityTemplate.findById(ApplicationPermissionEntity.class, applicationIdentifier, permittableGroupIdentifier).isPresent();
  }

  public List<PermissionType> getAllPermissionsForApplication(final String applicationIdentifier) {
    final List<ApplicationPermissionEntity> result = getAllApplicationPermissionEntitiesForApplication(applicationIdentifier);
    return result.stream().map(ApplicationPermissionEntity::getPermission).collect(Collectors.toList());
  }

  private List<ApplicationPermissionEntity> getAllApplicationPermissionEntitiesForApplication(final String applicationIdentifier) {
    final Mapper<ApplicationPermissionEntity> entityMapper = tenantAwareCassandraMapperProvider.getMapper(ApplicationPermissionEntity.class);
    final Session tenantSession = cassandraSessionProvider.getTenantSession();

    final Statement statement = QueryBuilder.select().from(TABLE_NAME).where(QueryBuilder.eq(APPLICATION_IDENTIFIER_COLUMN, applicationIdentifier));

    return entityMapper.map(tenantSession.execute(statement)).all();
  }

  public void delete(final String applicationIdentifier, final String permittableGroupIdentifier) {
    final Optional<ApplicationPermissionEntity> toDelete = tenantAwareEntityTemplate.findById(ApplicationPermissionEntity.class, applicationIdentifier, permittableGroupIdentifier);
    toDelete.ifPresent(tenantAwareEntityTemplate::delete);
  }

  public Optional<PermissionType> getPermissionForApplication(
          final String applicationIdentifier,
          final String permittableEndpointGroupIdentifier) {

    return tenantAwareEntityTemplate
            .findById(ApplicationPermissionEntity.class, applicationIdentifier, permittableEndpointGroupIdentifier)
            .map(ApplicationPermissionEntity::getPermission);
  }
}