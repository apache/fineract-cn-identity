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

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.schemabuilder.SchemaBuilder;
import com.datastax.driver.mapping.Mapper;
import io.mifos.core.cassandra.core.CassandraSessionProvider;
import io.mifos.core.cassandra.core.TenantAwareCassandraMapperProvider;
import io.mifos.core.cassandra.core.TenantAwareEntityTemplate;
import io.mifos.core.lang.security.RsaKeyPairFactory;
import io.mifos.identity.internal.util.IdentityConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * @author Myrle Krantz
 */
@Component
public class Tenants {
  static final String TABLE_NAME = "isis_tenant";
  static final String VERSION_COLUMN = "version";
  static final String FIXED_SALT_COLUMN = "fixed_salt";
  static final String PRIVATE_KEY_MOD_COLUMN = "private_key_mod";
  static final String PRIVATE_KEY_EXP_COLUMN = "private_key_exp";
  static final String PUBLIC_KEY_MOD_COLUMN = "public_key_mod";
  static final String PUBLIC_KEY_EXP_COLUMN = "public_key_exp";
  static final String PASSWORD_EXPIRES_IN_DAYS_COLUMN = "password_expires_in_days";
  static final String TIME_TO_CHANGE_PASSWORD_AFTER_EXPIRATION_IN_DAYS = "time_to_change_password_after_expiration_in_days";

  private final CassandraSessionProvider cassandraSessionProvider;
  private final TenantAwareEntityTemplate tenantAwareEntityTemplate;
  private final TenantAwareCassandraMapperProvider tenantAwareCassandraMapperProvider;

  @Autowired
  Tenants(final CassandraSessionProvider cassandraSessionProvider,
          final TenantAwareEntityTemplate tenantAwareEntityTemplate,
          final TenantAwareCassandraMapperProvider tenantAwareCassandraMapperProvider)
  {
    this.cassandraSessionProvider = cassandraSessionProvider;
    this.tenantAwareEntityTemplate = tenantAwareEntityTemplate;
    this.tenantAwareCassandraMapperProvider = tenantAwareCassandraMapperProvider;
  }

  public void buildTable() {
    final String statement =
        SchemaBuilder.createTable(TABLE_NAME)
            .addPartitionKey(VERSION_COLUMN, DataType.cint())
            .addColumn(FIXED_SALT_COLUMN, DataType.blob())
            .addColumn(PRIVATE_KEY_MOD_COLUMN, DataType.varint())
            .addColumn(PRIVATE_KEY_EXP_COLUMN, DataType.varint())
            .addColumn(PUBLIC_KEY_MOD_COLUMN, DataType.varint())
            .addColumn(PUBLIC_KEY_EXP_COLUMN, DataType.varint())
            .addColumn(PASSWORD_EXPIRES_IN_DAYS_COLUMN, DataType.cint())
            .addColumn(TIME_TO_CHANGE_PASSWORD_AFTER_EXPIRATION_IN_DAYS, DataType.cint())
            .buildInternal();

    cassandraSessionProvider.getTenantSession().execute(statement);
  }

  public void add(
          final byte[] fixedSalt,
          final RsaKeyPairFactory.KeyPairHolder keys,
          final int passwordExpiresInDays,
          final int timeToChangePasswordAfterExpirationInDays)
  {
    //There will only be one entry in this table.
    final BoundStatement tenantCreationStatement =
        cassandraSessionProvider.getTenantSession().prepare("INSERT INTO " + Tenants.TABLE_NAME + " ("
            + VERSION_COLUMN + ", "
            + FIXED_SALT_COLUMN + ", "
            + PRIVATE_KEY_MOD_COLUMN + ", "
            + PRIVATE_KEY_EXP_COLUMN + ", "
            + PUBLIC_KEY_MOD_COLUMN + ", "
            + PUBLIC_KEY_EXP_COLUMN + ", "
            + PASSWORD_EXPIRES_IN_DAYS_COLUMN + ", "
            + TIME_TO_CHANGE_PASSWORD_AFTER_EXPIRATION_IN_DAYS + ")"
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)").bind();

    tenantCreationStatement.setInt(VERSION_COLUMN, IdentityConstants.CURRENT_VERSION);

    tenantCreationStatement.setBytes(FIXED_SALT_COLUMN, ByteBuffer.wrap(fixedSalt));

    tenantCreationStatement.setVarint(PRIVATE_KEY_MOD_COLUMN, keys.getPrivateKeyMod());
    tenantCreationStatement.setVarint(PRIVATE_KEY_EXP_COLUMN, keys.getPrivateKeyExp());
    tenantCreationStatement.setVarint(PUBLIC_KEY_MOD_COLUMN, keys.getPublicKeyMod());
    tenantCreationStatement.setVarint(PUBLIC_KEY_EXP_COLUMN, keys.getPublicKeyExp());
    tenantCreationStatement.setInt(PASSWORD_EXPIRES_IN_DAYS_COLUMN, passwordExpiresInDays);
    tenantCreationStatement.setInt(TIME_TO_CHANGE_PASSWORD_AFTER_EXPIRATION_IN_DAYS, timeToChangePasswordAfterExpirationInDays);


    cassandraSessionProvider.getTenantSession().execute(tenantCreationStatement);
  }

  public Optional<PrivateTenantInfoEntity> getPrivateTenantInfo()
  {
    return tenantAwareEntityTemplate
        .findById(PrivateTenantInfoEntity.class, IdentityConstants.CURRENT_VERSION);
  }

  public SignatureEntity getSignature() {
    final Mapper<SignatureEntity> signatureEntityMapper
            = tenantAwareCassandraMapperProvider.getMapper(SignatureEntity.class);

    return signatureEntityMapper.get(IdentityConstants.CURRENT_VERSION);
  }

  public boolean currentTenantAlreadyProvisioned() {
    final String keyspace = cassandraSessionProvider.getTenantSession().getLoggedKeyspace();
    final KeyspaceMetadata keyspaceMetadata = cassandraSessionProvider.getTenantSession()
            .getCluster().getMetadata().getKeyspace(keyspace);

    return keyspaceMetadata.getTable(TABLE_NAME) != null;
  }
}
