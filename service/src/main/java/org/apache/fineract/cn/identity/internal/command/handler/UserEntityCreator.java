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
package org.apache.fineract.cn.identity.internal.command.handler;

import com.datastax.driver.core.LocalDate;
import java.nio.ByteBuffer;
import java.util.Optional;
import org.apache.fineract.cn.crypto.HashGenerator;
import org.apache.fineract.cn.crypto.SaltGenerator;
import org.apache.fineract.cn.identity.internal.repository.PrivateTenantInfoEntity;
import org.apache.fineract.cn.identity.internal.repository.Tenants;
import org.apache.fineract.cn.identity.internal.repository.UserEntity;
import org.apache.fineract.cn.identity.internal.util.IdentityConstants;
import org.apache.fineract.cn.identity.internal.util.Time;
import org.apache.fineract.cn.lang.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.util.EncodingUtils;
import org.springframework.stereotype.Component;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("WeakerAccess")
@Component
public class UserEntityCreator {

  private final SaltGenerator saltGenerator;
  private final HashGenerator hashGenerator;
  private final Tenants tenants;

  @Autowired UserEntityCreator(
      final SaltGenerator saltGenerator,
      final HashGenerator hashGenerator,
      final Tenants tenants)
  {
    this.saltGenerator = saltGenerator;
    this.hashGenerator = hashGenerator;
    this.tenants = tenants;
  }


  UserEntity build(
          final String identifier,
          final String role,
          final String password,
          final boolean passwordMustChange) {

    final Optional<PrivateTenantInfoEntity> tenantInfo = tenants.getPrivateTenantInfo();

    return tenantInfo
            .map(x -> build(identifier, role, password, passwordMustChange,
                    x.getFixedSalt().array(), x.getPasswordExpiresInDays()))
            .orElseThrow(() -> ServiceException.internalError("The tenant is not initialized."));
  }

  public  UserEntity build(
          final String identifier,
          final String role,
          final String password,
          final boolean passwordMustChange,
          final byte[] fixedSalt,
          final int passwordExpiresInDays)
  {
    final UserEntity userEntity = new UserEntity();

    userEntity.setIdentifier(identifier);
    userEntity.setRole(role);

    final byte[] variableSalt = this.saltGenerator.createRandomSalt();
    final byte[] fullSalt = EncodingUtils.concatenate(variableSalt, fixedSalt);

    userEntity.setPassword(ByteBuffer.wrap(this.hashGenerator.hash(password, fullSalt,
        IdentityConstants.ITERATION_COUNT, IdentityConstants.HASH_LENGTH)));

    userEntity.setSalt(ByteBuffer.wrap(variableSalt));
    userEntity.setIterationCount(IdentityConstants.ITERATION_COUNT);
    userEntity.setPasswordExpiresOn(deriveExpiration(passwordMustChange, passwordExpiresInDays));

    return userEntity;
  }

  private LocalDate deriveExpiration(final boolean passwordMustChange, final int passwordExpiresInDays) {
    final LocalDate now = Time.utcNowAsStaxLocalDate();

    if (passwordMustChange)
      return now;
    else {
      final int offset = (passwordExpiresInDays <= 0) ? 93 : passwordExpiresInDays;
      return LocalDate.fromDaysSinceEpoch(now.getDaysSinceEpoch() + offset);
    }
  }
}
