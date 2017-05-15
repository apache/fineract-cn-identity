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

import io.mifos.anubis.api.v1.TokenConstants;
import io.mifos.anubis.token.TenantRefreshTokenSerializer;
import io.mifos.anubis.token.TokenSerializationResult;
import io.mifos.core.api.context.AutoUserContext;
import io.mifos.core.api.util.FeignTargetWithCookieJar;
import io.mifos.core.api.util.InvalidTokenException;
import io.mifos.core.api.util.NotFoundException;
import io.mifos.core.test.domain.TimeStampChecker;
import io.mifos.core.test.env.TestEnvironment;
import io.mifos.core.test.fixture.TenantDataStoreTestContext;
import io.mifos.identity.api.v1.client.IdentityManager;
import io.mifos.identity.api.v1.domain.Authentication;
import io.mifos.identity.api.v1.domain.Password;
import io.mifos.identity.api.v1.domain.Permission;
import io.mifos.identity.api.v1.domain.User;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Myrle Krantz
 */
public class TestRefreshToken extends AbstractComponentTest {
  private static final int ACCESS_TOKEN_TIME_TO_LIVE = 5;
  private static final int REFRESH_TOKEN_TIME_TO_LIVE = 10;

  @BeforeClass
  public static void setup() throws Exception {
    //Shorten access time to 5 seconds for test purposes.;
    System.getProperties().setProperty("identity.token.access.ttl", String.valueOf(ACCESS_TOKEN_TIME_TO_LIVE));
    System.getProperties().setProperty("identity.token.refresh.ttl", String.valueOf(REFRESH_TOKEN_TIME_TO_LIVE));
  }

  @Test(expected = InvalidTokenException.class)
  public void adminLoginAccessTokenShouldTimeOut() throws InterruptedException {
    try (final AutoUserContext ignore = loginAdmin()) {
      Thread.sleep(TimeUnit.SECONDS.toMillis(ACCESS_TOKEN_TIME_TO_LIVE + 1));
      getTestSubject().getUser(ADMIN_IDENTIFIER);
    }
  }

  @Test(expected = InvalidTokenException.class)
  public void adminLoginRefreshTokenShouldTimeOut() throws InterruptedException {
    getTestSubject().login(ADMIN_IDENTIFIER, TestEnvironment.encodePassword(ADMIN_PASSWORD));

    Thread.sleep(TimeUnit.SECONDS.toMillis(REFRESH_TOKEN_TIME_TO_LIVE + 1));

    getTestSubject().refresh();
  }

  @Test
  public void afterAccessTokenExpiresRefreshTokenShouldAcquireNewAccessToken() throws InterruptedException {
    getTestSubject().login(ADMIN_IDENTIFIER, TestEnvironment.encodePassword(ADMIN_PASSWORD));

    Thread.sleep(TimeUnit.SECONDS.toMillis(ACCESS_TOKEN_TIME_TO_LIVE + 1));

    final Authentication refreshAccessTokenAuthentication =
            getTestSubject().refresh();

    try (final AutoUserContext ignored = new AutoUserContext(ADMIN_IDENTIFIER, refreshAccessTokenAuthentication.getAccessToken())) {
      getTestSubject().changeUserPassword(ADMIN_IDENTIFIER, new Password(TestEnvironment.encodePassword(ADMIN_PASSWORD)));
    }
  }

  @Test(expected = InvalidTokenException.class)
  public void refreshTokenShouldGrantAccessOnlyToOneTenant()
  {
    getTestSubject().login(ADMIN_IDENTIFIER, TestEnvironment.encodePassword(ADMIN_PASSWORD));

    try (final TenantDataStoreTestContext ignored = TenantDataStoreTestContext.forRandomTenantName(cassandraInitializer)) {
      try (final AutoUserContext ignored2
                   = tenantApplicationSecurityEnvironment.createAutoSeshatContext()) {
        getTestSubject().initialize(TestEnvironment.encodePassword(ADMIN_PASSWORD));
      }

      getTestSubject().refresh();
    }
  }

