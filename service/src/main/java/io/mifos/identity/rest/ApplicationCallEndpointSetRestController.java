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
import io.mifos.identity.api.v1.domain.CallEndpointSet;
import io.mifos.identity.internal.command.ChangeApplicationCallEndpointSetCommand;
import io.mifos.identity.internal.command.CreateApplicationCallEndpointSetCommand;
import io.mifos.identity.internal.command.DeleteApplicationCallEndpointSetCommand;
import io.mifos.identity.internal.service.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@RestController
@RequestMapping("/applications/{applicationidentifier}/callendpointset")
public class ApplicationCallEndpointSetRestController {
  private final CommandGateway commandGateway;
  private final ApplicationService applicationService;

  @Autowired
  public ApplicationCallEndpointSetRestController(
          final CommandGateway commandGateway,
          final ApplicationService applicationService) {
    this.commandGateway = commandGateway;
    this.applicationService = applicationService;
  }

  @RequestMapping(method = RequestMethod.POST,
          produces = {MediaType.APPLICATION_JSON_VALUE},
          consumes = {MediaType.ALL_VALUE})
  @Permittable(value = AcceptedTokenType.SYSTEM)
  public @ResponseBody
  ResponseEntity<Void> createApplicationCallEndpointSet(
          @PathVariable("applicationidentifier") final String applicationIdentifier,
          @RequestBody final CallEndpointSet instance)
  {
    if (!applicationService.applicationExists(applicationIdentifier))
      throw ServiceException.notFound("Application ''"
              + applicationIdentifier + "'' doesn''t exist.");

    commandGateway.process(new CreateApplicationCallEndpointSetCommand(applicationIdentifier, instance));
    return ResponseEntity.accepted().build();
  }

  @RequestMapping(value = "/{callendpointsetidentifier}", method = RequestMethod.PUT,
          produces = {MediaType.APPLICATION_JSON_VALUE},
          consumes = {MediaType.ALL_VALUE})
  @Permittable(value = AcceptedTokenType.SYSTEM)
  public @ResponseBody
  ResponseEntity<Void> changeApplicationCallEndpointSet(
          @PathVariable("applicationidentifier") String applicationIdentifier,
          @PathVariable("callendpointsetidentifier") String callEndpointSetIdentifier,
          @RequestBody final CallEndpointSet instance)
  {
    if (!applicationService.applicationCallEndpointSetExists(applicationIdentifier, callEndpointSetIdentifier))
      throw ServiceException.notFound("Application call endpointset ''"
              + applicationIdentifier + "." + callEndpointSetIdentifier + "'' doesn''t exist.");

    if (!callEndpointSetIdentifier.equals(instance.getIdentifier()))
      throw ServiceException.badRequest("Instance identifiers may not be changed.");

    commandGateway.process(new ChangeApplicationCallEndpointSetCommand(applicationIdentifier, callEndpointSetIdentifier, instance));
    return ResponseEntity.accepted().build();
  }

  @RequestMapping(value = "/{callendpointsetidentifier}", method = RequestMethod.GET,
          produces = {MediaType.APPLICATION_JSON_VALUE},
          consumes = {MediaType.ALL_VALUE})
  @Permittable(value = AcceptedTokenType.SYSTEM)
  public @ResponseBody
  ResponseEntity<CallEndpointSet> getApplicationCallEndpointSet(
          @PathVariable("applicationidentifier") String applicationIdentifier,
          @PathVariable("callendpointsetidentifier") String callEndpointSetIdentifier)
  {
    if (!applicationService.applicationCallEndpointSetExists(applicationIdentifier, callEndpointSetIdentifier))
      throw ServiceException.notFound("Application call endpointset ''"
              + applicationIdentifier + "." + callEndpointSetIdentifier + "'' doesn''t exist.");

    return applicationService.getCallEndpointSetForApplication(applicationIdentifier, callEndpointSetIdentifier)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> ServiceException.notFound("Call endpointset for application " + applicationIdentifier + " and call endpointset identifier " + callEndpointSetIdentifier + "doesn't exist."));
  }

  @RequestMapping(method = RequestMethod.GET,
          produces = {MediaType.APPLICATION_JSON_VALUE},
          consumes = {MediaType.ALL_VALUE})
  @Permittable(value = AcceptedTokenType.SYSTEM)
  public @ResponseBody
  ResponseEntity<List<CallEndpointSet>>
  getApplicationCallEndpointSets(
          @PathVariable("applicationidentifier") final String applicationIdentifier)
  {
    return ResponseEntity.ok(applicationService.getAllCallEndpointSetsForApplication(applicationIdentifier));
  }

  @RequestMapping(value = "/{callendpointsetidentifier}", method = RequestMethod.DELETE,
          produces = {MediaType.APPLICATION_JSON_VALUE},
          consumes = {MediaType.ALL_VALUE})
  @Permittable(value = AcceptedTokenType.SYSTEM)
  public @ResponseBody
  ResponseEntity<Void> deleteApplicationCallEndpointSet(
          @PathVariable("applicationidentifier") final String applicationIdentifier,
          @PathVariable("callendpointsetidentifier") final String callEndpointSetIdentifier)
  {
    if (!applicationService.applicationCallEndpointSetExists(applicationIdentifier, callEndpointSetIdentifier))
      throw ServiceException.notFound("Application call endpointset ''"
              + applicationIdentifier + "." + callEndpointSetIdentifier + "'' doesn''t exist.");
    commandGateway.process(new DeleteApplicationCallEndpointSetCommand(applicationIdentifier, callEndpointSetIdentifier));
    return ResponseEntity.accepted().build();
  }
}