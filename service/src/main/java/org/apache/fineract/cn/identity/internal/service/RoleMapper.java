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
package org.apache.fineract.cn.identity.internal.service;

import org.apache.fineract.cn.identity.api.v1.domain.Permission;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.fineract.cn.anubis.api.v1.domain.AllowedOperation;
import org.apache.fineract.cn.identity.internal.repository.AllowedOperationType;
import org.apache.fineract.cn.identity.internal.repository.PermissionType;

/**
 * @author Myrle Krantz
 */
public class RoleMapper {
  @SuppressWarnings("WeakerAccess")
  static Set<AllowedOperation> mapAllowedOperations(
          final Set<AllowedOperationType> allowedOperations) {
    return allowedOperations.stream()
        .map(RoleMapper::mapAllowedOperation)
        .collect(Collectors.toSet());
  }

  public static AllowedOperation mapAllowedOperation(final AllowedOperationType allowedOperationType) {
    return AllowedOperation.valueOf(allowedOperationType.toString());
  }

  static List<Permission> mapPermissions(List<PermissionType> permissions) {
    return permissions.stream()
            .map(i -> new Permission(
                    i.getPermittableGroupIdentifier(),
                    RoleMapper.mapAllowedOperations(i.getAllowedOperations()))).collect(Collectors.toList());
  }
}
