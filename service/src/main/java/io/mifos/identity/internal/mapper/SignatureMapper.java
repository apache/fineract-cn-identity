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
package io.mifos.identity.internal.mapper;

import io.mifos.anubis.api.v1.domain.ApplicationSignatureSet;
import io.mifos.anubis.api.v1.domain.Signature;
import io.mifos.identity.internal.repository.ApplicationSignatureEntity;
import io.mifos.identity.internal.repository.SignatureEntity;

/**
 * @author Myrle Krantz
 */
public interface SignatureMapper {
  static ApplicationSignatureSet mapToApplicationSignatureSet(final SignatureEntity signatureEntity) {
    return new ApplicationSignatureSet(
            signatureEntity.getKeyTimestamp(),
            new Signature(signatureEntity.getPublicKeyMod(), signatureEntity.getPublicKeyExp()),
            new Signature(signatureEntity.getPublicKeyMod(), signatureEntity.getPublicKeyExp()));
  }

  static Signature mapToSignature(final ApplicationSignatureEntity entity) {
    final Signature ret = new Signature();
    ret.setPublicKeyExp(entity.getPublicKeyExp());
    ret.setPublicKeyMod(entity.getPublicKeyMod());
    return ret;
  }
}
