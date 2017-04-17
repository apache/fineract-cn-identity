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
package io.mifos.identity.api.v1.client;

import io.mifos.anubis.api.v1.TokenConstants;
import io.mifos.core.api.util.CustomFeignClientsConfiguration;
import io.mifos.identity.api.v1.domain.Authentication;
import io.mifos.identity.api.v1.domain.Permission;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@FeignClient(name="identity-v1", path="/identity/v1", configuration=CustomFeignClientsConfiguration.class)
public interface IdentityManagerForApplications {

  @RequestMapping(value = "/token?grant_type=refresh_token", method = RequestMethod.POST,
          consumes = {MediaType.APPLICATION_JSON_VALUE},
          produces = {MediaType.ALL_VALUE})
  Authentication refresh(@CookieValue(TokenConstants.REFRESH_TOKEN_COOKIE_NAME) String refreshToken);

  @RequestMapping(value = "/applications/{applicationidentifier}/permissions", method = RequestMethod.POST,
          consumes = {MediaType.APPLICATION_JSON_VALUE},
          produces = {MediaType.ALL_VALUE})
  void createApplicationPermission(@PathVariable("applicationidentifier") String applicationIdentifier, Permission permission);

  @RequestMapping(value = "/applications/{applicationidentifier}/permissions", method = RequestMethod.GET,
          consumes = {MediaType.APPLICATION_JSON_VALUE},
          produces = {MediaType.ALL_VALUE})
  List<Permission> getApplicationPermissions(@PathVariable("applicationidentifier") String applicationIdentifier);

  @RequestMapping(value = "/applications/{applicationidentifier}/permissions/{permissionidentifier}", method = RequestMethod.DELETE,
          consumes = {MediaType.APPLICATION_JSON_VALUE},
          produces = {MediaType.ALL_VALUE})
  void deleteApplicationPermission(@PathVariable("applicationidentifier") String applicationIdentifier,
                                   @PathVariable("permissionidentifier") String permittableEndpointGroupIdentifier);
}