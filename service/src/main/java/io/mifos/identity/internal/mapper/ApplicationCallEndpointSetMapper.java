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
package io.mifos.identity.internal.mapper;

import io.mifos.identity.api.v1.domain.CallEndpointSet;
import io.mifos.identity.internal.repository.ApplicationCallEndpointSetEntity;

import java.util.Collections;

/**
 * @author Myrle Krantz
 */
public interface ApplicationCallEndpointSetMapper {
  static ApplicationCallEndpointSetEntity mapToEntity(
          final String applicationIdentifier,
          final CallEndpointSet callEndpointSet)
  {
    final ApplicationCallEndpointSetEntity ret = new ApplicationCallEndpointSetEntity();
    ret.setApplicationIdentifier(applicationIdentifier);
    ret.setCallEndpointSetIdentifier(callEndpointSet.getIdentifier());
    ret.setCallEndpointGroupIdentifiers(callEndpointSet.getPermittableEndpointGroupIdentifiers());
    return ret;
  }

  static CallEndpointSet map(final ApplicationCallEndpointSetEntity entity) {
    final CallEndpointSet ret = new CallEndpointSet();
    ret.setIdentifier(entity.getCallEndpointSetIdentifier());
    if (entity.getCallEndpointGroupIdentifiers() == null)
      ret.setPermittableEndpointGroupIdentifiers(Collections.emptyList());
    else
      ret.setPermittableEndpointGroupIdentifiers(entity.getCallEndpointGroupIdentifiers());
    return ret;
  }
}
