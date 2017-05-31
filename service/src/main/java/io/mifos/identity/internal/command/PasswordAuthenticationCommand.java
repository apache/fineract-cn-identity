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
package io.mifos.identity.internal.command;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
public class PasswordAuthenticationCommand {
  private String useridentifier;
  private transient String password;

  PasswordAuthenticationCommand() {}

  public PasswordAuthenticationCommand(
      final String useridentifier,
      final String password) {
    this.useridentifier = useridentifier;
    this.password = password;
  }

  public String getUseridentifier() {
    return useridentifier;
  }

  public void setUseridentifier(String useridentifier) {
    this.useridentifier = useridentifier;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  @Override
  public String toString() {
    return "PasswordAuthenticationCommand{" +
            "useridentifier='" + useridentifier + '\'' +
            '}';
  }
}
