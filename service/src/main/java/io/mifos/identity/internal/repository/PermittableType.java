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

import com.datastax.driver.mapping.annotations.Field;
import com.datastax.driver.mapping.annotations.UDT;

import java.util.Objects;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"unused", "WeakerAccess"})
@UDT(name = PermittableGroups.TYPE_NAME)
public class PermittableType {

  @Field(name = PermittableGroups.PATH_FIELD)
  private String path;
  @Field(name = PermittableGroups.METHOD_FIELD)
  private String method;
  @Field(name = PermittableGroups.SOURCE_GROUP_ID_FIELD)
  private String sourceGroupId;

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public String getSourceGroupId() {
    return sourceGroupId;
  }

  public void setSourceGroupId(String sourceGroupId) {
    this.sourceGroupId = sourceGroupId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PermittableType that = (PermittableType) o;
    return Objects.equals(path, that.path) &&
            Objects.equals(method, that.method) &&
            Objects.equals(sourceGroupId, that.sourceGroupId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path, method, sourceGroupId);
  }

  @Override
  public String toString() {
    return "PermittableType{" +
            "path='" + path + '\'' +
            ", method='" + method + '\'' +
            ", sourceGroupId='" + sourceGroupId + '\'' +
            '}';
  }
}
