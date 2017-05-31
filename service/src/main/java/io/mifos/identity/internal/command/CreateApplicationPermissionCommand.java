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

import io.mifos.identity.api.v1.domain.Permission;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
public class CreateApplicationPermissionCommand {
  private String applicationIdentifer;
  private Permission permission;

  public CreateApplicationPermissionCommand() {
  }

  public CreateApplicationPermissionCommand(final String applicationIdentifier, final Permission permission) {
    this.applicationIdentifer = applicationIdentifier;
    this.permission = permission;
  }

  public String getApplicationIdentifer() {
    return applicationIdentifer;
  }

  public void setApplicationIdentifer(String applicationIdentifer) {
    this.applicationIdentifer = applicationIdentifer;
  }

  public Permission getPermission() {
    return permission;
  }

  public void setPermission(Permission permission) {
    this.permission = permission;
  }

  @Override
  public String toString() {
    return "CreateApplicationPermissionCommand{" +
            "applicationIdentifer='" + applicationIdentifer + '\'' +
            ", permission=" + permission.getPermittableEndpointGroupIdentifier() +
            '}';
  }
}
