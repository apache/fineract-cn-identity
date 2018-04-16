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

import org.apache.fineract.cn.anubis.annotation.AcceptedTokenType;
import org.apache.fineract.cn.anubis.annotation.Permittable;
import org.apache.fineract.cn.anubis.api.v1.domain.ApplicationSignatureSet;
import org.apache.fineract.cn.identity.internal.command.handler.Provisioner;
import org.apache.fineract.cn.identity.internal.service.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@RestController
@RequestMapping()
public class InitializeRestController {
  private final TenantService tenantService;
  private final Provisioner provisioner;

  @Autowired
  InitializeRestController(
      final TenantService tenantService,
      final Provisioner provisioner)
  {
    this.tenantService = tenantService;
    this.provisioner = provisioner;
  }

  @RequestMapping(value = "/initialize",
          method = RequestMethod.POST,
          consumes = {MediaType.ALL_VALUE},
          produces = {MediaType.APPLICATION_JSON_VALUE})
  @Permittable(AcceptedTokenType.SYSTEM)
  public @ResponseBody ResponseEntity<ApplicationSignatureSet> initializeTenant(
      @RequestParam("password") final String adminPassword)
  {
    final ApplicationSignatureSet newSignatureSet = provisioner.provisionTenant(adminPassword);
    return new ResponseEntity<>(newSignatureSet, HttpStatus.OK);
  }

  @RequestMapping(value = "/signatures",
          method = RequestMethod.POST,
          consumes = {MediaType.ALL_VALUE},
          produces = {MediaType.APPLICATION_JSON_VALUE})
  @Permittable(AcceptedTokenType.SYSTEM)
  public @ResponseBody ResponseEntity<ApplicationSignatureSet> createSignatureSet() {
    return ResponseEntity.ok(tenantService.createSignatureSet());
  }
}
