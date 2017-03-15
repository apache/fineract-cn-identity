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

import java.util.Objects;

/**
 * @author Myrle Krantz
 */
public class AuthenticationCommandResponse {
  private String accessToken;
  private String accessTokenExpiration;
  private String refreshToken;
  private String refreshTokenExpiration;
  private String passwordExpiration;

  public AuthenticationCommandResponse() {
  }

  public AuthenticationCommandResponse(String accessToken, String accessTokenExpiration, String refreshToken, String refreshTokenExpiration, String passwordExpiration) {
    this.accessToken = accessToken;
    this.accessTokenExpiration = accessTokenExpiration;
    this.refreshToken = refreshToken;
    this.refreshTokenExpiration = refreshTokenExpiration;
    this.passwordExpiration = passwordExpiration;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public String getAccessTokenExpiration() {
    return accessTokenExpiration;
  }

  public void setAccessTokenExpiration(String accessTokenExpiration) {
    this.accessTokenExpiration = accessTokenExpiration;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  public String getRefreshTokenExpiration() {
    return refreshTokenExpiration;
  }

  public void setRefreshTokenExpiration(String refreshTokenExpiration) {
    this.refreshTokenExpiration = refreshTokenExpiration;
  }

  public String getPasswordExpiration() {
    return passwordExpiration;
  }

  public void setPasswordExpiration(String passwordExpiration) {
    this.passwordExpiration = passwordExpiration;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AuthenticationCommandResponse that = (AuthenticationCommandResponse) o;
    return Objects.equals(accessToken, that.accessToken) &&
            Objects.equals(accessTokenExpiration, that.accessTokenExpiration) &&
            Objects.equals(refreshToken, that.refreshToken) &&
            Objects.equals(refreshTokenExpiration, that.refreshTokenExpiration) &&
            Objects.equals(passwordExpiration, that.passwordExpiration);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessToken, accessTokenExpiration, refreshToken, refreshTokenExpiration, passwordExpiration);
  }

  @Override
  public String toString() {
    return "AuthenticationCommandResponse{" +
            "accessToken='" + accessToken + '\'' +
            ", accessTokenExpiration='" + accessTokenExpiration + '\'' +
            ", refreshToken='" + refreshToken + '\'' +
            ", refreshTokenExpiration='" + refreshTokenExpiration + '\'' +
            ", passwordExpiration='" + passwordExpiration + '\'' +
            '}';
  }
}
