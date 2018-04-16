/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.cn.identity.internal.command.handler;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import org.apache.fineract.cn.identity.api.v1.events.EventConstants;
import org.apache.fineract.cn.anubis.api.v1.domain.AllowedOperation;
import org.apache.fineract.cn.anubis.api.v1.domain.TokenContent;
import org.apache.fineract.cn.anubis.api.v1.domain.TokenPermission;
import org.apache.fineract.cn.anubis.provider.InvalidKeyTimestampException;
import org.apache.fineract.cn.anubis.provider.TenantRsaKeyProvider;
import org.apache.fineract.cn.anubis.security.AmitAuthenticationException;
import org.apache.fineract.cn.anubis.token.TenantAccessTokenSerializer;
import org.apache.fineract.cn.anubis.token.TenantApplicationRsaKeyProvider;
import org.apache.fineract.cn.anubis.token.TenantRefreshTokenSerializer;
import org.apache.fineract.cn.anubis.token.TokenDeserializationResult;
import org.apache.fineract.cn.anubis.token.TokenSerializationResult;
import org.apache.fineract.cn.command.annotation.Aggregate;
import org.apache.fineract.cn.command.annotation.CommandHandler;
import org.apache.fineract.cn.command.annotation.CommandLogLevel;
import org.apache.fineract.cn.crypto.HashGenerator;
import org.apache.fineract.cn.identity.internal.command.AuthenticationCommandResponse;
import org.apache.fineract.cn.identity.internal.command.PasswordAuthenticationCommand;
import org.apache.fineract.cn.identity.internal.command.RefreshTokenAuthenticationCommand;
import org.apache.fineract.cn.identity.internal.repository.AllowedOperationType;
import org.apache.fineract.cn.identity.internal.repository.ApplicationCallEndpointSetEntity;
import org.apache.fineract.cn.identity.internal.repository.ApplicationCallEndpointSets;
import org.apache.fineract.cn.identity.internal.repository.ApplicationPermissionUsers;
import org.apache.fineract.cn.identity.internal.repository.ApplicationPermissions;
import org.apache.fineract.cn.identity.internal.repository.ApplicationSignatureEntity;
import org.apache.fineract.cn.identity.internal.repository.ApplicationSignatures;
import org.apache.fineract.cn.identity.internal.repository.PermissionType;
import org.apache.fineract.cn.identity.internal.repository.PermittableGroupEntity;
import org.apache.fineract.cn.identity.internal.repository.PermittableGroups;
import org.apache.fineract.cn.identity.internal.repository.PermittableType;
import org.apache.fineract.cn.identity.internal.repository.PrivateSignatureEntity;
import org.apache.fineract.cn.identity.internal.repository.PrivateTenantInfoEntity;
import org.apache.fineract.cn.identity.internal.repository.RoleEntity;
import org.apache.fineract.cn.identity.internal.repository.Roles;
import org.apache.fineract.cn.identity.internal.repository.Signatures;
import org.apache.fineract.cn.identity.internal.repository.Tenants;
import org.apache.fineract.cn.identity.internal.repository.UserEntity;
import org.apache.fineract.cn.identity.internal.repository.Users;
import org.apache.fineract.cn.identity.internal.service.RoleMapper;
import org.apache.fineract.cn.identity.internal.util.IdentityConstants;
import org.apache.fineract.cn.lang.ApplicationName;
import org.apache.fineract.cn.lang.DateConverter;
import org.apache.fineract.cn.lang.ServiceException;
import org.apache.fineract.cn.lang.TenantContextHolder;
import org.apache.fineract.cn.lang.config.TenantHeaderFilter;
import org.apache.fineract.cn.lang.security.RsaPrivateKeyBuilder;
import org.apache.fineract.cn.lang.security.RsaPublicKeyBuilder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;

