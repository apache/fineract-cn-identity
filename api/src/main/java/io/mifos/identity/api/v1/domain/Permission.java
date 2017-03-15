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

import io.mifos.anubis.api.v1.domain.AllowedOperation;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Objects;
import java.util.Set;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Permission {
  @NotBlank
  private String permittableEndpointGroupIdentifier;

  @NotNull
  @Valid
  private Set<AllowedOperation> allowedOperations;

  public Permission() {
  }

  public Permission(String permittableEndpointGroupIdentifier, Set<AllowedOperation> allowedOperations) {
    this.permittableEndpointGroupIdentifier = permittableEndpointGroupIdentifier;
    this.allowedOperations = allowedOperations;
  }

  public String getPermittableEndpointGroupIdentifier() {
    return permittableEndpointGroupIdentifier;
  }

  public void setPermittableEndpointGroupIdentifier(String permittableEndpointGroupIdentifier) {
    this.permittableEndpointGroupIdentifier = permittableEndpointGroupIdentifier;
  }

  public Set<AllowedOperation> getAllowedOperations() {
    return allowedOperations;
  }

  public void setAllowedOperations(Set<AllowedOperation> allowedOperations) {
    this.allowedOperations = allowedOperations;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Permission that = (Permission) o;
    return Objects.equals(permittableEndpointGroupIdentifier, that.permittableEndpointGroupIdentifier) &&
            Objects.equals(allowedOperations, that.allowedOperations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(permittableEndpointGroupIdentifier, allowedOperations);
  }

  @Override
  public String toString() {
    return "Permission{" +
            "permittableEndpointGroupIdentifier='" + permittableEndpointGroupIdentifier + '\'' +
            ", allowedOperations=" + allowedOperations +
            '}';
  }
}
