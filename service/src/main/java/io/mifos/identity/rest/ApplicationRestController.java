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
import io.mifos.anubis.api.v1.domain.Signature;
import io.mifos.anubis.api.v1.validation.ValidKeyTimestamp;
import io.mifos.core.command.gateway.CommandGateway;
import io.mifos.core.lang.ServiceException;
import io.mifos.identity.api.v1.PermittableGroupIds;
import io.mifos.identity.api.v1.domain.Permission;
import io.mifos.identity.internal.command.CreateApplicationPermissionCommand;
import io.mifos.identity.internal.command.DeleteApplicationCommand;
import io.mifos.identity.internal.command.DeleteApplicationPermissionCommand;
import io.mifos.identity.internal.command.SetApplicationSignatureCommand;
import io.mifos.identity.internal.service.ApplicationService;
import io.mifos.identity.internal.service.PermittableGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nonnull;
import javax.validation.Valid;
import java.util.List;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("WeakerAccess")
@RestController
@RequestMapping("/applications")
public class ApplicationRestController {
  private final ApplicationService service;
  private final PermittableGroupService permittableGroupService;
  private final CommandGateway commandGateway;

  @Autowired
  public ApplicationRestController(
          final ApplicationService service,
          final PermittableGroupService permittableGroupService,
          final CommandGateway commandGateway) {
    this.service = service;
    this.permittableGroupService = permittableGroupService;
    this.commandGateway = commandGateway;
  }

  @RequestMapping(method = RequestMethod.GET,
          consumes = {MediaType.ALL_VALUE},
          produces = {MediaType.APPLICATION_JSON_VALUE})
  @Permittable(value = AcceptedTokenType.SYSTEM)
  public @ResponseBody
  ResponseEntity<List<String>>
  getApplications() {
    return ResponseEntity.ok(service.getAllApplications());
  }

  @RequestMapping(value = "/{applicationidentifier}/signatures/{timestamp}", method = RequestMethod.PUT,
          consumes = {MediaType.ALL_VALUE},
          produces = {MediaType.APPLICATION_JSON_VALUE})
  @Permittable(value = AcceptedTokenType.SYSTEM)
  public @ResponseBody
  ResponseEntity<Void>
  setApplicationSignature(@PathVariable("applicationidentifier") @Nonnull String applicationIdentifier,
                          @PathVariable("timestamp") @ValidKeyTimestamp String timestamp,
                          @RequestBody @Valid Signature signature) {
    commandGateway.process(new SetApplicationSignatureCommand(applicationIdentifier, timestamp, signature));

    return ResponseEntity.accepted().build();
  }

