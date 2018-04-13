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
package org.apache.fineract.cn.identity.rest;

import org.apache.fineract.cn.identity.api.v1.PermittableGroupIds;
import javax.annotation.Nonnull;
import org.apache.fineract.cn.anubis.annotation.AcceptedTokenType;
import org.apache.fineract.cn.anubis.annotation.Permittable;
import org.apache.fineract.cn.command.gateway.CommandGateway;
import org.apache.fineract.cn.identity.internal.command.SetApplicationPermissionUserEnabledCommand;
import org.apache.fineract.cn.identity.internal.service.ApplicationService;
import org.apache.fineract.cn.identity.internal.service.UserService;
import org.apache.fineract.cn.lang.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

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
