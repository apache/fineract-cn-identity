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
package io.mifos.identity.internal.command.handler;

import com.datastax.driver.core.exceptions.InvalidQueryException;
import io.mifos.anubis.api.v1.domain.Signature;
import io.mifos.core.lang.ServiceException;
import io.mifos.core.lang.security.RsaKeyPairFactory;
import io.mifos.identity.api.v1.PermittableGroupIds;
import io.mifos.identity.internal.repository.*;
import io.mifos.identity.internal.util.IdentityConstants;
import io.mifos.tool.crypto.SaltGenerator;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Myrle Krantz
 */
@Component
public class Provisioner {
  private final Tenants tenant;
  private final Users users;
  private final PermittableGroups permittableGroups;
  private final Roles roles;
  private final UserEntityCreator userEntityCreator;
  private final Logger logger;
  private final SaltGenerator saltGenerator;

  @Value("${spring.application.name}")
  private String applicationName;

  @Value("${identity.passwordExpiresInDays:93}")
  private int passwordExpiresInDays;

  @Value("${identity.timeToChangePasswordAfterExpirationInDays:4}")
  private int timeToChangePasswordAfterExpirationInDays;

  @Autowired
  Provisioner(
          final Tenants tenant,
          final Users users,
          final Roles roles,
          final PermittableGroups permittableGroups,
          final UserEntityCreator userEntityCreator,
          @Qualifier(IdentityConstants.LOGGER_NAME) final Logger logger,
          final SaltGenerator saltGenerator)
  {
    this.tenant = tenant;
    this.users = users;
    this.permittableGroups = permittableGroups;
    this.roles = roles;
    this.userEntityCreator = userEntityCreator;
    this.logger = logger;
    this.saltGenerator = saltGenerator;
  }

  public Signature provisionTenant(final String initialPasswordHash) {
    final RsaKeyPairFactory.KeyPairHolder keys = RsaKeyPairFactory.createKeyPair();

    byte[] fixedSalt = this.saltGenerator.createRandomSalt();

    try {
      tenant.buildTable();
      users.buildTable();
      permittableGroups.buildTable();
      roles.buildTable();

      tenant.add(fixedSalt, keys, passwordExpiresInDays, timeToChangePasswordAfterExpirationInDays);

      createPermittablesGroup(PermittableGroupIds.ROLE_MANAGEMENT, "/roles/*", "/permittablegroups/*");
      createPermittablesGroup(PermittableGroupIds.IDENTITY_MANAGEMENT, "/users/*");
      createPermittablesGroup(PermittableGroupIds.SELF_MANAGEMENT, "/users/{useridentifier}/password");

      final List<PermissionType> permissions = new ArrayList<>();
      permissions.add(fullAccess(PermittableGroupIds.ROLE_MANAGEMENT));
      permissions.add(fullAccess(PermittableGroupIds.IDENTITY_MANAGEMENT));

      final RoleEntity suRole = new RoleEntity();
      suRole.setIdentifier(IdentityConstants.SU_ROLE);
      suRole.setPermissions(permissions);

      roles.add(suRole);

      final UserEntity suUser = userEntityCreator
              .build(IdentityConstants.SU_NAME, IdentityConstants.SU_ROLE, initialPasswordHash, true,
                      fixedSalt, timeToChangePasswordAfterExpirationInDays);
      users.add(suUser);
    }
    catch (final InvalidQueryException e)
    {
      logger.error("Failed to provision cassandra tables for tenant.", e);
      throw ServiceException.internalError("Failed to provision tenant.");
    }

    return new Signature(keys.getPublicKeyMod(), keys.getPublicKeyExp());
  }

  private PermissionType fullAccess(final String permittableGroupIdentifier) {
    final PermissionType ret = new PermissionType();
    ret.setPermittableGroupIdentifier(permittableGroupIdentifier);
    ret.setAllowedOperations(AllowedOperationType.ALL);
    return ret;
  }

  private void createPermittablesGroup(final String identifier, final String... paths) {
    final PermittableGroupEntity permittableGroup = new PermittableGroupEntity();
    permittableGroup.setIdentifier(identifier);
    permittableGroup.setPermittables(Arrays.stream(paths).flatMap(this::permittables).collect(Collectors.toList()));
    permittableGroups.add(permittableGroup);
  }

  private Stream<PermittableType> permittables(final String path)
  {
    final PermittableType getret = new PermittableType();
    getret.setPath(applicationName + path);
    getret.setMethod("GET");

    final PermittableType postret = new PermittableType();
    postret.setPath(applicationName + path);
    postret.setMethod("POST");

    final PermittableType putret = new PermittableType();
    putret.setPath(applicationName + path);
    putret.setMethod("PUT");

    final PermittableType delret = new PermittableType();
    delret.setPath(applicationName + path);
    delret.setMethod("DELETE");

    final List<PermittableType> ret = new ArrayList<>();
    ret.add(getret);
    ret.add(postret);
    ret.add(putret);
    ret.add(delret);

    return ret.stream();
  }


}
