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

import com.google.gson.Gson;
import io.mifos.anubis.api.v1.domain.AllowedOperation;
import io.mifos.anubis.api.v1.domain.TokenContent;
import io.mifos.anubis.api.v1.domain.TokenPermission;
import io.mifos.anubis.security.AmitAuthenticationException;
import io.mifos.anubis.token.TenantAccessTokenSerializer;
import io.mifos.anubis.token.TokenSerializationResult;
import io.mifos.core.command.annotation.Aggregate;
import io.mifos.core.command.annotation.CommandHandler;
import io.mifos.core.lang.ApplicationName;
import io.mifos.core.lang.DateConverter;
import io.mifos.core.lang.ServiceException;
import io.mifos.core.lang.TenantContextHolder;
import io.mifos.core.lang.config.TenantHeaderFilter;
import io.mifos.core.lang.security.RsaPrivateKeyBuilder;
import io.mifos.identity.api.v1.events.EventConstants;
import io.mifos.identity.internal.command.AuthenticationCommandResponse;
import io.mifos.identity.internal.command.PasswordAuthenticationCommand;
import io.mifos.identity.internal.command.RefreshTokenAuthenticationCommand;
import io.mifos.identity.internal.repository.*;
import io.mifos.identity.internal.service.RoleMapper;
import io.mifos.identity.internal.util.IdentityConstants;
import io.mifos.tool.crypto.HashGenerator;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;

import java.security.PrivateKey;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"unused", "WeakerAccess"})
@Aggregate
@Component
public class AuthenticationCommandHandler {
  private final Users users;
  private final Roles roles;
  private final PermittableGroups permittableGroups;
  private final Signatures signatures;
  private final Tenants tenants;
  private final HashGenerator hashGenerator;
  private final TenantAccessTokenSerializer tenantAccessTokenSerializer;
  private final TenantRefreshTokenSerializer tenantRefreshTokenSerializer;
  private final JmsTemplate jmsTemplate;
  private final Gson gson;
  private final Logger logger;
  private final ApplicationName applicationName;

  @Value("${identity.token.access.ttl:1200}") //Given in seconds.  Default 20 minutes.
  private int accessTtl;


  @Value("${identity.token.refresh.ttl:54000}") //Given in seconds.  Default 15 hours.
  private int refreshTtl;


  @Autowired
  public AuthenticationCommandHandler(final Users users,
                                      final Roles roles,
                                      final PermittableGroups permittableGroups,
                                      final Signatures signatures,
                                      final Tenants tenants,
                                      final HashGenerator hashGenerator,
                                      @SuppressWarnings("SpringJavaAutowiringInspection")
                                      final TenantAccessTokenSerializer tenantAccessTokenSerializer,
                                      final TenantRefreshTokenSerializer tenantRefreshTokenSerializer,
                                      final JmsTemplate jmsTemplate,
                                      final ApplicationName applicationName,
                                      @Qualifier(IdentityConstants.JSON_SERIALIZER_NAME) final Gson gson,
                                      @Qualifier(IdentityConstants.LOGGER_NAME) final Logger logger) {
    this.users = users;
    this.roles = roles;
    this.permittableGroups = permittableGroups;
    this.signatures = signatures;
    this.tenants = tenants;
    this.hashGenerator = hashGenerator;
    this.tenantAccessTokenSerializer = tenantAccessTokenSerializer;
    this.tenantRefreshTokenSerializer = tenantRefreshTokenSerializer;
    this.jmsTemplate = jmsTemplate;
    this.gson = gson;
    this.logger = logger;
    this.applicationName = applicationName;
  }

