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
import io.mifos.anubis.api.v1.domain.ApplicationSignatureSet;
import io.mifos.anubis.api.v1.domain.Signature;
import io.mifos.anubis.config.TenantSignatureRepository;
import io.mifos.core.lang.security.RsaKeyPairFactory;
import io.mifos.core.lang.security.RsaPrivateKeyBuilder;
import io.mifos.identity.internal.mapper.SignatureMapper;
import io.mifos.identity.internal.repository.PrivateSignatureEntity;
import io.mifos.identity.internal.repository.SignatureEntity;
import io.mifos.identity.internal.repository.Signatures;
import io.mifos.identity.internal.repository.Tenants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.List;
import java.util.Optional;

/**
 * @author Myrle Krantz
 */
@Service
public class TenantService implements TenantSignatureRepository {
  private final Tenants tenants;
  private final Signatures signatures;

  @Autowired
  TenantService(final Tenants tenants, final Signatures signatures)
  {
    this.tenants = tenants;
    this.signatures = signatures;
  }

  public Optional<Signature> getIdentityManagerSignature(final String keyTimestamp) {
    final Optional<SignatureEntity> signature = signatures.getSignature(keyTimestamp);
    return signature.map(x -> new Signature(x.getPublicKeyMod(), x.getPublicKeyExp()));
  }

  @Override
  public List<String> getAllSignatureSetKeyTimestamps() {
    return signatures.getAllKeyTimestamps();
  }

  @Override
  public Optional<ApplicationSignatureSet> getSignatureSet(final String keyTimestamp) {
    final Optional<SignatureEntity> signatureEntity = signatures.getSignature(keyTimestamp);
    return signatureEntity.map(SignatureMapper::mapToApplicationSignatureSet);
  }

  @Override
  public void deleteSignatureSet(final String keyTimestamp) {
    signatures.invalidateEntry(keyTimestamp);
  }

  @Override
  public Optional<Signature> getApplicationSignature(final String keyTimestamp) {
    final Optional<SignatureEntity> signatureEntity = signatures.getSignature(keyTimestamp);
    return signatureEntity.map(x -> new Signature(x.getPublicKeyMod(), x.getPublicKeyExp()));
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

  public ApplicationSignatureSet createSignatureSet() {
    final RsaKeyPairFactory.KeyPairHolder keys = RsaKeyPairFactory.createKeyPair();
    final SignatureEntity signatureEntity = signatures.add(keys);
    return SignatureMapper.mapToApplicationSignatureSet(signatureEntity);
  }

  @Override
  public Optional<ApplicationSignatureSet> getLatestSignatureSet() {
    Optional<String> timestamp = getMostRecentTimestamp();
    return timestamp.flatMap(this::getSignatureSet);
  }

  @Override
  public Optional<Signature> getLatestApplicationSignature() {
    Optional<String> timestamp = getMostRecentTimestamp();
    return timestamp.flatMap(this::getApplicationSignature);
  }

  @Override
  public Optional<RsaKeyPairFactory.KeyPairHolder> getLatestApplicationSigningKeyPair() {
    final Optional<PrivateSignatureEntity> privateSignatureEntity = signatures.getPrivateSignature();
    return privateSignatureEntity.map(x -> {
      final String timestamp = x.getKeyTimestamp();
      final PrivateKey privateKey = new RsaPrivateKeyBuilder()
              .setPrivateKeyExp(x.getPrivateKeyExp())
              .setPrivateKeyMod(x.getPrivateKeyMod())
              .build();
      return new RsaKeyPairFactory.KeyPairHolder(timestamp, null, (RSAPrivateKey)privateKey);
    });
  }

  private Optional<String> getMostRecentTimestamp() {
    return getAllSignatureSetKeyTimestamps().stream()
            .max(String::compareTo);
  }
}