  @RequestMapping(value = "/{applicationidentifier}/signatures/{timestamp}", method = RequestMethod.GET,
          consumes = {MediaType.ALL_VALUE},
          produces = {MediaType.APPLICATION_JSON_VALUE})
  @Permittable(value = AcceptedTokenType.SYSTEM)
  public @ResponseBody
  ResponseEntity<Signature>
  getApplicationSignature(@PathVariable("applicationidentifier") @Nonnull String applicationIdentifier,
                          @PathVariable("timestamp") @ValidKeyTimestamp String timestamp) {
    return service.getSignatureForApplication(applicationIdentifier, timestamp)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> ServiceException.notFound("Signature for application ''" + applicationIdentifier + "'' and key timestamp ''" + timestamp + "'' doesn''t exist."));
  }

  @RequestMapping(value = "/{applicationidentifier}", method = RequestMethod.DELETE,
          consumes = {MediaType.ALL_VALUE},
          produces = {MediaType.APPLICATION_JSON_VALUE})
  @Permittable(value = AcceptedTokenType.SYSTEM)
  public @ResponseBody
  ResponseEntity<Void>
  deleteApplication(@PathVariable("applicationidentifier") @Nonnull String applicationIdentifier) {
    checkApplicationIdentifier(applicationIdentifier);
    commandGateway.process(new DeleteApplicationCommand(applicationIdentifier));
    return ResponseEntity.accepted().build();
  }

  @RequestMapping(value = "/{applicationidentifier}/permissions", method = RequestMethod.POST,
          consumes = {MediaType.ALL_VALUE},
          produces = {MediaType.APPLICATION_JSON_VALUE})
  @Permittable(value = AcceptedTokenType.SYSTEM)
  @Permittable(value = AcceptedTokenType.TENANT, permittedEndpoint = "applications/{applicationidentifier}/permissions", groupId = PermittableGroupIds.APPLICATION_SELF_MANAGEMENT)
  public @ResponseBody
  ResponseEntity<Void>
  createApplicationPermission(@PathVariable("applicationidentifier") @Nonnull String applicationIdentifier,
                              @RequestBody @Valid Permission permission) {
    checkApplicationIdentifier(applicationIdentifier);
    checkPermittableGroupIdentifier(permission.getPermittableEndpointGroupIdentifier());
    commandGateway.process(new CreateApplicationPermissionCommand(applicationIdentifier, permission));
    return ResponseEntity.accepted().build();
  }

  @RequestMapping(value = "/{applicationidentifier}/permissions", method = RequestMethod.GET,
          consumes = {MediaType.ALL_VALUE},
          produces = {MediaType.APPLICATION_JSON_VALUE})
  @Permittable(value = AcceptedTokenType.SYSTEM)
  @Permittable(value = AcceptedTokenType.TENANT, permittedEndpoint = "applications/{applicationidentifier}/permissions", groupId = PermittableGroupIds.APPLICATION_SELF_MANAGEMENT)
  public @ResponseBody
  ResponseEntity<List<Permission>>
  getApplicationPermissions(@PathVariable("applicationidentifier") @Nonnull String applicationIdentifier) {
    checkApplicationIdentifier(applicationIdentifier);
    return ResponseEntity.ok(service.getAllPermissionsForApplication(applicationIdentifier));
  }

  @RequestMapping(value = "/{applicationidentifier}/permissions/{permissionidentifier}", method = RequestMethod.GET,
          consumes = {MediaType.ALL_VALUE},
          produces = {MediaType.APPLICATION_JSON_VALUE})
  @Permittable(value = AcceptedTokenType.SYSTEM)
  public @ResponseBody
  ResponseEntity<Permission> getApplicationPermission(@PathVariable("applicationidentifier") String applicationIdentifier,
                                                     @PathVariable("permissionidentifier") String permittableEndpointGroupIdentifier) {
    return service.getPermissionForApplication(applicationIdentifier, permittableEndpointGroupIdentifier)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> ServiceException.notFound("Application permission ''{0}.{1}'' doesn''t exist.",
                    applicationIdentifier, permittableEndpointGroupIdentifier));
  }

  @RequestMapping(value = "/{applicationidentifier}/permissions/{permissionidentifier}", method = RequestMethod.DELETE,
          consumes = {MediaType.ALL_VALUE},
          produces = {MediaType.APPLICATION_JSON_VALUE})
  @Permittable(value = AcceptedTokenType.SYSTEM)
  public @ResponseBody
  ResponseEntity<Void>
  deleteApplicationPermission(@PathVariable("applicationidentifier") @Nonnull String applicationIdentifier,
                              @PathVariable("permissionidentifier") @Nonnull String permittableEndpointGroupIdentifier)
  {
    checkApplicationPermissionIdentifier(service, applicationIdentifier, permittableEndpointGroupIdentifier);
    commandGateway.process(new DeleteApplicationPermissionCommand(applicationIdentifier, permittableEndpointGroupIdentifier));
    return ResponseEntity.accepted().build();
  }

  private void checkApplicationIdentifier(final @Nonnull String identifier) {
    if (!service.applicationExists(identifier))
      throw ServiceException.notFound("Application with identifier ''" + identifier + "'' doesn''t exist.");
  }

  static void checkApplicationPermissionIdentifier(final @Nonnull ApplicationService service,
                                                   final @Nonnull String applicationIdentifier,
                                                   final @Nonnull String permittableEndpointGroupIdentifier) {
    if (!service.applicationPermissionExists(applicationIdentifier, permittableEndpointGroupIdentifier))
      throw ServiceException.notFound("Application permission ''{0}.{1}'' doesn''t exist.",
              applicationIdentifier, permittableEndpointGroupIdentifier);
  }

  private void checkPermittableGroupIdentifier(final String permittableEndpointGroupIdentifier) {
    permittableGroupService.findByIdentifier(permittableEndpointGroupIdentifier)
            .orElseThrow(() -> ServiceException.notFound("Permittable group ''{0}'' doesn''t exist.", permittableEndpointGroupIdentifier));
  }
}