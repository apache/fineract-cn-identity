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
import io.mifos.core.command.annotation.EventEmitter;
import io.mifos.identity.api.v1.events.ApplicationPermissionEvent;
import io.mifos.identity.api.v1.events.ApplicationSignatureEvent;
import io.mifos.identity.api.v1.events.EventConstants;
import io.mifos.identity.internal.command.CreateApplicationPermissionCommand;
import io.mifos.identity.internal.command.DeleteApplicationCommand;
import io.mifos.identity.internal.command.DeleteApplicationPermissionCommand;
import io.mifos.identity.internal.command.SetApplicationSignatureCommand;
import io.mifos.identity.internal.mapper.PermissionMapper;
import io.mifos.identity.internal.repository.ApplicationPermissionEntity;
import io.mifos.identity.internal.repository.ApplicationPermissions;
import io.mifos.identity.internal.repository.ApplicationSignatureEntity;
import io.mifos.identity.internal.repository.ApplicationSignatures;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Myrle Krantz
 */
@Aggregate
@Component
public class ApplicationCommandHandler {
  private final ApplicationSignatures applicationSignatures;
  private final ApplicationPermissions applicationPermissions;

  @Autowired
  public ApplicationCommandHandler(final ApplicationSignatures applicationSignatures,
                                   final ApplicationPermissions applicationPermissions) {
    this.applicationSignatures = applicationSignatures;
    this.applicationPermissions = applicationPermissions;
  }

  @CommandHandler
  @EventEmitter(selectorName = EventConstants.OPERATION_HEADER, selectorValue = EventConstants.OPERATION_PUT_APPLICATION_SIGNATURE)
  public ApplicationSignatureEvent process(final SetApplicationSignatureCommand command) {
    final ApplicationSignatureEntity applicationSignatureEntity = new ApplicationSignatureEntity();
    applicationSignatureEntity.setApplicationIdentifier(command.getApplicationIdentifier());
    applicationSignatureEntity.setKeyTimestamp(command.getKeyTimestamp());
    applicationSignatureEntity.setPublicKeyMod(command.getSignature().getPublicKeyMod());
    applicationSignatureEntity.setPublicKeyExp(command.getSignature().getPublicKeyExp());
    applicationSignatures.add(applicationSignatureEntity);

    return new ApplicationSignatureEvent(command.getApplicationIdentifier(), command.getKeyTimestamp());
  }

  @CommandHandler
  @EventEmitter(selectorName = EventConstants.OPERATION_HEADER, selectorValue = EventConstants.OPERATION_DELETE_APPLICATION)
  public String process(final DeleteApplicationCommand command) {
    applicationSignatures.delete(command.getApplicationIdentifier());
    return command.getApplicationIdentifier();
  }

  @CommandHandler
  @EventEmitter(selectorName = EventConstants.OPERATION_HEADER, selectorValue = EventConstants.OPERATION_POST_APPLICATION_PERMISSION)
  public ApplicationPermissionEvent process(final CreateApplicationPermissionCommand command) {
    final ApplicationPermissionEntity applicationPermissionEntity = new ApplicationPermissionEntity(
            command.getApplicationIdentifer(), PermissionMapper.mapToPermissionType(command.getPermission()));

    applicationPermissions.add(applicationPermissionEntity);
    return new ApplicationPermissionEvent(command.getApplicationIdentifer(), command.getPermission().getPermittableEndpointGroupIdentifier());
  }

  @CommandHandler
  @EventEmitter(selectorName = EventConstants.OPERATION_HEADER, selectorValue = EventConstants.OPERATION_DELETE_APPLICATION_PERMISSION)
  public ApplicationPermissionEvent process(final DeleteApplicationPermissionCommand command) {
    applicationPermissions.delete(command.getApplicationIdentifier(), command.getPermittableGroupIdentifier());
    return new ApplicationPermissionEvent(command.getApplicationIdentifier(), command.getPermittableGroupIdentifier());
  }
}