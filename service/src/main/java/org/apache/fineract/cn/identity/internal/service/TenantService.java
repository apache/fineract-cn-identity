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
package org.apache.fineract.cn.identity.internal.service;

import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.List;
import java.util.Optional;
import org.apache.fineract.cn.anubis.api.v1.domain.ApplicationSignatureSet;
import org.apache.fineract.cn.anubis.api.v1.domain.Signature;
import org.apache.fineract.cn.anubis.config.TenantSignatureRepository;
import org.apache.fineract.cn.identity.internal.mapper.SignatureMapper;
import org.apache.fineract.cn.identity.internal.repository.PrivateSignatureEntity;
import org.apache.fineract.cn.identity.internal.repository.SignatureEntity;
import org.apache.fineract.cn.identity.internal.repository.Signatures;
import org.apache.fineract.cn.lang.security.RsaKeyPairFactory;
import org.apache.fineract.cn.lang.security.RsaPrivateKeyBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Myrle Krantz
 */
@Service
public class TenantService implements TenantSignatureRepository {
  private final Signatures signatures;

  @Autowired
  TenantService(final Signatures signatures)
  {
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
