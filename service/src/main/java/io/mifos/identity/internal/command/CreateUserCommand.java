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

import io.mifos.identity.api.v1.domain.UserWithPassword;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
public class CreateUserCommand {
  private String identifier;
  private String role;

  //transient to ensure this field doesn't land in the audit log.
  private transient String password;

  public CreateUserCommand() {
  }

  public CreateUserCommand(final UserWithPassword instance) {
    this.identifier = instance.getIdentifier();
    this.role = instance.getRole();
    this.password = instance.getPassword();
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

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  @Override
  public String toString() {
    return "CreateUserCommand{" +
            "identifier='" + identifier + '\'' +
            ", role='" + role + '\'' +
            '}';
  }
}
