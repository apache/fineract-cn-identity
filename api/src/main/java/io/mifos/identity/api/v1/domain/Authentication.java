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
package io.mifos.identity.api.v1.domain;

import org.hibernate.validator.constraints.NotBlank;

import javax.annotation.Nullable;
import javax.validation.constraints.Null;
import java.util.Objects;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
public class Authentication {
  @NotBlank
  private String tokenType;

  @NotBlank
  private String accessToken;

  @NotBlank
  private String accessTokenExpiration;

  @NotBlank
  private String refreshTokenExpiration;

  /**
   * If password expiration is in the past, then the tokens provided only allow the user to change his/her password.
   * If password expiration is null then password will never expire.
   */
  @Nullable
  private String passwordExpiration;

  public Authentication()
  {
  }

  public Authentication(
      final String accessToken,
      final String accessTokenExpiration,
      final String refreshTokenExpiration,
      final String passwordExpiration) {
    this.tokenType = "bearer";
    this.accessToken = accessToken;
    this.accessTokenExpiration = accessTokenExpiration;
    this.refreshTokenExpiration = refreshTokenExpiration;
    this.passwordExpiration = passwordExpiration;
  }

  public String getTokenType() {
    return tokenType;
  }

  public void setTokenType(String tokenType) {
    this.tokenType = tokenType;
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

  @Override public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Authentication))
      return false;
    Authentication that = (Authentication) o;
    return Objects.equals(tokenType, that.tokenType) &&
        Objects.equals(accessToken, that.accessToken) &&
        Objects.equals(accessTokenExpiration, that.accessTokenExpiration) &&
        Objects.equals(refreshTokenExpiration, that.refreshTokenExpiration) &&
        Objects.equals(passwordExpiration, that.passwordExpiration);
  }

  @Override public int hashCode() {
    return Objects
        .hash(tokenType, accessToken, accessTokenExpiration, refreshTokenExpiration,
            passwordExpiration);
  }

  @Override public String toString() {
    return "Authentication{" +
        "tokenType='" + tokenType + '\'' +
        ", accessToken='" + accessToken + '\'' +
        ", accessTokenExpiration='" + accessTokenExpiration + '\'' +
        ", refreshTokenExpiration='" + refreshTokenExpiration + '\'' +
        ", passwordExpiration='" + passwordExpiration + '\'' +
        '}';
  }
}
