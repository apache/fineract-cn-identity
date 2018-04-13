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
import org.apache.fineract.cn.identity.api.v1.domain.PermittableGroup;
import org.apache.fineract.cn.anubis.annotation.AcceptedTokenType;
import org.apache.fineract.cn.anubis.annotation.Permittable;
import org.apache.fineract.cn.command.gateway.CommandGateway;
import org.apache.fineract.cn.identity.internal.command.CreatePermittableGroupCommand;
import org.apache.fineract.cn.identity.internal.service.PermittableGroupService;
import org.apache.fineract.cn.lang.ServiceException;
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
@RequestMapping("/permittablegroups")
public class PermittableGroupRestController {
  private final PermittableGroupService service;
  private final CommandGateway commandGateway;

  public PermittableGroupRestController(final PermittableGroupService service,
                                        final CommandGateway commandGateway) {
    this.service = service;
    this.commandGateway = commandGateway;
  }

  @RequestMapping(method = RequestMethod.POST,
          consumes = {MediaType.APPLICATION_JSON_VALUE},
          produces = {MediaType.APPLICATION_JSON_VALUE})
  @Permittable(value = AcceptedTokenType.SYSTEM)
  public @ResponseBody
  ResponseEntity<Void> create(@RequestBody @Valid final PermittableGroup instance)
  {
    if (instance == null)
      throw ServiceException.badRequest("Instance may not be null.");

    if (service.findByIdentifier(instance.getIdentifier()).isPresent())
      throw ServiceException.conflict("Instance already exists with identifier:" + instance.getIdentifier());

    final CreatePermittableGroupCommand createCommand = new CreatePermittableGroupCommand(instance);
    this.commandGateway.process(createCommand);
    return new ResponseEntity<>(HttpStatus.ACCEPTED);
  }

  @RequestMapping(method = RequestMethod.GET,
          consumes = {MediaType.ALL_VALUE},
          produces = {MediaType.APPLICATION_JSON_VALUE})
  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.ROLE_MANAGEMENT)
  public @ResponseBody
  List<PermittableGroup> findAll() {
    return service.findAll();
  }

  @RequestMapping(value= PathConstants.IDENTIFIER_RESOURCE_STRING, method = RequestMethod.GET,
          consumes = {MediaType.ALL_VALUE},
          produces = {MediaType.APPLICATION_JSON_VALUE})
  @Permittable(value = AcceptedTokenType.SYSTEM)
  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.ROLE_MANAGEMENT)
  public @ResponseBody ResponseEntity<PermittableGroup> get(@PathVariable(PathConstants.IDENTIFIER_PATH_VARIABLE) final String identifier)
  {
    return new ResponseEntity<>(checkIdentifier(identifier), HttpStatus.OK);
  }

  private PermittableGroup checkIdentifier(final String identifier) {
    if (identifier == null)
      throw ServiceException.badRequest("identifier may not be null.");

    return service.findByIdentifier(identifier)
            .orElseThrow(() -> ServiceException.notFound("Instance with identifier " + identifier + " doesn't exist."));
  }
}