import javax.annotation.Nullable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collector;
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
  private final TenantRsaKeyProvider tenantRsaKeyProvider;
  private final ApplicationSignatures applicationSignatures;
  private final ApplicationPermissions applicationPermissions;
  private final ApplicationPermissionUsers applicationPermissionUsers;
  private final ApplicationCallEndpointSets applicationCallEndpointSets;
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
                                      @SuppressWarnings("SpringJavaAutowiringInspection")
                                        final TenantRefreshTokenSerializer tenantRefreshTokenSerializer,
                                      @SuppressWarnings("SpringJavaAutowiringInspection")
                                        final TenantRsaKeyProvider tenantRsaKeyProvider,
                                      final ApplicationSignatures applicationSignatures,
                                      final ApplicationPermissions applicationPermissions,
                                      final ApplicationPermissionUsers applicationPermissionUsers,
                                      final ApplicationCallEndpointSets applicationCallEndpointSets,
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
    this.tenantRsaKeyProvider = tenantRsaKeyProvider;
    this.applicationSignatures = applicationSignatures;
    this.applicationPermissions = applicationPermissions;
    this.applicationPermissionUsers = applicationPermissionUsers;
    this.applicationCallEndpointSets = applicationCallEndpointSets;
    this.jmsTemplate = jmsTemplate;
    this.gson = gson;
    this.logger = logger;
    this.applicationName = applicationName;
  }

  @CommandHandler(logStart = CommandLogLevel.DEBUG, logFinish = CommandLogLevel.DEBUG)
  public AuthenticationCommandResponse process(final PasswordAuthenticationCommand command)
      throws AmitAuthenticationException
  {
    final byte[] base64decodedPassword;
    try {
      base64decodedPassword = Base64Utils.decodeFromString(command.getPassword());
    }
    catch (final IllegalArgumentException e)
    {
      throw ServiceException.badRequest("Password was not base64 encoded.");
    }

    final PrivateTenantInfoEntity privateTenantInfo = checkedGetPrivateTenantInfo();
    final PrivateSignatureEntity privateSignature = checkedGetPrivateSignature();

    byte[] fixedSalt = privateTenantInfo.getFixedSalt().array();
    final UserEntity user = getUser(command.getUseridentifier());

    if (!this.hashGenerator.isEqual(
            user.getPassword().array(),
            base64decodedPassword,
            fixedSalt,
            user.getSalt().array(),
            user.getIterationCount(),
            256))
    {
      throw AmitAuthenticationException.userPasswordCombinationNotFound();
    }

    final TokenSerializationResult refreshToken = getRefreshToken(user, privateSignature);

    final AuthenticationCommandResponse ret = getAuthenticationResponse(
            applicationName.toString(),
            Optional.empty(),
            privateTenantInfo,
            privateSignature,
            user,
            refreshToken.getToken(),
            refreshToken.getExpiration());

    fireAuthenticationEvent(user.getIdentifier());

    return ret;
  }

  private PrivateSignatureEntity checkedGetPrivateSignature() {
    final Optional<PrivateSignatureEntity> privateSignature = signatures.getPrivateSignature();
    if (!privateSignature.isPresent()) {
      logger.error("Authentication attempted on tenant with no valid signature{}.", TenantContextHolder
          .identifier());
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

  private class TenantIdentityRsaKeyProvider implements TenantApplicationRsaKeyProvider {
    @Override
    public PublicKey getApplicationPublicKey(final String tokenApplicationName, final String timestamp) throws InvalidKeyTimestampException {
      if (applicationName.toString().equals(tokenApplicationName))
        return tenantRsaKeyProvider.getPublicKey(timestamp);

      final ApplicationSignatureEntity signature = applicationSignatures.get(tokenApplicationName, timestamp)
              .orElseThrow(() -> new InvalidKeyTimestampException(timestamp));

      return new RsaPublicKeyBuilder()
              .setPublicKeyMod(signature.getPublicKeyMod())
              .setPublicKeyExp(signature.getPublicKeyExp())
              .build();
    }
  }

  @CommandHandler(logStart = CommandLogLevel.DEBUG, logFinish = CommandLogLevel.DEBUG)
  public AuthenticationCommandResponse process(final RefreshTokenAuthenticationCommand command)
      throws AmitAuthenticationException
  {
    final TokenDeserializationResult deserializedRefreshToken =
        tenantRefreshTokenSerializer.deserialize(new TenantIdentityRsaKeyProvider(), command.getRefreshToken());

    final PrivateTenantInfoEntity privateTenantInfo = checkedGetPrivateTenantInfo();
    final PrivateSignatureEntity privateSignature = checkedGetPrivateSignature();

    final UserEntity user = getUser(deserializedRefreshToken.getUserIdentifier());
    final String sourceApplicationName = deserializedRefreshToken.getSourceApplication();

    return getAuthenticationResponse(
            sourceApplicationName,
            Optional.ofNullable(deserializedRefreshToken.getEndpointSet()),
            privateTenantInfo,
            privateSignature,
            user,
            command.getRefreshToken(),
            LocalDateTime.ofInstant(deserializedRefreshToken.getExpiration().toInstant(), ZoneId.of("UTC")));
  }

  private AuthenticationCommandResponse getAuthenticationResponse(
          final String sourceApplicationName,
          @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
          final Optional<String> callEndpointSet,
          final PrivateTenantInfoEntity privateTenantInfo,
          final PrivateSignatureEntity privateSignature,
          final UserEntity user,
          final String refreshToken,
          final LocalDateTime refreshTokenExpiration) {
    final Optional<LocalDateTime> passwordExpiration = getExpiration(user);

    final int gracePeriod = privateTenantInfo.getTimeToChangePasswordAfterExpirationInDays();
    if (pastGracePeriod(passwordExpiration, gracePeriod))
      throw AmitAuthenticationException.passwordExpired();

    final Set<TokenPermission> tokenPermissions;

    if (sourceApplicationName.equals(applicationName.toString())) { //ie, this is a token for the identity manager.
      if (pastExpiration(passwordExpiration)) {
        tokenPermissions = identityEndpointsAllowedEvenWithExpiredPassword();
        logger.info("Password expired {}", passwordExpiration.map(LocalDateTime::toString).orElse("empty"));
      }
      else {
        tokenPermissions = getUserTokenPermissions(user);
      }
    }
    else {
      tokenPermissions = getApplicationTokenPermissions(user, sourceApplicationName, callEndpointSet);
    }

    final HashSet<TokenPermission> minifiedTokenPermissions = new HashSet<>(
        tokenPermissions
            .stream()
            .collect(Collectors.toMap(TokenPermission::getPath,
                tokenPermission -> tokenPermission,
                (currentTokenPermission, newTokenPermission) -> {
                  newTokenPermission.getAllowedOperations()
                      .forEach(allowedOperation -> currentTokenPermission.getAllowedOperations().add(allowedOperation));
                  return currentTokenPermission;
                })
            )
            .values()
    );


    logger.info("Access token for tenant '{}', user '{}', application '{}', and callEndpointSet '{}' being returned containing the permissions '{}'.",
            TenantContextHolder.identifier().orElse("null"),
            user.getIdentifier(),
            sourceApplicationName,
            callEndpointSet.orElse("null"),
            minifiedTokenPermissions.toString());

    final TokenSerializationResult accessToken = getAuthenticationResponse(
            user.getIdentifier(),
            minifiedTokenPermissions,
            privateSignature,
            sourceApplicationName);

    return new AuthenticationCommandResponse(
            accessToken.getToken(), DateConverter.toIsoString(accessToken.getExpiration()),
            refreshToken, DateConverter.toIsoString(refreshTokenExpiration),
            passwordExpiration.map(DateConverter::toIsoString).orElse(null));
  }

  private Optional<LocalDateTime> getExpiration(final UserEntity user)
  {
    if (user.getIdentifier().equals(IdentityConstants.SU_NAME))
      return Optional.empty();
    else
      return Optional.of(LocalDateTime.of(
              LocalDate.ofEpochDay(user.getPasswordExpiresOn().getDaysSinceEpoch()), //Convert from cassandra LocalDate to java LocalDate.
              LocalTime.MIDNIGHT));
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

  private TokenSerializationResult getAuthenticationResponse(
          final String userIdentifier,
          final Set<TokenPermission> tokenPermissions,
          final PrivateSignatureEntity privateSignatureEntity,
          final String sourceApplication) {

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
              .setUser(userIdentifier)
              .setSourceApplication(sourceApplication);

      return tenantAccessTokenSerializer.build(x);
  }

  private Set<TokenPermission> getUserTokenPermissions(
          final UserEntity user) {

    final Optional<RoleEntity> userRole = roles.get(user.getRole());
    final Set<TokenPermission> tokenPermissions = userRole
            .map(r -> r.getPermissions().stream().flatMap(this::mapPermissions).collect(Collectors.toSet()))
            .orElse(new HashSet<>());

    tokenPermissions.addAll(identityEndpointsForEveryUser());

    return tokenPermissions;
  }

  private Set<TokenPermission> getApplicationTokenPermissions(
          final UserEntity user,
          final String sourceApplicationName,
          @SuppressWarnings("OptionalUsedAsFieldOrParameterType") final Optional<String> callEndpointSet) {

    //If the call endpoint set was given, but does not correspond to a stored call endpoint set, throw an exception.
    //If it wasn't given then return all of the permissions for the application.
    final Optional<ApplicationCallEndpointSetEntity> applicationCallEndpointSet = callEndpointSet.map(x -> {
      final Optional<ApplicationCallEndpointSetEntity> optionalEndpointSetEntity =
          applicationCallEndpointSets.get(sourceApplicationName, x);
      if (optionalEndpointSetEntity.isPresent()) {
        return optionalEndpointSetEntity.get();
      } else {
        throw AmitAuthenticationException.invalidToken();
      }
    });

    final RoleEntity userRole = roles.get(user.getRole())
            .orElseThrow(AmitAuthenticationException::userPasswordCombinationNotFound);

    return applicationCallEndpointSet.map(x -> this.getApplicationCallEndpointSetTokenPermissions(user.getIdentifier(), userRole, x, sourceApplicationName))
            .orElseGet(() -> this.getApplicationUserTokenPermissions(user.getIdentifier(), userRole, sourceApplicationName));
  }

  private Set<TokenPermission> getApplicationCallEndpointSetTokenPermissions(
          final String userIdentifier,
          final RoleEntity userRole,
          final ApplicationCallEndpointSetEntity applicationCallEndpointSet,
          final String sourceApplicationName) {
    final List<PermissionType> permissionsForUser = userRole.getPermissions();
    final Set<PermissionType> permissionsRequestedByApplication = applicationCallEndpointSet.getCallEndpointGroupIdentifiers().stream()
            .map(x -> applicationPermissions.getPermissionForApplication(sourceApplicationName, x))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());

    final Stream<PermissionType> applicationRequestedPermissionsTheUserHas
            = intersectPermissionList(permissionsForUser, permissionsRequestedByApplication.stream());

    final Set<PermissionType> permissionsPossible = applicationRequestedPermissionsTheUserHas
            .filter(x ->
                    applicationPermissionUsers.enabled(sourceApplicationName, x.getPermittableGroupIdentifier(), userIdentifier))
            .collect(Collectors.toSet());

    if (!permissionsPossible.containsAll(permissionsRequestedByApplication))
      throw AmitAuthenticationException.applicationMissingPermissions(userIdentifier, sourceApplicationName);

    return permissionsPossible.stream()
            .flatMap(this::mapPermissions)
            .collect(Collectors.toSet());
  }

  private Set<TokenPermission> getApplicationUserTokenPermissions(
          final String userIdentifier,
          final RoleEntity userRole,
          final String sourceApplicationName) {
    final List<PermissionType> permissionsForUser = userRole.getPermissions();
    final List<PermissionType> permissionsRequestedByApplication = applicationPermissions.getAllPermissionsForApplication(sourceApplicationName);

    final Stream<PermissionType> applicationRequestedPermissionsTheUserHas
            = intersectPermissionList(permissionsForUser, permissionsRequestedByApplication.stream());

    return applicationRequestedPermissionsTheUserHas
            .filter(x ->
                    applicationPermissionUsers.enabled(sourceApplicationName, x.getPermittableGroupIdentifier(), userIdentifier))
            .flatMap(this::mapPermissions)
            .collect(Collectors.toSet());
  }

  private Stream<PermissionType> intersectPermissionList(
          final List<PermissionType> permissionsForUser,
          final Stream<PermissionType> permissionsRequestedByApplication) {
    final Map<String, Set<AllowedOperationType>> keyedUserPermissions = transformToSearchablePermissions(permissionsForUser);

    return permissionsRequestedByApplication
            .map(x -> new PermissionType(
                    x.getPermittableGroupIdentifier(),
                    intersectSets(keyedUserPermissions.get(x.getPermittableGroupIdentifier()), x.getAllowedOperations())))
            .filter(x -> !x.getAllowedOperations().isEmpty());
  }

  static <T> Set<T> intersectSets(
          final @Nullable Set<T> allowedOperations1,
          final @Nullable Set<T> allowedOperations2) {
    if (allowedOperations1 == null || allowedOperations2 == null)
      return Collections.emptySet();

    final Set<T> ret = new HashSet<>(allowedOperations1);
    ret.retainAll(allowedOperations2);
    return ret;
  }

  static Map<String, Set<AllowedOperationType>> transformToSearchablePermissions(final List<PermissionType> permissionsForUser) {
    final Collector<Set<AllowedOperationType>, Set<AllowedOperationType>, Set<AllowedOperationType>> setToSetCollector
            = Collector.of(
            HashSet::new,
            Set::addAll,
            (x, y) -> {
              final Set<AllowedOperationType> ret = new HashSet<>();
              ret.addAll(x);
              ret.addAll(y);
              return ret;
            });

    return permissionsForUser.stream().collect(
            Collectors.groupingBy(PermissionType::getPermittableGroupIdentifier,
                    Collectors.mapping(PermissionType::getAllowedOperations, setToSetCollector)));
  }

  private Set<TokenPermission> identityEndpointsForEveryUser() {
    final Set<TokenPermission> ret = identityEndpointsAllowedEvenWithExpiredPassword();

    ret.add(new TokenPermission(
            applicationName + "/applications/*/permissions/*/users/{useridentifier}/enabled",
            Sets.newHashSet(AllowedOperation.READ, AllowedOperation.CHANGE, AllowedOperation.DELETE)));
    ret.add(new TokenPermission(
            applicationName + "/users/{useridentifier}/permissions",
            Sets.newHashSet(AllowedOperation.READ)));

    return ret;
  }

  private Set<TokenPermission> identityEndpointsAllowedEvenWithExpiredPassword() {
    final Set<TokenPermission> ret = new HashSet<>();

    ret.add(new TokenPermission(
            applicationName + "/users/{useridentifier}/password",
            Sets.newHashSet(AllowedOperation.READ, AllowedOperation.CHANGE, AllowedOperation.DELETE)));
    ret.add(new TokenPermission(
            applicationName + "/token/_current",
            Sets.newHashSet(AllowedOperation.DELETE)));

    return ret;
  }

  static boolean pastExpiration(
          @SuppressWarnings("OptionalUsedAsFieldOrParameterType") final Optional<LocalDateTime> passwordExpiration) {
    return passwordExpiration.map(x -> LocalDateTime.now().compareTo(x) >= 0).orElse(false);
  }

  static boolean pastGracePeriod(
          @SuppressWarnings("OptionalUsedAsFieldOrParameterType") final Optional<LocalDateTime> passwordExpiration,
          final long gracePeriod) {
    return passwordExpiration.map(x -> (LocalDateTime.now().compareTo(x.plusDays(gracePeriod)) >= 0)).orElse(false);
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
    final HashSet<AllowedOperation> allowedOperations = new HashSet<>();
    allowedOperations.add(RoleMapper.mapAllowedOperation(AllowedOperationType.fromHttpMethod(permittable.getMethod())));
    return new TokenPermission(permittable.getPath(), allowedOperations);
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
            .setUser(user.getIdentifier())
            .setSourceApplication(applicationName.toString());

    return tenantRefreshTokenSerializer.build(x);
  }
}
