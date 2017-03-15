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
import io.mifos.identity.internal.util.IdentityConstants;

import java.math.BigInteger;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@Table(name = Tenants.TABLE_NAME)
public class SignatureEntity {
  @PartitionKey
  @Column(name = Tenants.VERSION_COLUMN)
  private int version;

  @Column(name = Tenants.PUBLIC_KEY_MOD_COLUMN)
  private BigInteger publicKeyMod;
  @Column(name = Tenants.PUBLIC_KEY_EXP_COLUMN)
  private BigInteger publicKeyExp;

  public SignatureEntity() { }

  public SignatureEntity(final BigInteger publicKeyMod, final BigInteger publicKeyExp) {
    this.version = IdentityConstants.CURRENT_VERSION;
    this.publicKeyMod = publicKeyMod;
    this.publicKeyExp = publicKeyExp;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public BigInteger getPublicKeyMod() {
    return publicKeyMod;
  }

  public void setPublicKeyMod(BigInteger publicKeyMod) {
    this.publicKeyMod = publicKeyMod;
  }

  public BigInteger getPublicKeyExp() {
    return publicKeyExp;
  }

  public void setPublicKeyExp(BigInteger publicKeyExp) {
    this.publicKeyExp = publicKeyExp;
  }
}
