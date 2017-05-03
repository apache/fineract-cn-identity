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
package io.mifos.identity.internal.service;

import io.mifos.identity.api.v1.domain.Role;
import io.mifos.identity.internal.repository.RoleEntity;
import io.mifos.identity.internal.repository.Roles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
@Service
public class RoleService {

  private final Roles repository;

  @Autowired
  public RoleService(final Roles repository) {
    this.repository = repository;
  }

  public List<Role> findAll() {
    return repository.getAll().stream()
        .map(this::mapEntity)
        .sorted((x, y) -> (x.getIdentifier().compareTo(y.getIdentifier())))
        .collect(Collectors.toList());
  }

  private Role mapEntity(final RoleEntity roleEntity) {
    final Role ret = new Role();
    ret.setIdentifier(roleEntity.getIdentifier());
    ret.setPermissions(RoleMapper.mapPermissions(roleEntity.getPermissions()));
    return ret;
  }

  public Optional<Role> findByIdentifier(final String identifier)
  {
    final Optional<RoleEntity> ret = repository.get(identifier);

    return ret.map(this::mapEntity);
  }
}
