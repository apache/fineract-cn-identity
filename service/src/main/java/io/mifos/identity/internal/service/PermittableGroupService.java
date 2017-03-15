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

import io.mifos.anubis.api.v1.domain.PermittableEndpoint;
import io.mifos.identity.api.v1.domain.PermittableGroup;
import io.mifos.identity.internal.repository.PermittableType;
import io.mifos.identity.internal.repository.PermittableGroupEntity;
import io.mifos.identity.internal.repository.PermittableGroups;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
@Service
public class PermittableGroupService {
  private final PermittableGroups repository;

  public PermittableGroupService(final PermittableGroups repository) {
    this.repository = repository;
  }

  public Optional<PermittableGroup> findByIdentifier(final String identifier) {
    final Optional<PermittableGroupEntity> ret = repository.get(identifier);

    return ret.map(this::mapPermittableGroup);
  }

  public List<PermittableGroup> findAll() {
    return repository.getAll().stream()
            .map(this::mapPermittableGroup)
            .collect(Collectors.toList());
  }

  private PermittableGroup mapPermittableGroup(final PermittableGroupEntity permittableGroupEntity) {
    final PermittableGroup ret = new PermittableGroup();
    ret.setIdentifier(permittableGroupEntity.getIdentifier());
    ret.setPermittables(permittableGroupEntity.getPermittables().stream()
            .map(this::mapPermittable)
            .collect(Collectors.toList()));
    return ret;
  }

  private PermittableEndpoint mapPermittable(final PermittableType entity) {
    final PermittableEndpoint ret = new PermittableEndpoint();
    ret.setMethod(entity.getMethod());
    ret.setGroupId(entity.getSourceGroupId());
    ret.setPath(entity.getPath());
    return ret;
  }
}
