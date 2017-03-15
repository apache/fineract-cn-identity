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
package io.mifos.identity.internal.service;

import com.datastax.driver.core.exceptions.InvalidQueryException;
import io.mifos.anubis.api.v1.TokenConstants;
import io.mifos.anubis.api.v1.domain.Signature;
import io.mifos.anubis.config.TenantSignatureProvider;
import io.mifos.identity.internal.repository.SignatureEntity;
import io.mifos.identity.internal.repository.Tenants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author Myrle Krantz
 */
@Service
public class TenantService implements TenantSignatureProvider {
  private final Tenants tenants;

  @Autowired TenantService(final Tenants tenants)
  {
    this.tenants = tenants;
  }

  public Optional<Signature> getSignature(final String version)
  {
    if (!version.equals(TokenConstants.VERSION))
      return Optional.empty();

    return Optional.of(getSignature());
  }

  public Signature getSignature() {
    final SignatureEntity signature = tenants.getSignature();
    return new Signature(signature.getPublicKeyMod(), signature.getPublicKeyExp());
  }

  public boolean tenantAlreadyProvisioned() {
    try {
      return tenants.currentTenantAlreadyProvisioned();
    }
    catch (final InvalidQueryException e)
    {
      return false;
    }
  }
}
