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

import org.apache.fineract.cn.identity.api.v1.PermittableGroupIds;
import org.apache.fineract.cn.identity.api.v1.domain.Permission;
import org.apache.fineract.cn.identity.api.v1.domain.User;
import org.apache.fineract.cn.anubis.api.v1.domain.AllowedOperation;
import org.apache.fineract.cn.identity.internal.repository.PermissionType;
import org.apache.fineract.cn.identity.internal.repository.RoleEntity;
import org.apache.fineract.cn.identity.internal.repository.Roles;
import org.apache.fineract.cn.identity.internal.repository.UserEntity;
import org.apache.fineract.cn.identity.internal.repository.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
@Service
public class UserService {

  private final Users users;
  private final Roles roles;

  @Autowired
  UserService(final Users users, final Roles roles)
  {
    this.users = users;
    this.roles = roles;
  }

  public List<User> findAll() {
    return users.getAll().stream().map(UserService::mapUser).collect(Collectors.toList());
  }

  public Optional<User> findByIdentifier(final String identifier)
  {
    return users.get(identifier).map(UserService::mapUser);
  }

  static private User mapUser(final UserEntity u) {
    return new User(u.getIdentifier(), u.getRole());
  }

  public Set<Permission> getPermissions(final String userIdentifier) {
    final Optional<UserEntity> userEntity = users.get(userIdentifier);
    final Optional<RoleEntity> roleEntity = userEntity.map(UserEntity::getRole).map(roles::get).orElse(Optional.empty());
    final List<PermissionType> permissionEntities = roleEntity.map(RoleEntity::getPermissions).orElse(Collections.emptyList());
    final List<Permission> permissions = RoleMapper.mapPermissions(permissionEntities);
    permissions.add(new Permission(PermittableGroupIds.SELF_MANAGEMENT, AllowedOperation.ALL));

    return new HashSet<>(permissions);
  }
}