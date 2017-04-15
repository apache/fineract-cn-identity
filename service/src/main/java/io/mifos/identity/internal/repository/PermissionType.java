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


import com.datastax.driver.mapping.annotations.Field;
import com.datastax.driver.mapping.annotations.UDT;

import java.util.Objects;
import java.util.Set;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"unused", "WeakerAccess"})
@UDT(name = Permissions.TYPE_NAME)
public class PermissionType {

  @Field(name = Permissions.PERMITTABLE_GROUP_IDENTIFIER_FIELD)
  private String permittableGroupIdentifier;

  @Field(name = Permissions.ALLOWED_OPERATIONS_FIELD)
  private Set<AllowedOperationType> allowedOperations;

  public PermissionType() {
  }

  public PermissionType(String permittableGroupIdentifier, Set<AllowedOperationType> allowedOperations) {
    this.permittableGroupIdentifier = permittableGroupIdentifier;
    this.allowedOperations = allowedOperations;
  }

  public String getPermittableGroupIdentifier() {
    return permittableGroupIdentifier;
  }

  public void setPermittableGroupIdentifier(String permittableGroupIdentifier) {
    this.permittableGroupIdentifier = permittableGroupIdentifier;
  }

  public Set<AllowedOperationType> getAllowedOperations() {
    return allowedOperations;
  }

  public void setAllowedOperations(Set<AllowedOperationType> allowedOperations) {
    this.allowedOperations = allowedOperations;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PermissionType that = (PermissionType) o;
    return Objects.equals(permittableGroupIdentifier, that.permittableGroupIdentifier) &&
            Objects.equals(allowedOperations, that.allowedOperations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(permittableGroupIdentifier, allowedOperations);
  }

  @Override
  public String toString() {
    return "PermissionType{" +
            "permittableGroupIdentifier='" + permittableGroupIdentifier + '\'' +
            ", allowedOperations=" + allowedOperations +
            '}';
  }
}