  @Test
  public void expirationDatesShouldBeCorrectIsoDateTimes() throws InterruptedException {
    final Authentication authentication =
            getTestSubject().login(ADMIN_IDENTIFIER, TestEnvironment.encodePassword(ADMIN_PASSWORD));

    final TimeStampChecker preRefreshAccessTokenTimeStampChecker = TimeStampChecker.inTheFuture(Duration.ofSeconds(ACCESS_TOKEN_TIME_TO_LIVE));
    final TimeStampChecker refreshTokenTimeStampChecker = TimeStampChecker.inTheFuture(Duration.ofSeconds(REFRESH_TOKEN_TIME_TO_LIVE));

    Assert.assertNotNull(authentication);

    preRefreshAccessTokenTimeStampChecker.assertCorrect(authentication.getAccessTokenExpiration());
    refreshTokenTimeStampChecker.assertCorrect(authentication.getRefreshTokenExpiration());

    TimeUnit.SECONDS.sleep(3);
    final TimeStampChecker postRefreshAccessTokenTimeStampChecker = TimeStampChecker.inTheFuture(Duration.ofSeconds(ACCESS_TOKEN_TIME_TO_LIVE));

    final Authentication refreshedAuthentication = getTestSubject().refresh();

    postRefreshAccessTokenTimeStampChecker.assertCorrect(refreshedAuthentication.getAccessTokenExpiration());
    refreshTokenTimeStampChecker.assertCorrect(refreshedAuthentication.getRefreshTokenExpiration());
  }

  @Test
  public void bothRefreshMethodsShouldProduceSamePermissions() throws InterruptedException {
    final Permission userPermission = buildUserPermission();
    final ApplicationSignatureTestData appPlusSig;
    try (final AutoUserContext ignored
                 = tenantApplicationSecurityEnvironment.createAutoSeshatContext()) {
      appPlusSig = setApplicationSignature();
      createApplicationPermission(appPlusSig.getApplicationIdentifier(), userPermission);
    }

    try (final AutoUserContext ignored = loginAdmin()) {
      getTestSubject().setApplicationPermissionEnabledForUser(
              appPlusSig.getApplicationIdentifier(),
              userPermission.getPermittableEndpointGroupIdentifier(),
              ADMIN_IDENTIFIER,
              true);
    }

    final TenantRefreshTokenSerializer refreshTokenSerializer = new TenantRefreshTokenSerializer();

    final TokenSerializationResult tokenSerializationResult =
            refreshTokenSerializer.build(new TenantRefreshTokenSerializer.Specification()
                    .setUser(ADMIN_IDENTIFIER)
                    .setSecondsToLive(30)
                    .setKeyTimestamp(appPlusSig.getKeyTimestamp())
                    .setPrivateKey(appPlusSig.getKeyPair().privateKey())
                    .setSourceApplication(appPlusSig.getApplicationIdentifier()));

    final FeignTargetWithCookieJar<IdentityManager> identityManagerWithCookieJar
            = apiFactory.createWithCookieJar(IdentityManager.class, testEnvironment.serverURI());

    identityManagerWithCookieJar.putCookie("/token", TokenConstants.REFRESH_TOKEN_COOKIE_NAME, tokenSerializationResult.getToken());

    final Authentication applicationAuthenticationViaCookie = identityManagerWithCookieJar.getFeignTarget().refresh();

    final Authentication applicationAuthenticationViaParam = getTestSubject().refresh(tokenSerializationResult.getToken());

    try (final AutoUserContext ignored = new AutoUserContext(ADMIN_IDENTIFIER, applicationAuthenticationViaCookie.getAccessToken()))
    {
      checkAccessToUsersAndOnlyUsers();
    }

    try (final AutoUserContext ignored = new AutoUserContext(ADMIN_IDENTIFIER, applicationAuthenticationViaParam.getAccessToken()))
    {
      checkAccessToUsersAndOnlyUsers();
    }

  }

  private void checkAccessToUsersAndOnlyUsers() {
    final List<User> users = getTestSubject().getUsers();
    Assert.assertFalse(users.isEmpty());

    try {
      getTestSubject().getRoles();
      Assert.fail("Shouldn't be able to get roles with token for application for which roles are not permitted.");
    }
    catch (final NotFoundException ignored2) { }
  }

}
