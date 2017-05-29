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
import io.mifos.core.api.util.UserContextHolder;
import io.mifos.core.command.gateway.CommandGateway;
import io.mifos.core.lang.ServiceException;
import io.mifos.identity.api.v1.PermittableGroupIds;
import io.mifos.identity.api.v1.domain.*;
import io.mifos.identity.internal.command.ChangeUserRoleCommand;
import io.mifos.identity.internal.command.ChangeUserPasswordCommand;
import io.mifos.identity.internal.command.CreateUserCommand;
import io.mifos.identity.internal.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

import static io.mifos.identity.internal.util.IdentityConstants.SU_NAME;
import static io.mifos.identity.rest.PathConstants.IDENTIFIER_PATH_VARIABLE;


/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@RestController
@RequestMapping("/users")
public class UserRestController {
  private final UserService service;
  private final CommandGateway commandGateway;

  @Autowired
  public UserRestController(
          final CommandGateway commandGateway,
          final UserService service) {
    this.commandGateway = commandGateway;
    this.service = service;
  }

  @RequestMapping(method = RequestMethod.GET,
      consumes = {MediaType.ALL_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.IDENTITY_MANAGEMENT)
  public @ResponseBody List<User> findAll() {
    return this.service.findAll();
  }

  @RequestMapping(method = RequestMethod.POST,
          consumes = {MediaType.APPLICATION_JSON_VALUE},
          produces = {MediaType.APPLICATION_JSON_VALUE})
  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.IDENTITY_MANAGEMENT)
  public @ResponseBody ResponseEntity<Void> create(@RequestBody @Valid final UserWithPassword instance)
  {
    if (instance == null)
      throw ServiceException.badRequest("Instance may not be null.");

    if (service.findByIdentifier(instance.getIdentifier()).isPresent())
      throw ServiceException.conflict("Instance already exists with identifier:" + instance.getIdentifier());

    final CreateUserCommand createCommand = new CreateUserCommand(instance);
    this.commandGateway.process(createCommand);
    return new ResponseEntity<>(HttpStatus.ACCEPTED);
  }

  @RequestMapping(value= PathConstants.IDENTIFIER_RESOURCE_STRING, method = RequestMethod.GET,
      consumes = {MediaType.ALL_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.IDENTITY_MANAGEMENT)
  public @ResponseBody ResponseEntity<User> get(@PathVariable(IDENTIFIER_PATH_VARIABLE) final String userIdentifier)
  {
    return new ResponseEntity<>(checkIdentifier(userIdentifier), HttpStatus.OK);
  }

  @RequestMapping(value = PathConstants.IDENTIFIER_RESOURCE_STRING + "/roleIdentifier", method = RequestMethod.PUT,
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.IDENTITY_MANAGEMENT)
  public @ResponseBody ResponseEntity<Void> changeUserRole(
      @PathVariable(IDENTIFIER_PATH_VARIABLE) final String userIdentifier,
      @RequestBody @Valid final RoleIdentifier roleIdentifier)
  {
    if (userIdentifier.equals(SU_NAME))
      throw ServiceException.badRequest("Role of user with identifier: " + userIdentifier + " cannot be changed.");

    checkIdentifier(userIdentifier);

    final ChangeUserRoleCommand changeCommand = new ChangeUserRoleCommand(userIdentifier, roleIdentifier.getIdentifier());
    this.commandGateway.process(changeCommand);
    return new ResponseEntity<>(HttpStatus.ACCEPTED);
  }

  @RequestMapping(value = PathConstants.IDENTIFIER_RESOURCE_STRING + "/permissions", method = RequestMethod.GET,
          consumes = {MediaType.ALL_VALUE},
          produces = {MediaType.APPLICATION_JSON_VALUE})
  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.IDENTITY_MANAGEMENT)
  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.SELF_MANAGEMENT, permittedEndpoint = "/users/{useridentifier}/permissions")
  @ResponseBody
  Set<Permission> getUserPermissions(@PathVariable(IDENTIFIER_PATH_VARIABLE) String userIdentifier)
  {
    checkIdentifier(userIdentifier);

    return service.getPermissions(userIdentifier);
  }

  @RequestMapping(value = PathConstants.IDENTIFIER_RESOURCE_STRING + "/password", method = RequestMethod.PUT,
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.IDENTITY_MANAGEMENT)
  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.SELF_MANAGEMENT, permittedEndpoint = "/users/{useridentifier}/password")
  public @ResponseBody ResponseEntity<Void> changeUserPassword(
      @PathVariable(IDENTIFIER_PATH_VARIABLE) final String userIdentifier,
      @RequestBody @Valid final Password password)
  {
    if (userIdentifier.equals(SU_NAME) && !UserContextHolder.checkedGetUser().equals(SU_NAME))
      throw ServiceException.badRequest("Role of user with identifier: " + userIdentifier + " cannot be changed.");

    checkIdentifier(userIdentifier);

    checkPassword(password);

    final ChangeUserPasswordCommand changeCommand = new ChangeUserPasswordCommand(userIdentifier, password.getPassword());
    this.commandGateway.process(changeCommand);
    return new ResponseEntity<>(HttpStatus.ACCEPTED);
  }

  private void checkPassword(final Password password) {
    if (password == null || password.getPassword() == null || password.getPassword().isEmpty())
      throw ServiceException.badRequest("password may not be empty.");
  }

  private User checkIdentifier(final String identifier) {
    if (identifier == null)
      throw ServiceException.badRequest("identifier may not be null.");

    return service.findByIdentifier(identifier)
            .orElseThrow(() -> ServiceException.notFound("Instance with identifier " + identifier + " doesn't exist."));
  }
}