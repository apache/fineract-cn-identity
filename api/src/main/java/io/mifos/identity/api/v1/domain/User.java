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

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.ScriptAssert;

import java.util.Objects;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"WeakerAccess", "unused"})
@ScriptAssert(lang = "javascript", script = "_this.identifier !== \"guest\" && _this.identifier !== \"seshat\" && _this.identifier !== \"system\" && _this.identifier !== \"wepemnefret\"" )
public class User {
  @NotBlank
  @Length(min = 4, max = 32)
  private String identifier;

  @NotBlank
  private String role;

  public User() { }

  public User(final String identifier, final String role) {
    this.identifier = identifier;
    this.role = role;
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  @Override public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof User))
      return false;
    User user = (User) o;
    return Objects.equals(identifier, user.identifier) && Objects.equals(role, user.role);
  }

  @Override public int hashCode() {
    return Objects.hash(identifier, role);
  }

  @Override public String toString() {
    return "User{" +
        "identifier='" + identifier + '\'' +
        ", role='" + role + '\'' +
        '}';
  }
}
