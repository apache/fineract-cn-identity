/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.cn.identity.internal.command;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
public class SetApplicationPermissionUserEnabledCommand {
  private String applicationIdentifier;
  private String permittableGroupIdentifier;
  private String userIdentifier;
  private boolean enabled;

  public SetApplicationPermissionUserEnabledCommand() {
  }

  public SetApplicationPermissionUserEnabledCommand(String applicationIdentifier, String permittableGroupIdentifier, String userIdentifier, boolean enabled) {
    this.applicationIdentifier = applicationIdentifier;
    this.permittableGroupIdentifier = permittableGroupIdentifier;
    this.userIdentifier = userIdentifier;
    this.enabled = enabled;
  }

  public String getApplicationIdentifier() {
    return applicationIdentifier;
  }

  public void setApplicationIdentifier(String applicationIdentifier) {
    this.applicationIdentifier = applicationIdentifier;
  }

  public String getPermittableGroupIdentifier() {
    return permittableGroupIdentifier;
  }

  public void setPermittableGroupIdentifier(String permittableGroupIdentifier) {
    this.permittableGroupIdentifier = permittableGroupIdentifier;
  }

  public String getUserIdentifier() {
    return userIdentifier;
  }

  public void setUserIdentifier(String userIdentifier) {
    this.userIdentifier = userIdentifier;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public String toString() {
    return "SetApplicationPermissionUserEnabledCommand{" +
            "applicationIdentifier='" + applicationIdentifier + '\'' +
            ", permittableGroupIdentifier='" + permittableGroupIdentifier + '\'' +
            ", userIdentifier='" + userIdentifier + '\'' +
            ", enabled=" + enabled +
            '}';
  }
}
