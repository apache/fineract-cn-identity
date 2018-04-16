/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.cn.identity.internal.repository;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.exceptions.InvalidQueryException;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import com.datastax.driver.core.schemabuilder.Create;
import com.datastax.driver.core.schemabuilder.SchemaBuilder;
import com.datastax.driver.core.schemabuilder.SchemaStatement;
import com.datastax.driver.mapping.Mapper;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.fineract.cn.cassandra.core.CassandraSessionProvider;
import org.apache.fineract.cn.cassandra.core.TenantAwareCassandraMapperProvider;
import org.apache.fineract.cn.lang.security.RsaKeyPairFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * All write accesses are synchronized.  These occur only during provisioning or key rotation, so they are not
 * performance critical.  These are only necessary because for some reason (that I have not yet been able to track
 * down), provisioning is being called multiple times in rapid succession for a tenant.
 *
 * All calls to cassandra which could conceivably be performed before provisioning is complete are surrounded by
 * a try-catch block for an InvalidQueryException.  If provisioning is not completed, the table is treated as empty.
 *
 * @author Myrle Krantz
 */
@Component
public class Signatures {
  static final String TABLE_NAME = "isis_signatures";
  private static final String INDEX_NAME = "isis_signatures_valid_index";
  static final String KEY_TIMESTAMP_COLUMN = "key_timestamp";
  static final String VALID_COLUMN = "valid";
  static final String PRIVATE_KEY_MOD_COLUMN = "private_key_mod";
  static final String PRIVATE_KEY_EXP_COLUMN = "private_key_exp";
  static final String PUBLIC_KEY_MOD_COLUMN = "public_key_mod";
  static final String PUBLIC_KEY_EXP_COLUMN = "public_key_exp";

  private final CassandraSessionProvider cassandraSessionProvider;
  private final TenantAwareCassandraMapperProvider tenantAwareCassandraMapperProvider;

  @Autowired
  public Signatures(
          final CassandraSessionProvider cassandraSessionProvider,
          final TenantAwareCassandraMapperProvider tenantAwareCassandraMapperProvider) {
    this.cassandraSessionProvider = cassandraSessionProvider;
    this.tenantAwareCassandraMapperProvider = tenantAwareCassandraMapperProvider;
  }

  public synchronized void buildTable() {
    final Create create = SchemaBuilder.createTable(TABLE_NAME)
        .ifNotExists()
        .addPartitionKey(KEY_TIMESTAMP_COLUMN, DataType.text())
        .addColumn(VALID_COLUMN, DataType.cboolean())
        .addColumn(PRIVATE_KEY_MOD_COLUMN, DataType.varint())
        .addColumn(PRIVATE_KEY_EXP_COLUMN, DataType.varint())
        .addColumn(PUBLIC_KEY_MOD_COLUMN, DataType.varint())
        .addColumn(PUBLIC_KEY_EXP_COLUMN, DataType.varint());

    cassandraSessionProvider.getTenantSession().execute(create);

    final SchemaStatement createValidIndex = SchemaBuilder.createIndex(INDEX_NAME)
        .ifNotExists()
        .onTable(TABLE_NAME)
        .andColumn(VALID_COLUMN);

    cassandraSessionProvider.getTenantSession().execute(createValidIndex);
  }

