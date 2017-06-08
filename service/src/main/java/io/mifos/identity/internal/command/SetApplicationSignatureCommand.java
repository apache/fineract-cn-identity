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
package io.mifos.identity.internal.command;

import io.mifos.anubis.api.v1.domain.Signature;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
public class SetApplicationSignatureCommand {
  private String applicationIdentifier;
  private String keyTimestamp;
  private Signature signature;

  public SetApplicationSignatureCommand() {
  }

  public SetApplicationSignatureCommand(String applicationIdentifier, String keyTimestamp, Signature signature) {
    this.applicationIdentifier = applicationIdentifier;
    this.keyTimestamp = keyTimestamp;
    this.signature = signature;
  }

  public String getApplicationIdentifier() {
    return applicationIdentifier;
  }

  public void setApplicationIdentifier(String applicationIdentifier) {
    this.applicationIdentifier = applicationIdentifier;
  }

  public String getKeyTimestamp() {
    return keyTimestamp;
  }

  public void setKeyTimestamp(String keyTimestamp) {
    this.keyTimestamp = keyTimestamp;
  }

  public Signature getSignature() {
    return signature;
  }

  public void setSignature(Signature signature) {
    this.signature = signature;
  }

  @Override
  public String toString() {
    return "SetApplicationSignatureCommand{" +
            "applicationIdentifier='" + applicationIdentifier + '\'' +
            ", keyTimestamp='" + keyTimestamp + '\'' +
            '}';
  }
}