  @CommandHandler
  public AuthenticationCommandResponse process(final PasswordAuthenticationCommand command)
      throws AmitAuthenticationException
  {

    final PrivateTenantInfoEntity privateTenantInfo = checkedGetPrivateTenantInfo();
    final PrivateSignatureEntity privateSignature = checkedGetPrivateSignature();

    byte[] fixedSalt = privateTenantInfo.getFixedSalt().array();
    final UserEntity user = getUser(command.getUseridentifier());

    if (!this.hashGenerator.isEqual(
        user.getPassword().array(),
        Base64Utils.decodeFromString(command.getPassword()),
        fixedSalt,
        user.getSalt().array(),
        user.getIterationCount(),
        256))
    {
      throw AmitAuthenticationException.userPasswordCombinationNotFound();
    }

    final LocalDate passwordExpiration = getExpiration(user);

    final TokenSerializationResult accessToken = getAccessToken(
            user.getIdentifier(),
            getTokenPermissions(user, passwordExpiration, privateTenantInfo.getTimeToChangePasswordAfterExpirationInDays()),
            privateSignature);

    final TokenSerializationResult refreshToken = getRefreshToken(user, privateSignature);

    fireAuthenticationEvent(user.getIdentifier());

    return new AuthenticationCommandResponse(
        accessToken.getToken(), DateConverter.toIsoString(accessToken.getExpiration()),
        refreshToken.getToken(), DateConverter.toIsoString(refreshToken.getExpiration()),
            DateConverter.toIsoString(passwordExpiration));
  }

  private PrivateSignatureEntity checkedGetPrivateSignature() {
    final Optional<PrivateSignatureEntity> privateSignature = signatures.getPrivateSignature();
    if (!privateSignature.isPresent()) {
      logger.error("Authentication attempted on tenant with no valid signature{}.", TenantContextHolder.identifier());
      throw ServiceException.internalError("Tenant has no valid signature.");
    }
    return privateSignature.get();
  }

  private PrivateTenantInfoEntity checkedGetPrivateTenantInfo() {
    final Optional<PrivateTenantInfoEntity> privateTenantInfo = tenants.getPrivateTenantInfo();
    if (!privateTenantInfo.isPresent()) {
      logger.error("Authentication attempted on uninitialized tenant {}.", TenantContextHolder.identifier());
      throw ServiceException.internalError("Tenant is not initialized.");
    }
    return privateTenantInfo.get();
  }

  @CommandHandler
  public AuthenticationCommandResponse process(final RefreshTokenAuthenticationCommand command)
      throws AmitAuthenticationException
  {
    final TenantRefreshTokenSerializer.Deserialized deserializedRefreshToken =
        tenantRefreshTokenSerializer.deserialize(command.getRefreshToken());

    final PrivateTenantInfoEntity privateTenantInfo = checkedGetPrivateTenantInfo();
    final PrivateSignatureEntity privateSignature = checkedGetPrivateSignature();

    final UserEntity user = getUser(deserializedRefreshToken.getUserIdentifier());

    final LocalDate passwordExpiration = getExpiration(user);

    final TokenSerializationResult accessToken = getAccessToken(
            user.getIdentifier(),
            getTokenPermissions(user, passwordExpiration, privateTenantInfo.getTimeToChangePasswordAfterExpirationInDays()),
            privateSignature);

    return new AuthenticationCommandResponse(
        accessToken.getToken(), DateConverter.toIsoString(accessToken.getExpiration()),
        command.getRefreshToken(), DateConverter.toIsoString(deserializedRefreshToken.getExpiration()),
        DateConverter.toIsoString(passwordExpiration));
  }

  private LocalDate getExpiration(final UserEntity user)
  {
    return LocalDate.ofEpochDay(user.getPasswordExpiresOn().getDaysSinceEpoch());
  }

  private UserEntity getUser(final String identifier) throws AmitAuthenticationException {
    final Optional<UserEntity> user = users.get(identifier);
    if (!user.isPresent()) {
      this.logger.info("Attempt to get a user who doesn't exist: " + identifier);
      throw AmitAuthenticationException.userPasswordCombinationNotFound();
    }

    return user.get();
  }

  private void fireAuthenticationEvent(final String userIdentifier) {
    this.jmsTemplate.convertAndSend(
        this.gson.toJson(userIdentifier),
        message -> {
          if (TenantContextHolder.identifier().isPresent()) {
            //noinspection OptionalGetWithoutIsPresent
            message.setStringProperty(
                TenantHeaderFilter.TENANT_HEADER,
                TenantContextHolder.identifier().get());
          }
          message.setStringProperty(EventConstants.OPERATION_HEADER,
              EventConstants.OPERATION_AUTHENTICATE
          );
          return message;
        }
    );
  }

