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
package io.mifos.identity.api.v1;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
public interface EventConstants {

  String DESTINATION = "identity-v1";
  String OPERATION_HEADER = "operation";

  String OPERATION_AUTHENTICATE = "auth";

  String OPERATION_POST_PERMITTABLE_GROUP = "post-role";

  String OPERATION_POST_ROLE = "post-role";
  String OPERATION_PUT_ROLE = "put-role";
  String OPERATION_DELETE_ROLE = "delete-role";

  String OPERATION_POST_USER = "post-user";
  String OPERATION_PUT_USER_ROLEIDENTIFIER = "put-user-roleidentifier";
  String OPERATION_PUT_USER_PASSWORD = "put-user-password";

  String SELECTOR_AUTHENTICATE = OPERATION_HEADER + " = '" + OPERATION_AUTHENTICATE + "'";

  String SELECTOR_POST_PERMITTABLE_GROUP = OPERATION_HEADER + " = '" + OPERATION_POST_PERMITTABLE_GROUP + "'";

  String SELECTOR_POST_ROLE = OPERATION_HEADER + " = '" + OPERATION_POST_ROLE + "'";
  String SELECTOR_PUT_ROLE = OPERATION_HEADER + " = '" + OPERATION_PUT_ROLE + "'";
  String SELECTOR_DELETE_ROLE = OPERATION_HEADER + " = '" + OPERATION_DELETE_ROLE + "'";

  String SELECTOR_POST_USER = OPERATION_HEADER + " = '" + OPERATION_POST_USER + "'";
  String SELECTOR_PUT_USER_ROLEIDENTIFIER = OPERATION_HEADER + " = '" + OPERATION_PUT_USER_ROLEIDENTIFIER + "'";
  String SELECTOR_PUT_USER_PASSWORD = OPERATION_HEADER + " = '" + OPERATION_PUT_USER_PASSWORD + "'";
}
