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

import com.datastax.driver.core.LocalDate;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.nio.ByteBuffer;

/**
 * @author Myrle Krantz
 */
@Table(name = Users.TABLE_NAME)
public class UserEntity {
  @PartitionKey
  @Column(name = Users.IDENTIFIER_COLUMN)
  private String identifier;
  @Column(name = Users.ROLE_COLUMN)
  private String role;
  @Column(name = Users.PASSWORD_COLUMN)
  private ByteBuffer password;
  @Column(name = Users.SALT_COLUMN)
  private ByteBuffer salt;
  @Column(name = Users.ITERATION_COUNT_COLUMN)
  private int iterationCount;
  @Column(name = Users.PASSWORD_EXPIRES_ON_COLUMN)
  private LocalDate passwordExpiresOn;

  public UserEntity() {}

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public ByteBuffer getPassword() {
    return password;
  }

  public void setPassword(ByteBuffer password) {
    this.password = password;
  }

  public ByteBuffer getSalt() {
    return salt;
  }

  public void setSalt(ByteBuffer salt) {
    this.salt = salt;
  }

  public int getIterationCount() {
    return iterationCount;
  }

  public void setIterationCount(int iterationCount) {
    this.iterationCount = iterationCount;
  }

  public LocalDate getPasswordExpiresOn() {
    return passwordExpiresOn;
  }

  public void setPasswordExpiresOn(LocalDate passwordExpiresOn) {
    this.passwordExpiresOn = passwordExpiresOn;
  }
}