  private TokenSerializationResult getAccessToken(
          final String identifier,
          final Set<TokenPermission> tokenPermissions,
          final PrivateSignatureEntity privateSignatureEntity) {

    final PrivateKey privateKey = new RsaPrivateKeyBuilder()
          .setPrivateKeyExp(privateSignatureEntity.getPrivateKeyExp())
          .setPrivateKeyMod(privateSignatureEntity.getPrivateKeyMod())
          .build();

      final TenantAccessTokenSerializer.Specification x =
          new TenantAccessTokenSerializer.Specification()
              .setKeyTimestamp(privateSignatureEntity.getKeyTimestamp())
              .setPrivateKey(privateKey)
              .setTokenContent(new TokenContent(new ArrayList<>(tokenPermissions)))
              .setSecondsToLive(accessTtl)
              .setUser(identifier);

      return tenantAccessTokenSerializer.build(x);
  }

  private Set<TokenPermission> getTokenPermissions(
          final UserEntity user,
          final LocalDate passwordExpiration,
          final long gracePeriod) throws AmitAuthenticationException {
    final Optional<RoleEntity> userRole = roles.get(user.getRole());
    final Set<TokenPermission> tokenPermissions;

    if (pastGracePeriod(passwordExpiration, gracePeriod))
      throw AmitAuthenticationException.passwordExpired();

    if (pastExpiration(passwordExpiration)) {
      tokenPermissions = new HashSet<>();
    }
    else {
      tokenPermissions = userRole.map(r -> r.getPermissions().stream().flatMap(this::mapPermissions).collect(Collectors.toSet()))
              .orElse(new HashSet<>());
    }

    tokenPermissions.add(
        new TokenPermission(
            applicationName + "/users/{useridentifier}/password",
            Collections.singleton(AllowedOperation.CHANGE)));
    tokenPermissions.add(
        new TokenPermission(
            applicationName + "/users/{useridentifier}/permissions",
            Collections.singleton(AllowedOperation.READ)));
    tokenPermissions.add(
        new TokenPermission(
            applicationName + "/token/_current",
            Collections.singleton(AllowedOperation.DELETE)));

    return tokenPermissions;
  }

  static boolean pastExpiration(final LocalDate passwordExpiration) {
    return LocalDate.now().compareTo(passwordExpiration) >= 0;
  }

  static boolean pastGracePeriod(final LocalDate passwordExpiration, final long gracePeriod) {
    return LocalDate.now().compareTo(passwordExpiration.plusDays(gracePeriod)) >= 0;
  }

  private Stream<TokenPermission> mapPermissions(final PermissionType permission) {
    return permittableGroups.get(permission.getPermittableGroupIdentifier())
            .map(PermittableGroupEntity::getPermittables)
            .map(Collection::stream)
            .orElse(Stream.empty())
            .filter(permittable -> isAllowed(permittable, permission))
            .map(this::getTokenPermission);
  }

  private boolean isAllowed(final PermittableType permittable, final PermissionType permission) {
    return permission.getAllowedOperations().contains(AllowedOperationType.fromHttpMethod(permittable.getMethod()));
  }

  private TokenPermission getTokenPermission(final PermittableType permittable) {
    return new TokenPermission(
            permittable.getPath(),
            Collections.singleton(RoleMapper.mapAllowedOperation(AllowedOperationType.fromHttpMethod(permittable.getMethod()))));
  }

  private TokenSerializationResult getRefreshToken(final UserEntity user,
                                                   final PrivateSignatureEntity privateSignatureEntity) {
    final PrivateKey privateKey = new RsaPrivateKeyBuilder()
        .setPrivateKeyExp(privateSignatureEntity.getPrivateKeyExp())
        .setPrivateKeyMod(privateSignatureEntity.getPrivateKeyMod())
        .build();

    final TenantRefreshTokenSerializer.Specification x =
        new TenantRefreshTokenSerializer.Specification()
            .setKeyTimestamp(privateSignatureEntity.getKeyTimestamp())
            .setPrivateKey(privateKey)
            .setSecondsToLive(refreshTtl)
            .setUser(user.getIdentifier());

    return tenantRefreshTokenSerializer.build(x);
  }
}
