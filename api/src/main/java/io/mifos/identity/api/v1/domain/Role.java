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
import io.mifos.identity.api.v1.validation.ChangeableRole;
import org.hibernate.validator.constraints.ScriptAssert;
import org.springframework.util.Assert;

import javax.annotation.Nonnull;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
public class Role {
  @ValidIdentifier
  @ChangeableRole
  private String identifier;

  @NotNull
  @Valid
  private List<Permission> permissions;



  public Role() {}

  public Role(
      final @Nonnull String identifier,
      final @Nonnull List<Permission> permissions) {
    Assert.notNull(identifier);
    Assert.notNull(permissions);

    this.identifier = identifier;
    this.permissions = permissions;
  }

  public void setIdentifier(String identifier) { this.identifier = identifier;}

  public String getIdentifier() {
    return identifier;
  }

  public void setPermissions(List<Permission> permissions) {this.permissions = permissions;}

  public List<Permission> getPermissions() {
    return permissions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Role role = (Role) o;
    return Objects.equals(identifier, role.identifier) &&
            Objects.equals(permissions, role.permissions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifier, permissions);
  }

  @Override
  public String toString() {
    return "Role{" +
            "identifier='" + identifier + '\'' +
            ", permissions=" + permissions +
            '}';
  }
}