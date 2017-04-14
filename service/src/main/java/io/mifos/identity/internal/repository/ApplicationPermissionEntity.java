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

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

/**
 * @author Myrle Krantz
 */
@Table(name = ApplicationPermissions.TABLE_NAME)
public class ApplicationPermissionEntity {
  @PartitionKey
  @Column(name = ApplicationPermissions.APPLICATION_IDENTIFIER_COLUMN)
  private String applicationIdentifier;

  @ClusteringColumn
  @Column(name = ApplicationPermissions.PERMITTABLE_GROUP_IDENTIFIER_COLUMN)
  private String permittableGroupIdentifier;

  @Column(name = ApplicationPermissions.PERMISSION_COLUMN)
  private PermissionType permission;

  public ApplicationPermissionEntity() {
  }

  public ApplicationPermissionEntity(final String applicationIdentifier, final PermissionType permission) {
    this.applicationIdentifier = applicationIdentifier;
    this.permittableGroupIdentifier = permission.getPermittableGroupIdentifier();
    this.permission = permission;
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

  public PermissionType getPermission() {
    return permission;
  }

  public void setPermission(PermissionType permission) {
    this.permission = permission;
  }
}