  public synchronized SignatureEntity add(final RsaKeyPairFactory.KeyPairHolder keys)
  {
    //There will only be one entry in this table.
    final BoundStatement tenantCreationStatement =
            cassandraSessionProvider.getTenantSession().prepare("INSERT INTO " + TABLE_NAME + " ("
                    + KEY_TIMESTAMP_COLUMN + ", "
                    + VALID_COLUMN + ", "
                    + PRIVATE_KEY_MOD_COLUMN + ", "
                    + PRIVATE_KEY_EXP_COLUMN + ", "
                    + PUBLIC_KEY_MOD_COLUMN + ", "
                    + PUBLIC_KEY_EXP_COLUMN + ")"
                    + "VALUES (?, ?, ?, ?, ?, ?)").bind();

    tenantCreationStatement.setString(KEY_TIMESTAMP_COLUMN, keys.getTimestamp());

    tenantCreationStatement.setBool(VALID_COLUMN, true);
    tenantCreationStatement.setVarint(PRIVATE_KEY_MOD_COLUMN, keys.getPrivateKeyMod());
    tenantCreationStatement.setVarint(PRIVATE_KEY_EXP_COLUMN, keys.getPrivateKeyExp());
    tenantCreationStatement.setVarint(PUBLIC_KEY_MOD_COLUMN, keys.getPublicKeyMod());
    tenantCreationStatement.setVarint(PUBLIC_KEY_EXP_COLUMN, keys.getPublicKeyExp());

    cassandraSessionProvider.getTenantSession().execute(tenantCreationStatement);

    final SignatureEntity ret = new SignatureEntity();
    ret.setKeyTimestamp(keys.getTimestamp());
    ret.setPublicKeyMod(keys.getPublicKeyMod());
    ret.setPublicKeyExp(keys.getPublicKeyExp());
    ret.setValid(true);

    return ret;
  }

  public Optional<SignatureEntity> getSignature(final String keyTimestamp) {
    try {
      final Mapper<SignatureEntity> signatureEntityMapper
          = tenantAwareCassandraMapperProvider.getMapper(SignatureEntity.class);

      final Optional<SignatureEntity> ret = Optional.ofNullable(signatureEntityMapper.get(keyTimestamp));
      return ret.filter(SignatureEntity::getValid);
    }
    catch (final InvalidQueryException e) {
      return Optional.empty();
    }
  }

  /**
   * @return the most current valid private key pair with key timestamp.  If there are no valid key pairs, returns Optional.empty.
   */
  public Optional<PrivateSignatureEntity> getPrivateSignature()
  {
    final Optional<String> maximumKeyTimestamp = streamValidKeyTimestamps().max(String::compareTo);

    return maximumKeyTimestamp.flatMap(this::getPrivateSignatureEntity);
  }

  private Optional<PrivateSignatureEntity> getPrivateSignatureEntity(final String keyTimestamp) {
    try {
      final Mapper<PrivateSignatureEntity> privateSignatureEntityMapper
          = tenantAwareCassandraMapperProvider.getMapper(PrivateSignatureEntity.class);

      final Optional<PrivateSignatureEntity> ret = Optional.ofNullable(privateSignatureEntityMapper.get(keyTimestamp));
      return ret.filter(PrivateSignatureEntity::getValid);
    }
    catch (final InvalidQueryException e) {
      return Optional.empty();
    }
  }

  public List<String> getAllKeyTimestamps() {
      return streamValidKeyTimestamps().collect(Collectors.toList());
  }

  private Stream<String> streamValidKeyTimestamps() {
    try {
      final Select.Where selectValid = QueryBuilder.select(KEY_TIMESTAMP_COLUMN)
          .from(TABLE_NAME)
          .where(QueryBuilder.eq(VALID_COLUMN, true));
      final ResultSet result = cassandraSessionProvider.getTenantSession().execute(selectValid);
      return StreamSupport.stream(result.spliterator(), false)
          .map(x -> x.get(KEY_TIMESTAMP_COLUMN, String.class));
    }
    catch (final InvalidQueryException e) {
      return Stream.empty();
    }
  }

  public synchronized void invalidateEntry(final String keyTimestamp) {
    final Update.Assignments updateQuery = QueryBuilder.update(TABLE_NAME).where(QueryBuilder.eq(KEY_TIMESTAMP_COLUMN, keyTimestamp)).with(QueryBuilder.set(VALID_COLUMN, false));
    cassandraSessionProvider.getTenantSession().execute(updateQuery);
  }
}
