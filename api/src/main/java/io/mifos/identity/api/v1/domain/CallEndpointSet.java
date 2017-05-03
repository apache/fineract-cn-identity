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
package io.mifos.identity.api.v1.domain;

import io.mifos.core.lang.validation.constraints.ValidIdentifier;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class CallEndpointSet {
  @ValidIdentifier
  private String identifier;

  @NotNull
  private List<String> permittableEndpointGroupIdentifiers;

  public CallEndpointSet() {
  }

  public CallEndpointSet(String identifier, List<String> permittableEndpointGroupIdentifiers) {
    this.identifier = identifier;
    this.permittableEndpointGroupIdentifiers = permittableEndpointGroupIdentifiers;
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public List<String> getPermittableEndpointGroupIdentifiers() {
    return permittableEndpointGroupIdentifiers;
  }

  public void setPermittableEndpointGroupIdentifiers(List<String> permittableEndpointGroupIdentifiers) {
    this.permittableEndpointGroupIdentifiers = permittableEndpointGroupIdentifiers;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CallEndpointSet that = (CallEndpointSet) o;
    return Objects.equals(identifier, that.identifier) &&
            Objects.equals(permittableEndpointGroupIdentifiers, that.permittableEndpointGroupIdentifiers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifier, permittableEndpointGroupIdentifiers);
  }

  @Override
  public String toString() {
    return "CallEndpointSet{" +
            "identifier='" + identifier + '\'' +
            ", permittableEndpointGroupIdentifiers=" + permittableEndpointGroupIdentifiers +
            '}';
  }
}
