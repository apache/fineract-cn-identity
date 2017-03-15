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

import com.datastax.driver.core.TypeCodec;
import com.datastax.driver.extras.codecs.enums.EnumNameCodec;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public enum AllowedOperationType {
  READ, //GET, TRACE
  CHANGE, //POST, PUT
  DELETE; //DELETE

  public static final Set<AllowedOperationType> ALL = Collections.unmodifiableSet(
      new HashSet<AllowedOperationType>() {{add(READ); add(CHANGE); add(DELETE);}});

  static TypeCodec<AllowedOperationType> getCodec()
  {
    return new EnumNameCodec<>(AllowedOperationType.class);
  }

  static public AllowedOperationType fromHttpMethod(final String httpMethod)
  {
    switch (httpMethod) {
      case "GET":
      case "TRACE":
        return READ;
      case "POST":
      case "PUT":
        return CHANGE;
      case "DELETE":
        return DELETE;
      default:
        return null;
    }
  }

}
