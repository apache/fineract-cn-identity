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
package io.mifos.identity.api.v1.events;

import java.util.Objects;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class ApplicationCallEndpointSetEvent {
  private String applicationIdentifier;
  private String callEndpointSetIdentifier;

  public ApplicationCallEndpointSetEvent() {
  }

  public ApplicationCallEndpointSetEvent(String applicationIdentifier, String callEndpointSetIdentifier) {
    this.applicationIdentifier = applicationIdentifier;
    this.callEndpointSetIdentifier = callEndpointSetIdentifier;
  }

  public String getApplicationIdentifier() {
    return applicationIdentifier;
  }

  public void setApplicationIdentifier(String applicationIdentifier) {
    this.applicationIdentifier = applicationIdentifier;
  }

  public String getCallEndpointSetIdentifier() {
    return callEndpointSetIdentifier;
  }

  public void setCallEndpointSetIdentifier(String callEndpointSetIdentifier) {
    this.callEndpointSetIdentifier = callEndpointSetIdentifier;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ApplicationCallEndpointSetEvent that = (ApplicationCallEndpointSetEvent) o;
    return Objects.equals(applicationIdentifier, that.applicationIdentifier) &&
            Objects.equals(callEndpointSetIdentifier, that.callEndpointSetIdentifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(applicationIdentifier, callEndpointSetIdentifier);
  }

  @Override
  public String toString() {
    return "ApplicationCallEndpointSetEvent{" +
            "applicationIdentifier='" + applicationIdentifier + '\'' +
            ", callEndpointSetIdentifier='" + callEndpointSetIdentifier + '\'' +
            '}';
  }
}
