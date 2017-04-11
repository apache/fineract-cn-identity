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
package io.mifos.identity.internal.command.handler;

import io.jsonwebtoken.*;
import io.mifos.anubis.api.v1.TokenConstants;
import io.mifos.anubis.provider.InvalidKeyTimestampException;
import io.mifos.anubis.provider.TenantRsaKeyProvider;
import io.mifos.anubis.security.AmitAuthenticationException;
import io.mifos.anubis.token.TokenSerializationResult;
import io.mifos.anubis.token.TokenType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.security.Key;
import java.security.PrivateKey;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author Myrle Krantz
 */
@Component
class TenantRefreshTokenSerializer {

  final private TenantRsaKeyProvider tenantRsaKeyProvider;

  @SuppressWarnings("SpringJavaAutowiringInspection")
  @Autowired
  private TenantRefreshTokenSerializer(final TenantRsaKeyProvider tenantRsaKeyProvider) {
    this.tenantRsaKeyProvider = tenantRsaKeyProvider;
  }

  static class Specification {
    private String keyTimestamp;
    private PrivateKey privateKey;
    private String user;
    private long secondsToLive;

    Specification setKeyTimestamp(final String keyTimestamp) {
      this.keyTimestamp = keyTimestamp;
      return this;
    }

    Specification setPrivateKey(final PrivateKey privateKey) {
      this.privateKey = privateKey;
      return this;
    }

    Specification setUser(final String user) {
      this.user = user;
      return this;
    }

    Specification setSecondsToLive(final long secondsToLive) {
      this.secondsToLive = secondsToLive;
      return this;
    }
  }

  static class Deserialized {
    final private String userIdentifier;
    final private Date expiration;

    Deserialized(final String userIdentifier, final Date expiration) {
      this.userIdentifier = userIdentifier;
      this.expiration = expiration;
    }

    String getUserIdentifier() {
      return userIdentifier;
    }

    Date getExpiration() {
      return expiration;
    }
  }

  TokenSerializationResult build(final Specification specification)
  {
    final long issued = System.currentTimeMillis();

    if (specification.keyTimestamp == null) {
      throw new IllegalArgumentException("token signature timestamp must not be null.");
    }
    if (specification.privateKey == null) {
      throw new IllegalArgumentException("token signature privateKey must not be null.");
    }

    final JwtBuilder jwtBuilder =
        Jwts.builder()
            .setSubject(specification.user)
            .claim(TokenConstants.JWT_SIGNATURE_TIMESTAMP_CLAIM, specification.keyTimestamp)
            .setIssuer(TokenType.TENANT.getIssuer())
            .setIssuedAt(new Date(issued))
            .signWith(SignatureAlgorithm.RS512, specification.privateKey);
    if (specification.secondsToLive <= 0) {
      throw new IllegalArgumentException("token secondsToLive must be positive.");
    }

    final Date expiration = new Date(issued + TimeUnit.SECONDS.toMillis(specification.secondsToLive));
    jwtBuilder.setExpiration(expiration);

    return new TokenSerializationResult(TokenConstants.PREFIX + jwtBuilder.compact(), expiration);
  }

  Deserialized deserialize(final String refreshToken)
  {
    final Optional<String> tokenString = getJwtTokenString(refreshToken);

    final String token = tokenString.orElseThrow(AmitAuthenticationException::invalidToken);


    try {
      final JwtParser parser = Jwts.parser().setSigningKeyResolver(new SigningKeyResolver() {
        @Override public Key resolveSigningKey(final JwsHeader header, final Claims claims) {
          final String keyTimestamp = getKeyTimestampFromClaims(claims);

          try {
            return tenantRsaKeyProvider.getPublicKey(keyTimestamp);
          }
          catch (final IllegalArgumentException e)
          {
            throw AmitAuthenticationException.missingTenant();
          }
          catch (final InvalidKeyTimestampException e)
          {
            throw AmitAuthenticationException.invalidTokenKeyTimestamp(TokenType.TENANT.getIssuer(), keyTimestamp);
          }
        }

        @Override public Key resolveSigningKey(final JwsHeader header, final String plaintext) {
          return null;
        }
      }).requireIssuer(TokenType.TENANT.getIssuer());

      @SuppressWarnings("unchecked") Jwt<Header, Claims> jwt = parser.parse(token);

      return new Deserialized(jwt.getBody().getSubject(), jwt.getBody().getExpiration());
    }
    catch (final JwtException e) {
      throw AmitAuthenticationException.invalidToken();
    }
  }

  private static Optional<String> getJwtTokenString(final String refreshToken) {
    if ((refreshToken == null) || refreshToken.equals(
        TokenConstants.NO_AUTHENTICATION)){
      return Optional.empty();
    }

    if (!refreshToken.startsWith(TokenConstants.PREFIX)) {
      throw AmitAuthenticationException.invalidToken();
    }
    return Optional.of(refreshToken.substring(TokenConstants.PREFIX.length()).trim());
  }

  private @Nonnull String getKeyTimestampFromClaims(final Claims claims) {
    return claims.get(TokenConstants.JWT_SIGNATURE_TIMESTAMP_CLAIM, String.class);
  }
}
