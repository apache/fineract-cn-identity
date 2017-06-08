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

import io.mifos.core.command.annotation.Aggregate;
import io.mifos.core.command.annotation.CommandHandler;
import io.mifos.core.command.annotation.CommandLogLevel;
import io.mifos.core.command.annotation.EventEmitter;
import io.mifos.core.lang.ServiceException;
import io.mifos.identity.api.v1.events.EventConstants;
import io.mifos.identity.internal.command.ChangeUserPasswordCommand;
import io.mifos.identity.internal.command.ChangeUserRoleCommand;
import io.mifos.identity.internal.command.CreateUserCommand;
import io.mifos.identity.internal.repository.UserEntity;
import io.mifos.identity.internal.repository.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@Aggregate
@Component
public class UserCommandHandler {

  private final Users usersRepository;
  private final UserEntityCreator userEntityCreator;

  @Autowired
  UserCommandHandler(
          final Users usersRepository,
          final UserEntityCreator userEntityCreator)
  {
    this.usersRepository = usersRepository;
    this.userEntityCreator = userEntityCreator;
  }

  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.OPERATION_HEADER, selectorValue = EventConstants.OPERATION_PUT_USER_ROLEIDENTIFIER)
  public String process(final ChangeUserRoleCommand command) {
    final UserEntity user = usersRepository.get(command.getIdentifier())
        .orElseThrow(() -> ServiceException.notFound(
            "User " + command.getIdentifier() + " doesn't exist."));

    user.setRole(command.getRole());
    usersRepository.add(user);

    return user.getIdentifier();
  }

  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.OPERATION_HEADER, selectorValue = EventConstants.OPERATION_PUT_USER_PASSWORD)
  public String process(final ChangeUserPasswordCommand command) {
    final UserEntity user = usersRepository.get(command.getIdentifier())
        .orElseThrow(() -> ServiceException.notFound(
            "User " + command.getIdentifier() + " doesn't exist."));

    final UserEntity userWithNewPassword = userEntityCreator.build(
            user.getIdentifier(), user.getRole(), command.getPassword(),
            !SecurityContextHolder.getContext().getAuthentication().getName().equals(command.getIdentifier()));
    usersRepository.add(userWithNewPassword);

    return user.getIdentifier();
  }

  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.OPERATION_HEADER, selectorValue = EventConstants.OPERATION_POST_USER)
  public String process(final CreateUserCommand command) {
    Assert.hasText(command.getPassword());

    final UserEntity userEntity = userEntityCreator.build(
        command.getIdentifier(), command.getRole(), command.getPassword(), true);

    usersRepository.add(userEntity);

    return command.getIdentifier();
  }
}
