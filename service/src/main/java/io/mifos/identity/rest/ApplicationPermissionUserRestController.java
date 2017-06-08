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
package io.mifos.identity.rest;

import io.mifos.anubis.annotation.AcceptedTokenType;
import io.mifos.anubis.annotation.Permittable;
import io.mifos.core.command.gateway.CommandGateway;
import io.mifos.core.lang.ServiceException;
import io.mifos.identity.api.v1.PermittableGroupIds;
import io.mifos.identity.internal.command.SetApplicationPermissionUserEnabledCommand;
import io.mifos.identity.internal.service.ApplicationService;
import io.mifos.identity.internal.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nonnull;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@RestController
@RequestMapping("/applications/{applicationidentifier}/permissions/{permissionidentifier}/users/{useridentifier}")
public class ApplicationPermissionUserRestController {
  private final ApplicationService service;
  private final UserService userService;
  private final CommandGateway commandGateway;

  @Autowired
  public ApplicationPermissionUserRestController(
          final ApplicationService service,
          final UserService userService,
          final CommandGateway commandGateway) {
    this.service = service;
    this.userService = userService;
    this.commandGateway = commandGateway;
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.SELF_MANAGEMENT)
  @RequestMapping(value = "/enabled", method = RequestMethod.PUT,
          consumes = {MediaType.ALL_VALUE},
          produces = {MediaType.APPLICATION_JSON_VALUE})
  public @ResponseBody
  ResponseEntity<Void>
  setApplicationPermissionEnabledForUser(@PathVariable("applicationidentifier") String applicationIdentifier,
                                         @PathVariable("permissionidentifier") String permittableEndpointGroupIdentifier,
                                         @PathVariable("useridentifier") String userIdentifier,
                                         @RequestBody Boolean enabled)
  {
    ApplicationRestController.checkApplicationPermissionIdentifier(service, applicationIdentifier, permittableEndpointGroupIdentifier);
    checkUserIdentifier(userIdentifier);
    commandGateway.process(new SetApplicationPermissionUserEnabledCommand(applicationIdentifier, permittableEndpointGroupIdentifier, userIdentifier, enabled));
    return ResponseEntity.accepted().build();
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.SELF_MANAGEMENT)
  @RequestMapping(value = "/enabled", method = RequestMethod.GET,
          consumes = {MediaType.APPLICATION_JSON_VALUE},
          produces = {MediaType.APPLICATION_JSON_VALUE})
  public @ResponseBody
  ResponseEntity<Boolean>
  getApplicationPermissionEnabledForUser(@PathVariable("applicationidentifier") String applicationIdentifier,
                                         @PathVariable("permissionidentifier") String permittableEndpointGroupIdentifier,
                                         @PathVariable("useridentifier") String userIdentifier) {
    ApplicationRestController.checkApplicationPermissionIdentifier(service, applicationIdentifier, permittableEndpointGroupIdentifier);
    checkUserIdentifier(userIdentifier);
    return ResponseEntity.ok(service.applicationPermissionEnabledForUser(applicationIdentifier, permittableEndpointGroupIdentifier, userIdentifier));
  }

  private void checkUserIdentifier(final @Nonnull String identifier) {
    userService.findByIdentifier(identifier).orElseThrow(
            () -> ServiceException.notFound("User ''" + identifier + "'' doesn''t exist."));
  }
}
