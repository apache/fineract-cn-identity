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
package org.apache.fineract.cn.identity.internal.command.handler;

import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.apache.fineract.cn.command.annotation.Aggregate;
import org.apache.fineract.cn.command.annotation.CommandHandler;
import org.apache.fineract.cn.command.annotation.CommandLogLevel;
import org.apache.fineract.cn.command.annotation.EventEmitter;
import org.apache.fineract.cn.identity.api.v1.domain.Role;
import org.apache.fineract.cn.identity.api.v1.events.EventConstants;
import org.apache.fineract.cn.identity.internal.command.ChangeRoleCommand;
import org.apache.fineract.cn.identity.internal.command.CreateRoleCommand;
import org.apache.fineract.cn.identity.internal.command.DeleteRoleCommand;
import org.apache.fineract.cn.identity.internal.mapper.PermissionMapper;
import org.apache.fineract.cn.identity.internal.repository.RoleEntity;
import org.apache.fineract.cn.identity.internal.repository.Roles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@Aggregate
@Component
public class RoleCommandHandler {

  private final Roles roles;

  @Autowired
  public RoleCommandHandler(final Roles roles)
  {
    this.roles = roles;
  }

  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.OPERATION_HEADER, selectorValue = EventConstants.OPERATION_PUT_ROLE)
  public String process(final ChangeRoleCommand command) {
    final Optional<RoleEntity> instance = roles.get(command.getIdentifier());
    Assert.isTrue(instance.isPresent());

    instance.ifPresent(x -> roles.change(mapRole(command.getInstance())));

    return command.getInstance().getIdentifier();
  }

  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.OPERATION_HEADER, selectorValue = EventConstants.OPERATION_POST_ROLE)
  public String process(final CreateRoleCommand command) {
    Assert.isTrue(!roles.get(command.getInstance().getIdentifier()).isPresent());

    roles.add(mapRole(command.getInstance()));

    return command.getInstance().getIdentifier();
  }

  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.OPERATION_HEADER, selectorValue = EventConstants.OPERATION_DELETE_ROLE)
  public String process(final DeleteRoleCommand command) {
    final Optional<RoleEntity> instance = roles.get(command.getIdentifier());
    Assert.isTrue(instance.isPresent());

    instance.ifPresent(roles::delete);

    return command.getIdentifier();
  }

  private @Nonnull RoleEntity mapRole(
      @Nonnull final Role role) {
    final RoleEntity ret = new RoleEntity();
    ret.setIdentifier(role.getIdentifier());
    ret.setPermissions(role.getPermissions().stream()
            .map(PermissionMapper::mapToPermissionType)
            .collect(Collectors.toList()));

    return ret;
  }
}