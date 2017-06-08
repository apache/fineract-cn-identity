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

import io.mifos.identity.api.v1.domain.CallEndpointSet;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
public class CreateApplicationCallEndpointSetCommand {
  private String applicationIdentifier;
  private CallEndpointSet callEndpointSet;

  public CreateApplicationCallEndpointSetCommand() {
  }

  public CreateApplicationCallEndpointSetCommand(String applicationIdentifier, CallEndpointSet callEndpointSet) {
    this.applicationIdentifier = applicationIdentifier;
    this.callEndpointSet = callEndpointSet;
  }

  public String getApplicationIdentifier() {
    return applicationIdentifier;
  }

  public void setApplicationIdentifier(String applicationIdentifier) {
    this.applicationIdentifier = applicationIdentifier;
  }

  public CallEndpointSet getCallEndpointSet() {
    return callEndpointSet;
  }

  public void setEndpointSet(CallEndpointSet callEndpointSet) {
    this.callEndpointSet = callEndpointSet;
  }

  @Override
  public String toString() {
    return "CreateApplicationCallEndpointSetCommand{" +
            "applicationIdentifier='" + applicationIdentifier + '\'' +
            ", callEndpointSet=" + callEndpointSet.getIdentifier() +
            '}';
  }
}