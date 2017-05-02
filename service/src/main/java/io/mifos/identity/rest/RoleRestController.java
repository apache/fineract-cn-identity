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
import io.mifos.identity.api.v1.domain.Role;
import io.mifos.identity.api.v1.validation.CheckRoleChangeable;
import io.mifos.identity.internal.command.ChangeRoleCommand;
import io.mifos.identity.internal.command.CreateRoleCommand;
import io.mifos.identity.internal.command.DeleteRoleCommand;
import io.mifos.identity.internal.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;


/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@RestController
@RequestMapping("/roles")
public class RoleRestController
{
  private final RoleService service;
  private final CommandGateway commandGateway;

  @Autowired public RoleRestController(
      final CommandGateway commandGateway,
      final RoleService service)
  {
    this.commandGateway = commandGateway;
    this.service = service;
  }

  @RequestMapping(method = RequestMethod.POST,
          consumes = {MediaType.APPLICATION_JSON_VALUE},
          produces = {MediaType.APPLICATION_JSON_VALUE})
  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.ROLE_MANAGEMENT)
  public @ResponseBody ResponseEntity<Void> create(@RequestBody @Valid final Role instance)
  {
    if (instance == null)
      throw ServiceException.badRequest("Instance may not be null.");

    if (service.findByIdentifier(instance.getIdentifier()).isPresent())
      throw ServiceException.conflict("Instance already applicationExists with identifier:" + instance.getIdentifier());

    final CreateRoleCommand createCommand = new CreateRoleCommand(instance);
    this.commandGateway.process(createCommand);
    return new ResponseEntity<>(HttpStatus.ACCEPTED);
  }

  @RequestMapping(method = RequestMethod.GET,
      consumes = {MediaType.ALL_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.ROLE_MANAGEMENT)
  public @ResponseBody List<Role> findAll() {
    return service.findAll();
  }

  @RequestMapping(value= PathConstants.IDENTIFIER_RESOURCE_STRING, method = RequestMethod.GET,
      consumes = {MediaType.ALL_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @Permittable(AcceptedTokenType.TENANT)
  public @ResponseBody ResponseEntity<Role> get(@PathVariable(PathConstants.IDENTIFIER_PATH_VARIABLE) final String identifier)
  {
    return new ResponseEntity<>(checkIdentifier(identifier), HttpStatus.OK);
  }

  @RequestMapping(value= PathConstants.IDENTIFIER_RESOURCE_STRING, method = RequestMethod.DELETE,
      consumes = {MediaType.ALL_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.ROLE_MANAGEMENT)
  public @ResponseBody ResponseEntity<Void> delete(@PathVariable(PathConstants.IDENTIFIER_PATH_VARIABLE) final String identifier)
  {
    if (!CheckRoleChangeable.isChangeableRoleIdentifier(identifier))
      throw ServiceException.badRequest("Role with identifier: " + identifier + " cannot be deleted.");

    checkIdentifier(identifier);

    final DeleteRoleCommand deleteCommand = new DeleteRoleCommand(identifier);
    this.commandGateway.process(deleteCommand);
    return new ResponseEntity<>(HttpStatus.ACCEPTED);
  }

  @RequestMapping(value= PathConstants.IDENTIFIER_RESOURCE_STRING,method = RequestMethod.PUT,
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.ROLE_MANAGEMENT)
  public @ResponseBody ResponseEntity<Void> change(
          @PathVariable(PathConstants.IDENTIFIER_PATH_VARIABLE) final String identifier, @RequestBody @Valid final Role instance)
  {
    if (!CheckRoleChangeable.isChangeableRoleIdentifier(identifier))
      throw ServiceException.badRequest("Role with identifier: " + identifier + " cannot be changed.");

    checkIdentifier(identifier);

    if (!identifier.equals(instance.getIdentifier()))
      throw ServiceException.badRequest("Instance identifiers may not be changed.");

    final ChangeRoleCommand changeCommand = new ChangeRoleCommand(identifier, instance);
    this.commandGateway.process(changeCommand);
    return new ResponseEntity<>(HttpStatus.ACCEPTED);
  }

  private Role checkIdentifier(final String identifier) {
    if (identifier == null)
      throw ServiceException.badRequest("identifier may not be null.");

    return service.findByIdentifier(identifier)
            .orElseThrow(() -> ServiceException.notFound("Instance with identifier " + identifier + " doesn't exist."));
  }
}
