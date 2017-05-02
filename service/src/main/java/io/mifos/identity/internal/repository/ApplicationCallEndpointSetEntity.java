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
package io.mifos.identity.internal.repository;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.List;

/**
 * @author Myrle Krantz
 */
@Table(name = ApplicationCallEndpointSets.TABLE_NAME)
public class ApplicationCallEndpointSetEntity {
  @PartitionKey
  @Column(name = ApplicationCallEndpointSets.APPLICATION_IDENTIFIER_COLUMN)
  private String applicationIdentifier;

  @ClusteringColumn
  @Column(name = ApplicationCallEndpointSets.CALLENDPOINTSET_IDENTIFIER_COLUMN)
  private String callEndpointSetIdentifier;

  @Column(name = ApplicationCallEndpointSets.CALLENDPOINT_GROUP_IDENTIFIERS_COLUMN)
  private List<String> callEndpointGroupIdentifiers;

  public ApplicationCallEndpointSetEntity() {
  }

  public String getApplicationIdentifier() {
    return applicationIdentifier;
  }

  public void setApplicationIdentifier(String applicationIdentifier) {
    this.applicationIdentifier = applicationIdentifier;
  }

  public String getCallEndpointSetIdentifier() {
    return callEndpointSetIdentifier;
  }

  public void setCallEndpointSetIdentifier(String callEndpointSetIdentifier) {
    this.callEndpointSetIdentifier = callEndpointSetIdentifier;
  }

  public List<String> getCallEndpointGroupIdentifiers() {
    return callEndpointGroupIdentifiers;
  }

  public void setCallEndpointGroupIdentifiers(List<String> callEndpointGroupIdentifiers) {
    this.callEndpointGroupIdentifiers = callEndpointGroupIdentifiers;
  }
}
