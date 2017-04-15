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
import com.datastax.driver.core.schemabuilder.Create;
import com.datastax.driver.core.schemabuilder.SchemaBuilder;
import io.mifos.core.cassandra.core.CassandraSessionProvider;
import io.mifos.core.cassandra.core.TenantAwareEntityTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Myrle Krantz
 */
@Component
public class ApplicationPermissionUsers {
  static final String TABLE_NAME = "isis_application_permission_users";
  static final String APPLICATION_IDENTIFIER_COLUMN = "application_identifier";
  static final String PERMITTABLE_GROUP_IDENTIFIER_COLUMN = "permittable_group_identifier";
  static final String USER_IDENTIFIER_COLUMN = "user_identifier";
  static final String ENABLED_COLUMN = "enabled";
  private final CassandraSessionProvider cassandraSessionProvider;
  private final TenantAwareEntityTemplate tenantAwareEntityTemplate;

  @Autowired
  public ApplicationPermissionUsers(final CassandraSessionProvider cassandraSessionProvider,
                                    final TenantAwareEntityTemplate tenantAwareEntityTemplate) {
    this.cassandraSessionProvider = cassandraSessionProvider;
    this.tenantAwareEntityTemplate = tenantAwareEntityTemplate;
  }

  public void buildTable() {

    final Create createTableStatement =
            SchemaBuilder.createTable(TABLE_NAME)
                    .addPartitionKey(APPLICATION_IDENTIFIER_COLUMN, DataType.text())
                    .addClusteringColumn(PERMITTABLE_GROUP_IDENTIFIER_COLUMN, DataType.text())
                    .addClusteringColumn(USER_IDENTIFIER_COLUMN, DataType.text())
                    .addColumn(ENABLED_COLUMN, DataType.cboolean());

    cassandraSessionProvider.getTenantSession().execute(createTableStatement);
  }

  public boolean enabled(final String applicationIdentifier,
                         final String permittableEndpointGroupIdentifier,
                         final String userIdentifier) {
    return tenantAwareEntityTemplate.findById(
            ApplicationPermissionUsersEntity.class, applicationIdentifier, permittableEndpointGroupIdentifier, userIdentifier)
            .map(ApplicationPermissionUsersEntity::getEnabled)
            .orElse(false);
  }

  public void setEnabled(final String applicationIdentifier,
                         final String permittableGroupIdentifier,
                         final String userIdentifier,
                         final boolean enabled) {
    tenantAwareEntityTemplate.save(new ApplicationPermissionUsersEntity(applicationIdentifier, permittableGroupIdentifier, userIdentifier, enabled));
  }
}
