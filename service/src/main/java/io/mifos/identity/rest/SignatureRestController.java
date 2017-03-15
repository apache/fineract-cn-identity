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
import io.mifos.identity.internal.service.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@RestController //
@RequestMapping("/signature") //
public class SignatureRestController {
  private final TenantService tenantService;

  @Autowired public SignatureRestController(
      final TenantService tenantService) {
    super();
    this.tenantService = tenantService;
  }

  @RequestMapping(method = RequestMethod.GET) //
  @Permittable(AcceptedTokenType.SYSTEM)
  public @ResponseBody ResponseEntity<Signature> get() {
    return new ResponseEntity<>(tenantService.getSignature(), HttpStatus.OK);
  }
}
