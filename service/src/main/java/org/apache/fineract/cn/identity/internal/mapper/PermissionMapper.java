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
package org.apache.fineract.cn.identity.internal.mapper;

import org.apache.fineract.cn.identity.api.v1.domain.Permission;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.fineract.cn.anubis.api.v1.domain.AllowedOperation;
import org.apache.fineract.cn.identity.internal.repository.AllowedOperationType;
import org.apache.fineract.cn.identity.internal.repository.PermissionType;

/**
 * @author Myrle Krantz
 */
public interface PermissionMapper {

  static Set<AllowedOperationType> mapToAllowedOperationsTypeSet(
          @Nullable final Set<AllowedOperation> allowedOperations) {
    if (allowedOperations == null)
      return Collections.emptySet();

    return allowedOperations.stream().map(op -> {
      switch (op) {
        case READ:
          return AllowedOperationType.READ;
        case CHANGE:
          return AllowedOperationType.CHANGE;
        case DELETE:
          return AllowedOperationType.DELETE;
        default:
          return null;
      }
    }).filter(op -> (op != null)).collect(Collectors.toSet());
  }

  static Set<AllowedOperation> mapToAllowedOperations(final Set<AllowedOperationType> allowedOperations) {
    return allowedOperations.stream().map(op -> {
      switch (op) {
        case READ:
          return AllowedOperation.READ;
        case CHANGE:
          return AllowedOperation.CHANGE;
        case DELETE:
          return AllowedOperation.DELETE;
        default:
          return null;
      }
    }).filter(op -> (op != null)).collect(Collectors.toSet());
  }

  static PermissionType mapToPermissionType(final Permission instance) {
    return new PermissionType(instance.getPermittableEndpointGroupIdentifier(), mapToAllowedOperationsTypeSet(instance.getAllowedOperations()));
  }

  static Permission mapToPermission(final PermissionType instance) {
    return new Permission(instance.getPermittableGroupIdentifier(), mapToAllowedOperations(instance.getAllowedOperations()));
  }
}
