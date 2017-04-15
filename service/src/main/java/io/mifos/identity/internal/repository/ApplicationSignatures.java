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
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
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
public class ApplicationSignatures {
  static final java.lang.String TABLE_NAME = "isis_application_signatures";
  static final String APPLICATION_IDENTIFIER_COLUMN = "application_identifier";
  static final String KEY_TIMESTAMP_COLUMN = "key_timestamp";
  static final String PUBLIC_KEY_MOD_COLUMN = "public_key_mod";
  static final String PUBLIC_KEY_EXP_COLUMN = "public_key_exp";

  private final CassandraSessionProvider cassandraSessionProvider;
  private final TenantAwareEntityTemplate tenantAwareEntityTemplate;
  private final TenantAwareCassandraMapperProvider tenantAwareCassandraMapperProvider;

  @Autowired
  public ApplicationSignatures(final CassandraSessionProvider cassandraSessionProvider,
                               final TenantAwareEntityTemplate tenantAwareEntityTemplate,
                               final TenantAwareCassandraMapperProvider tenantAwareCassandraMapperProvider) {
    this.cassandraSessionProvider = cassandraSessionProvider;
    this.tenantAwareEntityTemplate = tenantAwareEntityTemplate;
    this.tenantAwareCassandraMapperProvider = tenantAwareCassandraMapperProvider;
  }

  public void buildTable()
  {
    final Create createTable = SchemaBuilder.createTable(TABLE_NAME)
            .addPartitionKey(APPLICATION_IDENTIFIER_COLUMN, DataType.text())
            .addClusteringColumn(KEY_TIMESTAMP_COLUMN, DataType.text())
            .addColumn(PUBLIC_KEY_MOD_COLUMN, DataType.varint())
            .addColumn(PUBLIC_KEY_EXP_COLUMN, DataType.varint());

    cassandraSessionProvider.getTenantSession().execute(createTable);
  }

  public void add(final ApplicationSignatureEntity entity) {
    tenantAwareEntityTemplate.save(entity);
  }

  public Optional<ApplicationSignatureEntity> get(final String applicationIdentifier, final String keyTimestamp)
  {
    final ApplicationSignatureEntity entity =
            tenantAwareCassandraMapperProvider.getMapper(ApplicationSignatureEntity.class).get(applicationIdentifier, keyTimestamp);

    if (entity != null) {
      Assert.notNull(entity.getApplicationIdentifier());
      Assert.notNull(entity.getKeyTimestamp());
      Assert.notNull(entity.getPublicKeyMod());
      Assert.notNull(entity.getPublicKeyExp());
    }

    return Optional.ofNullable(entity);
  }

  public List<ApplicationSignatureEntity> getAll() {
    final Mapper<ApplicationSignatureEntity> entityMapper = tenantAwareCassandraMapperProvider.getMapper(ApplicationSignatureEntity.class);
    final Session tenantSession = cassandraSessionProvider.getTenantSession();

    final Statement statement = QueryBuilder.select().all().from(TABLE_NAME);

    return entityMapper.map(tenantSession.execute(statement)).all();
  }

  public void delete(final String applicationIdentifier) {
    final Delete.Where deleteStatement = QueryBuilder.delete().from(TABLE_NAME)
            .where(QueryBuilder.eq(APPLICATION_IDENTIFIER_COLUMN, applicationIdentifier));
    cassandraSessionProvider.getTenantSession().execute(deleteStatement);
  }

  public boolean signaturesExistForApplication(final String applicationIdentifier) {
    final Select.Where selectStatement = QueryBuilder.select().from(TABLE_NAME)
            .where(QueryBuilder.eq(APPLICATION_IDENTIFIER_COLUMN, applicationIdentifier));
    final ResultSet selected = cassandraSessionProvider.getTenantSession().execute(selectStatement);
    final int count = selected.getAvailableWithoutFetching();
    return count > 0;
  }
}