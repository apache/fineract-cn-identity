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

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.nio.ByteBuffer;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@Table(name = Tenants.TABLE_NAME)
public class PrivateTenantInfoEntity {
  @PartitionKey
  @Column(name = Tenants.VERSION_COLUMN)
  private int version;

  @Column(name = Tenants.FIXED_SALT_COLUMN)
  private ByteBuffer fixedSalt;

  @Column(name = Tenants.PASSWORD_EXPIRES_IN_DAYS_COLUMN)
  private int passwordExpiresInDays;

  @Column(name = Tenants.TIME_TO_CHANGE_PASSWORD_AFTER_EXPIRATION_IN_DAYS)
  private int timeToChangePasswordAfterExpirationInDays;

  public PrivateTenantInfoEntity() { }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public ByteBuffer getFixedSalt() {
    return fixedSalt;
  }

  public void setFixedSalt(ByteBuffer fixedSalt) {
    this.fixedSalt = fixedSalt;
  }

  public int getPasswordExpiresInDays() {
    return passwordExpiresInDays;
  }

  public void setPasswordExpiresInDays(final int passwordExpiresInDays) {
    this.passwordExpiresInDays = passwordExpiresInDays;
  }

  public int getTimeToChangePasswordAfterExpirationInDays() {
    return timeToChangePasswordAfterExpirationInDays;
  }

  public void setTimeToChangePasswordAfterExpirationInDays(int timeToChangePasswordAfterExpirationInDays) {
    this.timeToChangePasswordAfterExpirationInDays = timeToChangePasswordAfterExpirationInDays;
  }
}
