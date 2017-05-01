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
package listener;

import io.mifos.core.lang.config.TenantHeaderFilter;
import io.mifos.core.test.listener.EventRecorder;
import io.mifos.identity.api.v1.events.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@Component
public class ApplicationEventListener {

  private final EventRecorder eventRecorder;

  @Autowired
  public ApplicationEventListener(@SuppressWarnings("SpringJavaAutowiringInspection") final EventRecorder eventRecorder)
  {
    this.eventRecorder = eventRecorder;
  }

  @JmsListener(
          subscription = EventConstants.DESTINATION,
          destination = EventConstants.DESTINATION,
          selector = EventConstants.SELECTOR_PUT_APPLICATION_SIGNATURE
  )
  public void onSetApplicationSignature(
          @Header(TenantHeaderFilter.TENANT_HEADER)final String tenant,
          final String payload) throws Exception {
    eventRecorder.event(tenant, EventConstants.OPERATION_PUT_APPLICATION_SIGNATURE, payload, ApplicationSignatureEvent.class);
  }

  @JmsListener(
          subscription = EventConstants.DESTINATION,
          destination = EventConstants.DESTINATION,
          selector = EventConstants.SELECTOR_DELETE_APPLICATION
  )
  public void onDeleteApplication(
          @Header(TenantHeaderFilter.TENANT_HEADER)final String tenant,
          final String payload) throws Exception {
    eventRecorder.event(tenant, EventConstants.OPERATION_DELETE_APPLICATION, payload, String.class);
  }

  @JmsListener(
          subscription = EventConstants.DESTINATION,
          destination = EventConstants.DESTINATION,
          selector = EventConstants.SELECTOR_POST_APPLICATION_PERMISSION
  )
  public void onCreateApplicationPermission(
          @Header(TenantHeaderFilter.TENANT_HEADER)final String tenant,
          final String payload) throws Exception {
    eventRecorder.event(tenant, EventConstants.OPERATION_POST_APPLICATION_PERMISSION, payload, ApplicationPermissionEvent.class);
  }

  @JmsListener(
          subscription = EventConstants.DESTINATION,
          destination = EventConstants.DESTINATION,
          selector = EventConstants.SELECTOR_DELETE_APPLICATION_PERMISSION
  )
  public void onDeleteApplicationPermission(
          @Header(TenantHeaderFilter.TENANT_HEADER)final String tenant,
          final String payload) throws Exception {
    eventRecorder.event(tenant, EventConstants.OPERATION_DELETE_APPLICATION_PERMISSION, payload, ApplicationPermissionEvent.class);
  }

  @JmsListener(
          subscription = EventConstants.DESTINATION,
          destination = EventConstants.DESTINATION,
          selector = EventConstants.SELECTOR_PUT_APPLICATION_PERMISSION_USER_ENABLED
  )
  public void onPutApplicationPermissionEnabledForUser(
          @Header(TenantHeaderFilter.TENANT_HEADER)final String tenant,
          final String payload) throws Exception {
    eventRecorder.event(tenant, EventConstants.OPERATION_PUT_APPLICATION_PERMISSION_USER_ENABLED, payload, ApplicationPermissionUserEvent.class);
  }

  @JmsListener(
          subscription = EventConstants.DESTINATION,
          destination = EventConstants.DESTINATION,
          selector = EventConstants.SELECTOR_POST_APPLICATION_CALLENDPOINTSET
  )
  public void onCreateApplicationEndpointSet(
          @Header(TenantHeaderFilter.TENANT_HEADER)final String tenant,
          final String payload) throws Exception {
    eventRecorder.event(tenant, EventConstants.OPERATION_POST_APPLICATION_CALLENDPOINTSET, payload, ApplicationCallEndpointSetEvent.class);
  }

  @JmsListener(
          subscription = EventConstants.DESTINATION,
          destination = EventConstants.DESTINATION,
          selector = EventConstants.SELECTOR_PUT_APPLICATION_CALLENDPOINTSET
  )
  public void onSetApplicationEndpointSet(
          @Header(TenantHeaderFilter.TENANT_HEADER)final String tenant,
          final String payload) throws Exception {
    eventRecorder.event(tenant, EventConstants.OPERATION_PUT_APPLICATION_CALLENDPOINTSET, payload, ApplicationCallEndpointSetEvent.class);
  }

  @JmsListener(
          subscription = EventConstants.DESTINATION,
          destination = EventConstants.DESTINATION,
          selector = EventConstants.SELECTOR_DELETE_APPLICATION_CALLENDPOINTSET
  )
  public void onDeleteApplicationEndpointSet(
          @Header(TenantHeaderFilter.TENANT_HEADER)final String tenant,
          final String payload) throws Exception {
    eventRecorder.event(tenant, EventConstants.OPERATION_DELETE_APPLICATION_CALLENDPOINTSET, payload, ApplicationCallEndpointSetEvent.class);
  }
}