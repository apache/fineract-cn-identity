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

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.Frozen;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.List;
import java.util.Objects;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"unused", "WeakerAccess"})
@Table(name = Roles.TABLE_NAME)
public class RoleEntity {
  @PartitionKey
  @Column(name = Roles.IDENTIFIER_COLUMN)
  private String identifier;

  @Frozen
  @Column(name = Roles.PERMISSIONS_COLUMN)
  private List<PermissionType> permissions;

  public RoleEntity() {
  }

  public RoleEntity(String identifier, List<PermissionType> permissions) {
    this.identifier = identifier;
    this.permissions = permissions;
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public List<PermissionType> getPermissions() {
    return permissions;
  }

  public void setPermissions(List<PermissionType> permissions) {
    this.permissions = permissions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RoleEntity that = (RoleEntity) o;
    return Objects.equals(identifier, that.identifier) &&
            Objects.equals(permissions, that.permissions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifier, permissions);
  }

  @Override
  public String toString() {
    return "RoleEntity{" +
            "identifier='" + identifier + '\'' +
            ", permissions=" + permissions +
            '}';
  }
}
