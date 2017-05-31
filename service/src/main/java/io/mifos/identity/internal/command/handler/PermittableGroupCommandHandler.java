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

import io.mifos.anubis.api.v1.domain.PermittableEndpoint;
import io.mifos.core.command.annotation.Aggregate;
import io.mifos.core.command.annotation.CommandHandler;
import io.mifos.core.command.annotation.CommandLogLevel;
import io.mifos.core.command.annotation.EventEmitter;
import io.mifos.identity.api.v1.domain.PermittableGroup;
import io.mifos.identity.api.v1.events.EventConstants;
import io.mifos.identity.internal.command.CreatePermittableGroupCommand;
import io.mifos.identity.internal.repository.PermittableGroupEntity;
import io.mifos.identity.internal.repository.PermittableGroups;
import io.mifos.identity.internal.repository.PermittableType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@Aggregate
@Component
public class PermittableGroupCommandHandler {

  private final PermittableGroups repository;

  @Autowired
  public PermittableGroupCommandHandler(final PermittableGroups repository)
  {
    this.repository = repository;
  }

  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.OPERATION_HEADER, selectorValue = EventConstants.OPERATION_POST_PERMITTABLE_GROUP)
  public String process(final CreatePermittableGroupCommand command) {
    Assert.isTrue(!repository.get(command.getInstance().getIdentifier()).isPresent());

    repository.add(map(command.getInstance()));

    return command.getInstance().getIdentifier();
  }

  private PermittableGroupEntity map(final PermittableGroup instance) {
    final PermittableGroupEntity ret = new PermittableGroupEntity();
    ret.setIdentifier(instance.getIdentifier());
    ret.setPermittables(instance.getPermittables().stream().map(this::map).collect(Collectors.toList()));
    return ret;
  }

  private PermittableType map(final PermittableEndpoint instance) {
    final PermittableType ret = new PermittableType();
    ret.setMethod(instance.getMethod());
    ret.setSourceGroupId(instance.getGroupId());
    ret.setPath(instance.getPath());
    return ret;
  }
}
