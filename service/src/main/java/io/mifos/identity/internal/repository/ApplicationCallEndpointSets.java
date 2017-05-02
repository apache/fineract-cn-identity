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
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;

/**
 * @author Myrle Krantz
 */
@Component
public class ApplicationCallEndpointSets {
  static final String TABLE_NAME = "isis_application_callendpointsets";
  static final String APPLICATION_IDENTIFIER_COLUMN = "application_identifier";
  static final String CALLENDPOINTSET_IDENTIFIER_COLUMN = "call_endpoint_set_identifier";
  static final String CALLENDPOINT_GROUP_IDENTIFIERS_COLUMN = "call_endpoint_group_identifiers";

  private final CassandraSessionProvider cassandraSessionProvider;
  private final TenantAwareEntityTemplate tenantAwareEntityTemplate;
  private final TenantAwareCassandraMapperProvider tenantAwareCassandraMapperProvider;

  @Autowired
  public ApplicationCallEndpointSets(
          final CassandraSessionProvider cassandraSessionProvider,
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
                    .addClusteringColumn(CALLENDPOINTSET_IDENTIFIER_COLUMN, DataType.text())
                    .addColumn(CALLENDPOINT_GROUP_IDENTIFIERS_COLUMN, DataType.list(DataType.text()));

    cassandraSessionProvider.getTenantSession().execute(createTableStatement);
  }

  public void add(final ApplicationCallEndpointSetEntity entity) {
    tenantAwareEntityTemplate.save(entity);
  }

  public void change(final ApplicationCallEndpointSetEntity instance) {
    tenantAwareEntityTemplate.save(instance);
  }

  public Optional<ApplicationCallEndpointSetEntity> get(final String applicationIdentifier, final String callEndpointSetIdentifier)
  {
    final ApplicationCallEndpointSetEntity entity =
            tenantAwareCassandraMapperProvider.getMapper(ApplicationCallEndpointSetEntity.class).get(applicationIdentifier, callEndpointSetIdentifier);

    if (entity != null) {
      Assert.notNull(entity.getApplicationIdentifier());
      Assert.notNull(entity.getCallEndpointSetIdentifier());
    }

    return Optional.ofNullable(entity);
  }

  public List<ApplicationCallEndpointSetEntity> getAllForApplication(final String applicationIdentifier) {
    final Mapper<ApplicationCallEndpointSetEntity> entityMapper = tenantAwareCassandraMapperProvider.getMapper(ApplicationCallEndpointSetEntity.class);
    final Session tenantSession = cassandraSessionProvider.getTenantSession();

    final Statement statement = QueryBuilder.select().from(TABLE_NAME).where(QueryBuilder.eq(APPLICATION_IDENTIFIER_COLUMN, applicationIdentifier));

    return entityMapper.map(tenantSession.execute(statement)).all();
  }

  public void delete(final String applicationIdentifier, final String callEndpointSetIdentifier) {
    final Optional<ApplicationCallEndpointSetEntity> toDelete = tenantAwareEntityTemplate.findById(ApplicationCallEndpointSetEntity.class, applicationIdentifier, callEndpointSetIdentifier);
    toDelete.ifPresent(tenantAwareEntityTemplate::delete);
  }
}
